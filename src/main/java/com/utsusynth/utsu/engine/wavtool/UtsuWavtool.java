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
        WavData scaledWav = applyEnvelope(wavData.get(), note.getEnvelope());

        overlaps.add(boundedOverlap);
        fragments.add(scaledWav);
        totalDelta += noteLength - boundedOverlap;
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

        int numSamples = RoundUtils.round(duration / 1000 * 44100);
        WavData silenceWav = new WavData(duration, new double[numSamples]);

        overlaps.add((double) 0);
        fragments.add(silenceWav);
        totalDelta += duration;
    }

    private WavData applyEnvelope(WavData wavData, EnvelopeData envelopeData) {
        double[] widths = envelopeData.getWidths();
        double[] result = new double[wavData.getSamples().length];
        double samplesPerMs = wavData.getSamplesPerMs();
        int[] xValues = new int[7];
        xValues[0] = 0;
        xValues[1] = RoundUtils.round(widths[0] * samplesPerMs);
        xValues[2] = RoundUtils.round((widths[0] + widths[1]) * samplesPerMs);
        xValues[3] = RoundUtils.round((widths[0] + widths[1] + widths[4]) * samplesPerMs);
        xValues[4] = RoundUtils.round((wavData.getLengthMs() - widths[2] - widths[3]) * samplesPerMs);
        xValues[5] = RoundUtils.round((wavData.getLengthMs() - widths[3]) * samplesPerMs);
        xValues[6] = RoundUtils.round(wavData.getSamples().length);
        double[] yValues = new double[7];
        yValues[0] = 0;
        for (int i = 0; i <= 4; i++) {
            yValues[i + 1] = envelopeData.getHeights()[i] / 100.0;
        }
        yValues[6] = 0;
        for (int segment = 0; segment < 6; segment++) {
            int minX = xValues[segment];
            int maxX = xValues[segment + 1];
            for (int xValue = minX; xValue < maxX; xValue++) {
                // How far through this x value is through the segment.
                double ratio = (xValue - minX) * 1.0 / (maxX - minX);
                // The value to multiply a sample by.
                double yValue = (yValues[segment] * (1 - ratio)) + (yValues[segment + 1] * ratio);
                result[xValue] = wavData.getSamples()[xValue] * yValue;
            }
        }
        return new WavData(wavData.getLengthMs(), result);
    }

    private void saveToOutputFile(File outputFile) {
        double sampleRate = 44100; // Number of samples per second of the output file.
        double durationMs = totalDelta - startDelta;
        int numSamples = RoundUtils.round(durationMs / 1000 * sampleRate);
        double[] combinedSamples = new double[numSamples];
        int curSample = 0;
        for (int i = 0; i < fragments.size(); i++) {
            WavData fragment = fragments.get(i);
            int overlapSamples = RoundUtils.round(overlaps.get(i) / 1000 * sampleRate);
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
}
