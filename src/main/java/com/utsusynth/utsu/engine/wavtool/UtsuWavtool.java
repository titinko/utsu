package com.utsusynth.utsu.engine.wavtool;

import com.google.inject.Inject;
import com.utsusynth.utsu.common.data.EnvelopeData;
import com.utsusynth.utsu.common.data.WavData;
import com.utsusynth.utsu.common.utils.RoundUtils;
import com.utsusynth.utsu.files.voicebank.SoundFileReader;
import com.utsusynth.utsu.files.voicebank.SoundFileWriter;
import com.utsusynth.utsu.model.song.Note;
import com.utsusynth.utsu.model.song.Song;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

public class UtsuWavtool implements Wavtool {
    private final SoundFileReader soundFileReader;
    private final SoundFileWriter soundFileWriter;
    private final ArrayList<Double> overlaps = new ArrayList<>();
    private final ArrayList<WavData> fragments = new ArrayList<>();
    private double startDelta = 0; // Start duration in ms.
    private double totalDelta = 0; // Total duration in ms, used to debug timing issues.

    @Inject
    public UtsuWavtool(SoundFileReader soundFileReader, SoundFileWriter soundFileWriter) {
        this.soundFileReader = soundFileReader;
        this.soundFileWriter = soundFileWriter;
    }

    @Override
    public void startRender(double startDelta) {
        overlaps.clear();
        fragments.clear();
        this.startDelta = startDelta;
        totalDelta = startDelta;
    }

    @Override
    public void addNewNote(
            Song song,
            Note note,
            double noteLength,
            double expectedDelta,
            File inputFile,
            File outputFile,
            boolean includeOverlap,
            boolean triggerSynthesis) {
        double boundedOverlap = Math.max(0, Math.min(note.getFadeIn(), noteLength));
        // Ignore overlap if current note doesn't touch previous one.
        if (!includeOverlap) {
            boundedOverlap = 0;
        }

        // Check that current length matches expected length and correct any discrepancies.
        if (expectedDelta > totalDelta && Math.abs(expectedDelta - totalDelta) > 0.01) {
            double timingCorrection = expectedDelta - totalDelta;
            if (boundedOverlap > timingCorrection) {
                // Disable for now.
                // boundedOverlap -= timingCorrection;
                System.out.println("Would correct note timing by " + timingCorrection + " ms.");
            }
        }
        totalDelta += noteLength - boundedOverlap;

        Optional<WavData> wavData = soundFileReader.loadWavData(inputFile);
        if (wavData.isEmpty()) {
            // TODO: Throw an error.
            System.out.println("Error: Unable to read WAV data.");
            return;
        }
        if (wavData.get().getLengthMs() < noteLength) {
            System.out.println("Error: Input note is not long enough.");
            return;
        }
        int numSamples = msToNumSamples(noteLength);
        WavData truncatedWav =
                new WavData(noteLength, Arrays.copyOf(wavData.get().getSamples(), numSamples));
        WavData scaledWav = applyEnvelope(truncatedWav, note.getEnvelope());

        overlaps.add(boundedOverlap);
        fragments.add(scaledWav);
        if (triggerSynthesis) {
            saveToOutputFile(outputFile);
        }
    }

    @Override
    public void addSilence(
            double duration,
            double expectedDelta,
            File inputFile,
            File outputFile,
            boolean triggerSynthesis) {
        // Check that current length matches expected length and correct any discrepancies.
        if (expectedDelta > totalDelta && Math.abs(expectedDelta - totalDelta) > 0.01) {
            double timingCorrection = expectedDelta - totalDelta;
            duration += timingCorrection;
            System.out.println("Corrected timing by " + timingCorrection + " ms.");
        }

        int numSamples = msToNumSamples(duration);
        WavData silenceWav = new WavData(duration, new double[numSamples]);

        overlaps.add((double) 0);
        fragments.add(silenceWav);
        totalDelta += duration;
    }

    @Override
    public String toString() {
        return "Default";
    }

    private WavData applyEnvelope(WavData wavData, EnvelopeData envelopeData) {
        double[] widths = envelopeData.getWidths();
        double[] result = new double[wavData.getSamples().length];
        double samplesPerMs = wavData.getSamplesPerMs();
        int[] xValues = new int[8];
        xValues[0] = 0;
        xValues[1] = RoundUtils.round(widths[0] * samplesPerMs);
        xValues[2] = RoundUtils.round((widths[0] + widths[1]) * samplesPerMs);
        xValues[3] = RoundUtils.round((widths[0] + widths[1] + widths[4]) * samplesPerMs);
        xValues[4] = RoundUtils.round((wavData.getLengthMs() - widths[2] - widths[3]) * samplesPerMs);
        xValues[5] = RoundUtils.round((wavData.getLengthMs() - widths[3]) * samplesPerMs);
        xValues[6] = Math.max(xValues[5], wavData.getSamples().length - 400); // Final phase out.
        xValues[7] = wavData.getSamples().length;

        double[] yValues = new double[8];
        yValues[0] = 0;
        yValues[1] = envelopeData.getHeights()[0] / 100.0;
        yValues[2] = envelopeData.getHeights()[1] / 100.0;
        yValues[3] = envelopeData.getHeights()[4] / 100.0;
        yValues[4] = envelopeData.getHeights()[2] / 100.0;
        yValues[5] = envelopeData.getHeights()[3] / 100.0;
        yValues[6] = 0.1; // Final phase out.
        yValues[7] = 0;
        for (int segment = 0; segment < xValues.length - 1; segment++) {
            int minX = xValues[segment];
            int maxX = xValues[segment + 1];
            for (int xValue = minX; xValue < maxX; xValue++) {
                // How far through this x value is through the segment.
                double ratio = (xValue - minX) * 1.0 / (maxX - minX);
                // The value to multiply a sample by.
                double yValue = (yValues[segment] * (1 - ratio)) + (yValues[segment + 1] * ratio);
                result[xValue] = wavData.getSamples()[xValue] * yValue;
                result[xValue] = Math.min(1, Math.max(-1, result[xValue])); // Clamp to [-1,1].
            }
        }
        return new WavData(wavData.getLengthMs(), result);
    }

    private void saveToOutputFile(File outputFile) {
        double durationMs = totalDelta - startDelta;
        int numSamples = msToNumSamples(durationMs);
        double[] combinedSamples = new double[numSamples];
        int curSample = 0;
        for (int i = 0; i < fragments.size(); i++) {
            WavData fragment = fragments.get(i);
            int overlapSamples = msToNumSamples(overlaps.get(i));
            curSample = Math.min(numSamples, Math.max(0, curSample - overlapSamples));
            int samplesToWrite = Math.min(fragment.getSamples().length, numSamples - curSample);
            int firstSample = curSample;
            int lastSample = curSample + samplesToWrite - 1; // Off-by-1 error.
            for (; curSample <= lastSample; curSample++) {
                if (curSample < firstSample + overlapSamples) {
                    combinedSamples[curSample] += fragment.getSamples()[curSample - firstSample];
                } else {
                    combinedSamples[curSample] = fragment.getSamples()[curSample - firstSample];
                }
            }
        }
        WavData combinedWav = new WavData(durationMs, combinedSamples);
        soundFileWriter.writeWavData(combinedWav, outputFile);
    }

    private static int msToNumSamples(double lengthMs) {
        // Convert milliseconds to samples, assuming a sample rate of 44,100 Hz.
        double sampleRate = 44100;
        int msPerSecond = 1000;
        return RoundUtils.round(lengthMs / msPerSecond * sampleRate);
    }
}
