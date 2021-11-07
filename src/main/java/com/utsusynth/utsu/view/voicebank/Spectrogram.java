package com.utsusynth.utsu.view.voicebank;

import com.google.common.collect.ImmutableList;
import com.utsusynth.utsu.common.data.WavData;
import com.utsusynth.utsu.common.utils.Complex;
import com.utsusynth.utsu.common.utils.FFTUtils;
import com.utsusynth.utsu.common.utils.RoundUtils;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws a wave spectrogram for the WAV files of lyrics.
 */
public class Spectrogram {
    /**
     * Size of each window FFT is applied on. Must be a power of 2.
     */
    private static final int WAV_WINDOW_SIZE = 1024;

    /**
     * Distance between each window. Must be a power of 2.
     */
    private static final int WAV_HOP_SIZE = 1024;

    /**
     * Default color of the spectrogram.
     */
    private static final Color DEFAULT_COLOR = Color.CORNFLOWERBLUE;

    public Spectrogram() {
        // TODO: Inject any necessary tools.
    }

    public Image createSpectrogram(WavData wavData, int height) {
        int numSamples = wavData.getSamples().length;
        double sampleRate = numSamples / (wavData.getLengthMs() / 1000);

        List<Color> colorScale = createColorScale(height);
        List<Integer> freqFilters = createFreqFilters(height, sampleRate);

        int totalNumHops = (numSamples + WAV_HOP_SIZE - WAV_WINDOW_SIZE) / WAV_HOP_SIZE;

        WritableImage spectrogram = new WritableImage(totalNumHops, height);
        // TODO: Do this for every hop in parallel.
        for (int hop = 0; hop < totalNumHops; hop++) {
            double[] window = FFTUtils.hammingWindow(
                    wavData.getSamples(), hop * WAV_HOP_SIZE, WAV_WINDOW_SIZE);
            Complex[] frequencies = FFTUtils.fft(FFTUtils.toComplex(window)); // FFT results.
            double[] magnitudes = new double[height]; // Magnitude at each pixel.
            double windowMin = Integer.MAX_VALUE; // Minimum magnitude of any pixel.
            for (int i = 0; i < magnitudes.length; i++) {
                int startBand = freqFilters.get(i);
                int endBand = freqFilters.get(i + 2);
                int numBands = endBand - startBand;
                int halfBands = numBands / 2;

                double maxMagnitude = Integer.MIN_VALUE;
                for (int band = startBand; band < endBand; band++) {
                    double magnitude;
                    double fftResult =
                            Math.sqrt((frequencies[band].getReal() * frequencies[band].getReal() )
                                    + (frequencies[band].getImaginary()
                                    * frequencies[band].getImaginary()));
                    // Shrink the FFT result by the triangle frequency filter.
                    if (band <= startBand + halfBands) {
                        magnitude = (band - startBand + 1.0) / (halfBands + 1.0) * fftResult;
                    } else {
                        magnitude = (numBands - (band - startBand)) * 1.0 / halfBands * fftResult;
                    }
                    if (magnitude > maxMagnitude) {
                        maxMagnitude = magnitude;
                    }
                }
                magnitudes[i] = maxMagnitude == 0 ? 0 : 20 * Math.log10(maxMagnitude);

                if (magnitudes[i] < windowMin) {
                    windowMin = magnitudes[i];
                }
            }

            if (windowMin < 0) {
                for (int i = 0; i < magnitudes.length; i++) {
                    magnitudes[i] -= windowMin;
                }
            }

            for (int i = 0; i < magnitudes.length; i++) {
                int colorIndex = Math.max(0, Math.min(height - 1,
                        RoundUtils.round(magnitudes[i] * height / 100.0)));
                spectrogram.getPixelWriter().setColor(
                        hop, height - (i + 1), colorScale.get(colorIndex));
            }
        }
        return spectrogram;
    }

    private List<Color> createColorScale(int numColors) {
        // Num colors is a multiple of height, so scale can be drawn on the side if needed.
        ArrayList<Color> colorScale = new ArrayList<>(numColors);
        int blackToColorSteps = numColors / 2;
        for (int i = 0; i < blackToColorSteps; i++) {
            colorScale.add(Color.BLACK.interpolate(DEFAULT_COLOR, i * 1.0 / blackToColorSteps));
        }
        int colorToWhiteSteps = numColors - blackToColorSteps;
        for (int i = 0; i < colorToWhiteSteps; i++) {
            colorScale.add(DEFAULT_COLOR.interpolate(Color.WHITE, i * 1.0 / colorToWhiteSteps));
        }
        return colorScale;
    }

    private ImmutableList<Integer> createFreqFilters(int height, double sampleRate) {
        // Min cycle length in seconds. FFT frequency bands will be based on this value.
        double minCycleLen = WAV_WINDOW_SIZE / sampleRate;
        int numBins = WAV_WINDOW_SIZE / 2; // Number of frequency bins returned by FFT.

        // Create an evenly-spaced axis of mels, sized for the height of the spectrogram.
        double maxMel = melTransform(numBins / minCycleLen);
        double minMel = melTransform(1.0 / minCycleLen);
        ArrayList<Double> hzFilters = new ArrayList<>(height + 2);
        for (int i = 0; i < height + 2; i++) {
            double melFilter = minMel + (i / (height + 1.0) * (maxMel - minMel));
            hzFilters.add(inverseMelTransform(melFilter)); // Convert from mels to Hz.
        }

        // Convert from Hz to the nearest preceding index in FFT output.
        Integer[] freqFilters = new Integer[height + 2];
        int index = 0;
        for (int fftIndex = 1; fftIndex <= numBins; fftIndex++) {
            double freqInHz = fftIndex / minCycleLen;
            if (freqInHz >= hzFilters.get(index)) {
                freqFilters[index] = fftIndex - 1;
                index++;
            }
            if (index == height + 2) {
                break;
            }
        }
        freqFilters[height + 1] = numBins;
        return ImmutableList.copyOf(freqFilters);
    }

    private static double melTransform(double freqInHz) {
        return 1127.0 * Math.log(1 + (freqInHz / 700.0)); // Natural log.
    }

    private static double inverseMelTransform(double freqInMels) {
        return 700.0 * (Math.pow(Math.E, freqInMels / 1127.0) - 1);
    }
}
