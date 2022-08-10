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
import java.util.Optional;

public class UtsuWavtool implements Wavtool {
    private final SoundFileReader soundFileReader;
    private final SoundFileWriter soundFileWriter;
    private double startDelta = 0; // Start duration in ms.
    private double totalDelta = 0; // Total duration in ms, used to debug timing issues.

    @Inject
    public UtsuWavtool(SoundFileReader soundFileReader, SoundFileWriter soundFileWriter) {
        this.soundFileReader = soundFileReader;
        this.soundFileWriter = soundFileWriter;
    }

    @Override
    public void startRender(double startDelta) {
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

        double relativeDelta = totalDelta - startDelta;
        Optional<WavData> wavData = soundFileReader.loadWavData(inputFile);
        System.out.println(relativeDelta + " and " + boundedOverlap);
        Optional<WavData> overlapData = soundFileReader.loadWavData(
                outputFile, RoundUtils.round(relativeDelta - boundedOverlap));
        if (wavData.isEmpty() || (boundedOverlap > 0 && overlapData.isEmpty())) {
            // TODO: Throw an error.
            System.out.println("Error: Unable to read WAV data.");
            return;
        }
        if (wavData.get().getLengthMs() < noteLength) {
            System.out.println("Error: Input note is not long enough.");
            return;
        }
        double[] wavSamples = wavData.get().getSamples();
        double[] overlapSamples = overlapData.map(WavData::getSamples).orElse(new double[] {});
        double[] combinedSamples = new double[wavSamples.length];
        for (int i = 0; i < wavSamples.length; i++) {
            if (i < overlapSamples.length) {
                combinedSamples[i] = wavSamples[i] + overlapSamples[i];
            } else {
                combinedSamples[i] = wavSamples[i];
            }
        }
        WavData combinedWav = new WavData(noteLength, combinedSamples);
        WavData scaledWav = applyEnvelope(combinedWav, note.getEnvelope());
        soundFileWriter.writeWavData(scaledWav, outputFile, RoundUtils.round(relativeDelta));

        totalDelta += noteLength - boundedOverlap;
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

        double relativeDelta = totalDelta - startDelta;
        int numSamples = RoundUtils.round(duration / 1000 * 44100);
        WavData silenceData = new WavData(duration, new double[numSamples]);
        soundFileWriter.writeWavData(
                silenceData, outputFile, RoundUtils.round(relativeDelta));

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
}
