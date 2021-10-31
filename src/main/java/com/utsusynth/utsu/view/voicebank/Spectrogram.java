package com.utsusynth.utsu.view.voicebank;

import com.utsusynth.utsu.common.data.WavData;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Draws a wave spectrogram for the WAV files of lyrics.
 */
public class Spectrogram {
    /**
     * Size of each window FFT is applied on. Must be a power of 2.
     */
    public static final int WAV_WINDOW_SIZE = 2048;

    /**
     * Distance between each window. Must be a power of 2.
     */
    public static final int WAV_HOP_SIZE = 512;

    /**
     * Minimum pitch on y-axis of spectrogram, in Mel units.
     */
    public static final int MIN_MEL = 0;

    /**
     * Maximum pitch on y-axis of spectrogram, in Mel units.
     */
    public static final int MAX_MEL = 3200;

    public Spectrogram() {
        // TODO: Inject any necessary tools.
    }

    public Image createSpectrogram(WavData wavData, int height) {
        List<Color> colorScale = createColorScale(height);
        List<Double> freqFilters = createFreqFilters(height); // Y-axis in Hz.
        return new WritableImage(10, height);
    }

    private List<Color> createColorScale(int height) {
        // Use one color for every pixel of height, so scale can be drawn on the side if needed.
        ArrayList<Color> colorScale = new ArrayList<>(height);
        int blackToColorSteps = height / 2;
        for (int i = 0; i < blackToColorSteps; i++) {
            colorScale.add(Color.BLACK.interpolate(Color.AZURE, i * 1.0 / blackToColorSteps));
        }
        int colorToWhiteSteps = height - blackToColorSteps; // Can handle an odd height.
        for (int i = 0; i < colorToWhiteSteps; i++) {
            colorScale.add(Color.AZURE.interpolate(Color.WHITE, i * 1.0 / colorToWhiteSteps));
        }
        return colorScale;
    }

    private List<Double> createFreqFilters(int height) {
        // Create an evenly-spaced axis of mels, sized for the height of the spectrogram.
        ArrayList<Double> melFilters = new ArrayList<>(height + 2);
        for (int i = 0; i < height + 2; i++) {
            melFilters.add(MIN_MEL + (i / (height + 1.0) * MAX_MEL));
        }
        // Convert from mels to Hz.
        return melFilters.stream()
                .map(Spectrogram::inverseMelTransform)
                .collect(Collectors.toList());
    }

    private static double melTransform(double freqInHz) {
        return 1127.0 * Math.log(1 + (freqInHz / 700.0)); // Natural log.
    }

    private static double inverseMelTransform(double freqInMels) {
        return 700.0 * (Math.pow(Math.E, freqInMels / 1127.0) - 1);
    }
}
