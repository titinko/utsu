package com.utsusynth.utsu.model.song;

import java.io.File;
import java.util.Optional;
import com.google.common.collect.ImmutableList;
import com.utsusynth.utsu.common.data.EnvelopeData;
import com.utsusynth.utsu.common.data.NoteConfigData;
import com.utsusynth.utsu.common.data.NoteUpdateData;
import com.utsusynth.utsu.common.data.PitchbendData;
import com.utsusynth.utsu.common.utils.RoundUtils;

/**
 * Represents one note in a song. This is primarily a data storage class, so it can be instantiated
 * directly and not injected.
 */
public class Note {
    // Values the user has control over. These are saved to file.
    private int delta; // In ms, corresponds with 125 bpm tempo.
    private int duration; // In ms, corresponds with 125 bpm tempo.
    private int length; // In ms, corresponds with 125 bpm tempo.
    private String lyric;
    private int noteNum; // Encapsulates both key and note.
    private double preutter; // Optional, use lyric preutter if not set.
    private double overlap; // Optional, use note overlap if not set.
    private double velocity;
    private double startPoint;
    private int intensity;
    private int modulation;
    private String noteFlags;
    private ImmutableList<Double> pbs; // Pitch bend start.
    private ImmutableList<Double> pbw; // Pitch bend widths
    private ImmutableList<Double> pby; // Pitch bend shifts
    private ImmutableList<String> pbm; // Pitch bend curves
    private double[] envelopeWidth; // "p" in ms
    private double[] envelopeHeight; // "v" in % of total intensity (0-100)
    private double envelopeOverlap; // This value is meaningless.
    private int[] vibrato;

    // Values calculated in-program and not saved to any file.
    // These are set in SongNoteStandardizer.
    private double realPreutter;
    private double realDuration;
    private double autoStartPoint; // This is added to the user-added startPoint.
    private String trueLyric; // Note's lyric after processing and formatting.
    private Optional<File> cacheFile; // Result of applying resampler to this note.

    public Note() {
        // Set every required field to its default.
        this.delta = -1; // Must be set in builder.
        this.duration = -1; // Must be set in builder.
        this.length = -1; // Must be set in builder.
        this.lyric = ""; // Must be set in builder.
        this.noteNum = -1; // Must be set in builder.
        this.preutter = -1; // Should be ignored if not explicitly set.
        this.overlap = -1; // Should be ignored if not explicitly set.
        this.velocity = 100;
        this.startPoint = 0;
        this.intensity = 100;
        this.modulation = 0;
        this.noteFlags = "";
        this.pbs = ImmutableList.of(-40.0, 0.0);
        this.pbw = ImmutableList.of(80.0);
        this.pby = ImmutableList.of();
        this.pbm = ImmutableList.of();
        this.envelopeWidth = new double[5];
        this.envelopeHeight = new double[5];
        this.setEnvelope(
                new String[] {"5", "1", "1", "100", "100", "100", "100", "7", "35", "1", "100"});
        this.vibrato = new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

        this.realPreutter = 0;
        this.realDuration = -1; // Should be ignored if not explicitly set.
        this.autoStartPoint = 0;
        this.trueLyric = ""; // Defaults to "" if no lyric is found.
        this.cacheFile = Optional.empty();
    }

    public void setDelta(int delta) {
        this.delta = delta;
    }

    public int getDelta() {
        return this.delta;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public void safeSetDuration(int duration) {
        if (this.length != -1 && this.length < duration) {
            this.duration = this.length;
        } else {
            this.duration = duration;
        }
    }

    public int getDuration() {
        return this.duration;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public void safeSetLength(int length) {
        if (this.duration != -1 && length < this.duration) {
            this.duration = length;
        }
        this.length = length;
    }

    public int getLength() {
        return this.length;
    }

    public void setLyric(String lyric) {
        this.lyric = lyric;
    }

    public String getLyric() {
        return this.lyric;
    }

    public void setNoteNum(int noteNum) {
        this.noteNum = noteNum;
    }

    public int getNoteNum() {
        if (this.noteNum < 0) {
            throw new IllegalStateException("Note num was not set!");
        }
        return this.noteNum;
    }

    public void setPreutter(double preutter) {
        this.preutter = preutter;
    }

    public void clearPreutter() {
        this.preutter = -1;
    }

    public Optional<Double> getPreutter() {
        if (preutter >= 0) {
            return Optional.of(preutter);
        }
        return Optional.empty();
    }

    public void setOverlap(double overlap) {
        this.overlap = overlap;
    }

    public void clearOverlap() {
        this.overlap = -1;
    }

    public Optional<Double> getOverlap() {
        if (overlap >= 0) {
            return Optional.of(overlap);
        }
        return Optional.empty();
    }

    public void setVelocity(double velocity) {
        this.velocity = velocity;
    }

    public double getVelocity() {
        return this.velocity;
    }

    public void setStartPoint(double startPoint) {
        this.startPoint = startPoint;
    }

    public double getStartPoint() {
        return this.startPoint;
    }

    public void setIntensity(int intensity) {
        this.intensity = intensity;
    }

    public int getIntensity() {
        return this.intensity;
    }

    public void setModulation(int modulation) {
        this.modulation = modulation;
    }

    public int getModulation() {
        return this.modulation;
    }

    public void setNoteFlags(String noteFlags) {
        this.noteFlags = noteFlags;
    }

    public String getNoteFlags() {
        return this.noteFlags;
    }

    public NoteUpdateData getUpdateData(int curPosition) {
        return new NoteUpdateData(
                curPosition,
                this.trueLyric,
                getEnvelope(),
                getPitchbends(),
                getConfigData());
    }

    public NoteConfigData getConfigData() {
        return new NoteConfigData(
                getPreutter(),
                getOverlap(),
                this.velocity,
                this.startPoint,
                this.intensity,
                this.modulation,
                this.noteFlags);
    }

    public void setConfigData(NoteConfigData configData) {
        if (configData.getPreutter().isPresent()) {
            this.preutter = configData.getPreutter().get();
        } else {
            clearPreutter();
        }
        if (configData.getOverlap().isPresent()) {
            this.overlap = configData.getOverlap().get();
        } else {
            clearOverlap();
        }
        this.velocity = configData.getConsonantVelocity();
        this.startPoint = configData.getStartPoint();
        this.intensity = configData.getIntensity();
        this.modulation = configData.getModulation();
        this.noteFlags = configData.getNoteFlags();
    }

    public PitchbendData getPitchbends() {
        return new PitchbendData(pbs, pbw, pby, pbm, vibrato);
    }

    public void setPitchbends(PitchbendData pitchbends) {
        this.pbs = pitchbends.getPBS();
        this.pbw = pitchbends.getPBW();
        this.pby = pitchbends.getPBY();
        this.pbm = pitchbends.getPBM();
        this.vibrato = pitchbends.getVibrato();
    }

    public void setPBS(String[] pbsValues) {
        ImmutableList.Builder<Double> builder = ImmutableList.builder();
        for (String value : pbsValues) {
            builder.add(safeParseDouble(value, 0));
        }
        pbs = builder.build();
    }

    public ImmutableList<Double> getPBS() {
        return pbs;
    }

    public void setPBW(String[] pbwValues) {
        ImmutableList.Builder<Double> builder = ImmutableList.builder();
        for (String value : pbwValues) {
            builder.add(safeParseDouble(value, 1));
        }
        pbw = builder.build();
    }

    public ImmutableList<Double> getPBW() {
        return pbw;
    }

    public void setPBY(String[] pbyValues) {
        ImmutableList.Builder<Double> builder = ImmutableList.builder();
        for (String value : pbyValues) {
            builder.add(safeParseDouble(value, 0));
        }
        pby = builder.build();
    }

    public ImmutableList<Double> getPBY() {
        return pby;
    }

    public void setPBM(String[] pbmValues) {
        pbm = ImmutableList.copyOf(pbmValues);
    }

    public ImmutableList<String> getPBM() {
        return pbm;
    }

    public void setEnvelope(String[] envelopeValues) {
        // Parse ust envelope values.
        if (envelopeValues.length > 5) {
            envelopeWidth[0] = safeParseDouble(envelopeValues[0], envelopeWidth[0]); // p1
            envelopeWidth[1] = safeParseDouble(envelopeValues[1], envelopeWidth[1]); // p2
            envelopeWidth[2] = safeParseDouble(envelopeValues[2], envelopeWidth[2]); // p3
            envelopeHeight[0] = safeParseDouble(envelopeValues[3], envelopeHeight[0]); // v1
            envelopeHeight[1] = safeParseDouble(envelopeValues[4], envelopeHeight[1]); // v2
            envelopeHeight[2] = safeParseDouble(envelopeValues[5], envelopeHeight[2]); // v3
        }
        if (envelopeValues.length > 6) {
            envelopeHeight[3] = safeParseDouble(envelopeValues[6], envelopeHeight[3]); // v4
        }
        if (envelopeValues.length > 8) {
            if (!envelopeValues[7].equals("%")) {
                envelopeOverlap = safeParseDouble(envelopeValues[7], envelopeOverlap); // useless
            }
            envelopeWidth[3] = safeParseDouble(envelopeValues[8], envelopeWidth[3]); // p4
        }
        if (envelopeValues.length > 10) {
            envelopeWidth[4] = safeParseDouble(envelopeValues[9], envelopeWidth[4]); // p5
            envelopeHeight[4] = safeParseDouble(envelopeValues[10], envelopeHeight[4]); // v5
        }

        // Try to catch an envelope that's V2/V3 crossfaded.
        if (envelopeHeight[0] < 1 && envelopeHeight[3] < 1) {
            // TODO: Ask the user if the weird envelope values were intentional.
            // Convert to V1/V4 crossfading.
            envelopeWidth[0] = envelopeWidth[1];
            envelopeWidth[1] = 1;
            envelopeHeight[0] = envelopeHeight[1];
            envelopeWidth[3] = envelopeWidth[2];
            envelopeWidth[2] = 1;
            envelopeHeight[3] = envelopeHeight[2];
        }
    }

    public String[] getFullEnvelope() {
        String[] envelope = new String[11];
        envelope[0] = Double.toString(envelopeWidth[0]); // p1
        envelope[1] = Double.toString(envelopeWidth[1]); // p2
        envelope[2] = Double.toString(envelopeWidth[2]); // p3
        envelope[3] = Double.toString(envelopeHeight[0]); // v1
        envelope[4] = Double.toString(envelopeHeight[1]); // v2
        envelope[5] = Double.toString(envelopeHeight[2]); // v3
        envelope[6] = Double.toString(envelopeHeight[3]); // v4
        envelope[7] = Double.toString(envelopeOverlap); // overlap
        envelope[8] = Double.toString(envelopeWidth[3]); // p4
        envelope[9] = Double.toString(envelopeWidth[4]); // p5
        envelope[10] = Double.toString(envelopeHeight[4]); // v5
        return envelope;
    }

    public double[] getRawFullEnvelope() {
        double[] envelope = new double[11];
        envelope[0] = envelopeWidth[0]; // p1
        envelope[1] = envelopeWidth[1]; // p2
        envelope[2] = envelopeWidth[2]; // p3
        envelope[3] = envelopeHeight[0]; // v1
        envelope[4] = envelopeHeight[1]; // v2
        envelope[5] = envelopeHeight[2]; // v3
        envelope[6] = envelopeHeight[3]; // v4
        envelope[7] = envelopeOverlap; // overlap
        envelope[8] = envelopeWidth[3]; // p4
        envelope[9] = envelopeWidth[4]; // p5
        envelope[10] = envelopeHeight[4]; // v5
        return envelope;
    }

    public void setEnvelope(EnvelopeData envelopeData) {
        // Ignore the envPreutter and envLength received from the view.
        envelopeWidth = envelopeData.getWidths();
        envelopeHeight = envelopeData.getHeights();
    }

    public EnvelopeData getEnvelope() {
        return new EnvelopeData(realPreutter, realDuration, envelopeWidth, envelopeHeight);
    }

    public double getFadeIn() {
        return envelopeWidth[0];
    }

    public void setFadeIn(double newFadeIn) {
        envelopeWidth[0] = newFadeIn;
    }

    public double getFadeOut() {
        return envelopeWidth[3];
    }

    public void setFadeOut(double newFadeOut) {
        envelopeWidth[3] = newFadeOut;
    }

    public void setVibrato(String[] vibratoValues) {
        for (int i = 0; i < 10; i++) {
            // Leave all unfilled vibrato values as the defaults.
            if (vibratoValues.length > i) {
                vibrato[i] = RoundUtils.round(safeParseDouble(vibratoValues[i], 0));
            }
        }
    }

    public String[] getVibrato() {
        String[] vibratoValues = new String[10];
        for (int i = 0; i < 10; i++) {
            vibratoValues[i] = Integer.toString(vibrato[i]);
        }
        return vibratoValues;
    }

    public double getRealPreutter() {
        return this.realPreutter;
    }

    public void setRealPreutter(double realPreutter) {
        this.realPreutter = realPreutter;
    }

    public double getRealDuration() {
        return this.realDuration;
    }

    public void setRealDuration(double realDuration) {
        this.realDuration = realDuration;
    }

    public double getAutoStartPoint() {
        return this.autoStartPoint;
    }

    public void setAutoStartPoint(double autoStartPoint) {
        this.autoStartPoint = autoStartPoint;
    }

    public String getTrueLyric() {
        return this.trueLyric;
    }

    public void setTrueLyric(String trueLyric) {
        this.trueLyric = trueLyric;
    }

    public Optional<File> getCacheFile() {
        return this.cacheFile;
    }

    public void setCacheFile(Optional<File> cacheFile) {
        this.cacheFile = cacheFile;
    }

    private static double safeParseDouble(String fromMe, double fallback) {
        try {
            return Double.parseDouble(fromMe);
        } catch (Exception e) {
            System.out.println("Warning: failed to parse double from " + fromMe);
            return fallback;
        }
    }

    /**
     * Validates that the given note has all required values populated.
     *
     * @throws IllegalStateException if the note is invalid.
     */
    public void validate() {
        if (this.delta < 0) {
            throw new IllegalArgumentException(
                    String.format("Invalid Note: Delta was negative or unset: %s", this.delta));
        }
        if (this.duration < 0) {
            throw new IllegalArgumentException(
                    String.format(
                            "Invalid Note: Duration was negative or unset: %s",
                            this.duration));
        }
        if (this.length < 0) {
            throw new IllegalArgumentException(
                    String.format("Invalid Note: Length was negative or unset: %s", this.length));
        }
        if (this.lyric.isEmpty()) {
            throw new IllegalArgumentException(
                    String.format("Invalid Note: Lyric was empty or unset: %s", this.lyric));
        }
        if (this.noteNum < 0) {
            throw new IllegalArgumentException(
                    String.format(
                            "Invalid Note: Note number was negative or unset: %s",
                            this.noteNum));
        }
        // TODO: Validate vibrato.
    }
}
