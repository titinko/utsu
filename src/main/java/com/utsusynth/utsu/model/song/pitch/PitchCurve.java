package com.utsusynth.utsu.model.song.pitch;

import java.util.HashMap;
import java.util.Optional;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.utsusynth.utsu.common.data.PitchbendData;
import com.utsusynth.utsu.model.song.pitch.portamento.Portamento;
import com.utsusynth.utsu.model.song.pitch.portamento.PortamentoFactory;

/**
 * Stores up to one pitchbend for each "pitch step" in a song. There are always 96 pitch steps per
 * beat, regardless of tempo.
 */
public class PitchCurve {
    // Map of pitch step number to attached pitchbend, if any.
    // TODO: Limit the minimum and maximum x-values of portamento/vibrato.
    private final HashMap<Integer, Pitchbend> pitchbends;
    private final PortamentoFactory portamentoFactory;

    @Inject
    public PitchCurve(PortamentoFactory portamentoFactory) {
        this.pitchbends = new HashMap<>();
        this.portamentoFactory = portamentoFactory;
    }

    /** Adds pitchbends for a single note. */
    public void addPitchbends(
            int noteStartMs,
            int noteLengthMs,
            PitchbendData data,
            int prevNoteNum,
            int curNoteNum) {
        if (data.getPBS().isEmpty() || data.getPBW().isEmpty()) {
            // TODO: Handle this.
            return;
        }
        // Start x value (in milliseconds) and y value (in tenths) of a pitchbend.
        double startMs = noteStartMs + data.getPBS().get(0);
        double pitchStart = prevNoteNum * 10; // Measured in tenths (1/10 a semitone.)
        ImmutableList<Double> pbw = data.getPBW();
        ImmutableList<Double> pby = data.getPBY();
        ImmutableList<String> pbm = data.getPBM();

        // Parse each portamento from provided values.
        for (int i = 0; i < pbw.size(); i++) {
            double endMs = startMs + pbw.get(i);
            double pitchEnd = curNoteNum * 10;
            if (pbw.size() > i + 1 && pby.size() >= i + 1) {
                // Update pitch if we're not at the last width and a y-value exists.
                pitchEnd += pby.get(i);
            }
            String pitchShape = "";
            if (pbm.size() >= i + 1) {
                pitchShape = pbm.get(i);
            }
            Portamento portamento = portamentoFactory
                    .makePortamento(startMs, pitchStart, endMs, pitchEnd, pitchShape);

            // Add portamento to all affected steps on the pitch curve.
            for (int j = nextPitchStep(startMs); j <= prevPitchStep(endMs); j++) {
                if (pitchbends.containsKey(j)) {
                    pitchbends.get(j).addPortamento(noteStartMs, portamento);
                } else {
                    pitchbends.put(j, Pitchbend.makePitchbend(noteStartMs, portamento));
                }
            }
            // End of the current pitchbend is the start of the next one.
            startMs = endMs;
            pitchStart = pitchEnd;
        }

        // Parse vibrato if vibrato length > 0.
        if (data.getVibrato(0) > 0) {
            double vibratoLengthMs = noteLengthMs * (data.getVibrato(0) * 1.0 / 100);
            double vibratoStartMs = noteStartMs + noteLengthMs - vibratoLengthMs;
            double vibratoEndMs = noteStartMs + noteLengthMs;
            // Does not use indices 7 and 9 because they don't seem to do anything.
            Vibrato vibrato = new Vibrato(
                    vibratoStartMs,
                    vibratoEndMs,
                    data.getVibrato(1),
                    data.getVibrato(2),
                    data.getVibrato(3),
                    data.getVibrato(4),
                    data.getVibrato(5),
                    data.getVibrato(6),
                    data.getVibrato(8));
            for (int i = nextPitchStep(vibratoStartMs); i < prevPitchStep(vibratoEndMs); i++) {
                if (pitchbends.containsKey(i)) {
                    pitchbends.get(i).addVibrato(vibrato);
                } else {
                    pitchbends.put(i, Pitchbend.makePitchbend(vibrato));
                }
            }
        }
    }

    /** Removes pitchbends for a single note. */
    public void removePitchbends(int noteStartMs, int noteLengthMs, PitchbendData data) {
        if (data.getPBS().isEmpty() || data.getPBW().isEmpty()) {
            // TODO: Handle this.
            return;
        }
        double startMs = noteStartMs + data.getPBS().get(0);
        double endMs = startMs;
        for (double width : data.getPBW()) {
            endMs += width;
        }
        for (int i = nextPitchStep(startMs); i <= prevPitchStep(endMs); i++) {
            // Remove portamento from each pitch step it covers.
            if (pitchbends.containsKey(i)) {
                Pitchbend pitchbend = pitchbends.get(i);
                pitchbend.removePortamento(noteStartMs);
                if (pitchbend.isEmpty()) {
                    pitchbends.remove(i);
                }
            }
        }

        // Remove vibrato from the entire note.
        if (data.getVibrato(0) > 0 || data.getVibrato(1) > 0) {
            double noteEndMs = noteStartMs + noteLengthMs;
            for (int i = nextPitchStep(noteStartMs); i < prevPitchStep(noteEndMs); i++) {
                if (pitchbends.containsKey(i)) {
                    Pitchbend pitchbend = pitchbends.get(i);
                    pitchbend.removeVibrato();
                    if (pitchbend.isEmpty()) {
                        pitchbends.remove(i);
                    }
                }
            }
        }
    }

    /** Writes out pitchbends for a section into a format readable by resamplers. */
    public String renderPitchbends(int firstStep, int lastStep, int noteNum) {
        StringBuilder result = new StringBuilder();
        double noteNumPitch = noteNum * 10; // In tenths. (1/10 of a semitone)
        double defaultPitch = 0; // In tenths. (1/10 of a semitone)
        for (int scanStep = firstStep; scanStep <= lastStep; scanStep++) {
            // Scan through the steps until first default pitch is found.
            if (pitchbends.containsKey(scanStep)) {
                Optional<Portamento> portamento = pitchbends.get(scanStep).getPortamento();
                if (portamento.isPresent()) {
                    defaultPitch = portamento.get().getStartPitch();
                    break;
                }
            }
        }

        for (int step = firstStep; step <= lastStep; step++) {
            if (pitchbends.containsKey(step)) {
                // Write pitchbend.
                int positionMs = step * 5; // 92 pitch steps in a beat of 480 ms.
                double realPitch = pitchbends.get(step).apply(positionMs); // In tenths.
                if (!pitchbends.get(step).getPortamento().isPresent()) {
                    realPitch += defaultPitch; // Vibrato modifies default pitch if no portamento.
                }
                int diff = (int) ((realPitch - noteNumPitch) * 10); // In cents.
                result.append(convertTo12Bit(diff));

                // Set the default pitch to the one at the end of current portamento.
                Optional<Portamento> portamento = pitchbends.get(step).getPortamento();
                if (portamento.isPresent()) {
                    defaultPitch = portamento.get().getEndPitch();
                }
            } else {
                // Write a stretch of no pitchbends.
                int numEmpty = 0;
                int emptyStep = step;
                for (; emptyStep <= lastStep; emptyStep++) {
                    if (pitchbends.containsKey(emptyStep)) {
                        break;
                    } else {
                        numEmpty++;
                    }
                }
                int diff = (int) ((defaultPitch - noteNumPitch) * 10); // In cents.
                result.append(convertTo12Bit(diff));
                if (numEmpty > 1) {
                    result.append(String.format("#%d#", numEmpty - 1));
                }
                step = emptyStep - 1; // Move step to the end of the empty stretch.
            }
        }
        return result.toString();
    }

    /**
     * For some reason, resamplers want two characters that represent a 12-bit number in two's
     * complement form (-2048 to 2047). I would not be using this format if existing resamplers
     * didn't require it.
     * 
     * @return A string of length 2 representing a 12-bit number.
     */
    private static String convertTo12Bit(int convertMe) {
        // Convert out of two's complement form.
        if (convertMe < 0) {
            convertMe += 4096;
        }
        // Make sure convertMe is between 0 and 4095.
        convertMe = Math.max(0, Math.min(4095, convertMe));
        StringBuilder result = new StringBuilder(); // Set to 0 by default.
        for (int sixBitNumber : ImmutableList.of(convertMe / 64, convertMe % 64)) {
            if (sixBitNumber >= 0 && sixBitNumber < 26) {
                result.append((char) (sixBitNumber + 'A'));
            } else if (sixBitNumber >= 26 && sixBitNumber < 52) {
                result.append((char) (sixBitNumber - 26 + 'a'));
            } else if (sixBitNumber >= 52 && sixBitNumber < 62) {
                result.append((char) (sixBitNumber - 52 + '0'));
            } else if (sixBitNumber == 62) {
                result.append('+');
            } else if (sixBitNumber == 63) {
                result.append('/');
            } else {
                // Return 0 if the number is not in range [0, 64).
                return "AA";
            }
        }
        if (result.length() != 2) {
            return "AA";
        }
        return result.toString();
    }

    // Finds the pitch step just after this position.
    private static int nextPitchStep(double positionMs) {
        return ((int) Math.ceil(positionMs / 5.0));
    }

    // Returns the pitch step just before this position.
    private static int prevPitchStep(double positionMs) {
        int prevStep = ((int) Math.floor(positionMs / 5.0));
        if (prevStep == nextPitchStep(positionMs)) {
            // Do not let prevPitchStep and nextPitchStep return the same value.
            return prevStep - 1;
        }
        return prevStep;
    }
}
