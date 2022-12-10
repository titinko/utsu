package com.utsusynth.utsu.engine.wavtool;

import com.google.inject.Inject;
import com.utsusynth.utsu.common.StatusBar;
import com.utsusynth.utsu.common.data.EnvelopeData;
import com.utsusynth.utsu.common.data.WavData;
import com.utsusynth.utsu.common.exception.ErrorLogger;
import com.utsusynth.utsu.common.utils.RoundUtils;
import com.utsusynth.utsu.files.voicebank.SoundFileReader;
import com.utsusynth.utsu.files.voicebank.SoundFileWriter;
import com.utsusynth.utsu.model.song.Note;
import com.utsusynth.utsu.model.song.Song;
import javafx.util.Pair;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.*;

public class UtsuWavtool implements Wavtool {
    private static final ErrorLogger errorLogger = ErrorLogger.getLogger();

    private final StatusBar statusBar;
    private final int threadPoolSize;
    private final SoundFileReader soundFileReader;
    private final SoundFileWriter soundFileWriter;
    private final ArrayList<Future<Pair<Double, WavData>>> futures = new ArrayList<>();
    private double startDelta = 0; // Start duration in ms.
    private double totalDelta = 0; // Total duration in ms, used to debug timing issues.
    private ExecutorService threadPool;

    @Inject
    public UtsuWavtool(
            StatusBar statusBar,
            int threadPoolSize,
            SoundFileReader soundFileReader,
            SoundFileWriter soundFileWriter) {
        this.statusBar = statusBar;
        this.threadPoolSize = threadPoolSize;
        this.soundFileReader = soundFileReader;
        this.soundFileWriter = soundFileWriter;
    }

    @Override
    public void startRender(double startDelta) {
        futures.clear();
        if (threadPool != null && !threadPool.isTerminated()) {
            threadPool.shutdownNow();
        }
        threadPool = Executors.newFixedThreadPool(threadPoolSize);
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

        final double finalOverlap = boundedOverlap;
        final int numSamples = Math.max(0, msToNumSamples(noteLength));
        futures.add(threadPool.submit(() -> {
            // Files are often not ready to read immediately after the resampler finishes.
            // Putting a wait here reduces data race errors without significantly impacting
            // render time.
            Thread.sleep(100);
            Optional<WavData> wavData = soundFileReader.loadWavData(inputFile, message -> null);
            if (wavData.isEmpty()) {
                System.out.println("Warning: Unable to read WAV data: " + inputFile.getName());
                WavData silenceWav = new WavData(noteLength, new double[numSamples]);
                return new Pair<>(0.0, silenceWav);
            }
            if (wavData.get().getLengthMs() < noteLength) {
                // Will truncate or pad with zeroes to get to the desired length.
                System.out.println("Warning: Input not is not long enough: "
                        + wavData.get().getLengthMs() + " < " + noteLength);
            }
            WavData truncatedWav =
                    new WavData(noteLength, Arrays.copyOf(wavData.get().getSamples(), numSamples));
            WavData scaledWav = applyEnvelope(truncatedWav, note.getEnvelope());

            return new Pair<>(finalOverlap, scaledWav);
        }));
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
        final double finalDuration = duration;
        final int numSamples = msToNumSamples(duration);
        totalDelta += duration;

        futures.add(threadPool.submit(() -> {
            WavData silenceWav = new WavData(finalDuration, new double[numSamples]);
            return new Pair<>(0.0, silenceWav);
        }));
        if (triggerSynthesis) {
            saveToOutputFile(outputFile);
        }
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

        // Ensure envelope values are strictly and don't exceed note length.
        xValues[1] = Math.min(Math.max(xValues[1], xValues[0]), wavData.getSamples().length);
        xValues[2] = Math.min(Math.max(xValues[2], xValues[1]), wavData.getSamples().length);
        xValues[3] = Math.min(Math.max(xValues[3], xValues[2]), wavData.getSamples().length);
        xValues[4] = Math.min(Math.max(xValues[4], xValues[3]), wavData.getSamples().length);
        xValues[5] = Math.min(Math.max(xValues[5], xValues[4]), wavData.getSamples().length);
        xValues[6] = Math.min(Math.max(xValues[6], xValues[5]), wavData.getSamples().length);
        xValues[7] = Math.min(Math.max(xValues[7], xValues[6]), wavData.getSamples().length);

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

    // Asyncronously apply synthesis and return the comined wav.
    private WavData synthesize() throws ExecutionException, InterruptedException {
        double durationMs = totalDelta - startDelta;
        int numSamples = msToNumSamples(durationMs);
        double[] combinedSamples = new double[numSamples];
        int curSample = 0;
        for (int i = 0; i < futures.size(); i++) {
            double curProgress = i * 1.0 / futures.size();
            statusBar.setProgressAsync(curProgress);

            WavData fragment = futures.get(i).get().getValue();
            double overlapMs = futures.get(i).get().getKey();
            int overlapSamples = msToNumSamples(overlapMs);
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
        return new WavData(durationMs, combinedSamples);
    }

    private void saveToOutputFile(File outputFile) {
        try {
            WavData combinedWav = synthesize();
            soundFileWriter.writeWavData(combinedWav, outputFile);
        } catch (InterruptedException | ExecutionException e) {
            System.out.println("Wavtool run failed or was canceled.");
            errorLogger.logError(e);
            threadPool.shutdownNow();  // Cancel all other tasks.
        }
        threadPool.shutdown();
    }

    private static int msToNumSamples(double lengthMs) {
        // Convert milliseconds to samples, assuming a sample rate of 44,100 Hz.
        double sampleRate = 44100;
        int msPerSecond = 1000;
        return RoundUtils.round(lengthMs / msPerSecond * sampleRate);
    }
}
