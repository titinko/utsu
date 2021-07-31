package com.utsusynth.utsu.common.data;

import java.util.Optional;

public class EnvelopeData {
    private final Optional<Double> envPreutter;
    private final Optional<Double> envLength;
    private final double[] widths; // "p" in milliseconds
    private final double[] heights; // "v" in % of total intensity (0-200)

    public EnvelopeData(double[] envWidths, double[] envHeights) {
        // TODO: Add more parameter checking here, don't just assume the inputs make sense.
        this.envPreutter = Optional.empty();
        this.envLength = Optional.empty();
        this.widths = envWidths;
        this.heights = envHeights;
    }

    public EnvelopeData(
            double envPreutter,
            double envLength,
            double[] envWidths,
            double[] envHeights) {
        this.envPreutter = Optional.of(envPreutter);
        this.envLength = Optional.of(envLength);
        this.widths = envWidths;
        this.heights = envHeights;
    }

    private EnvelopeData(
            Optional<Double> envPreutter,
            Optional<Double> envLength,
            double[] envWidths,
            double[] envHeights) {
        this.envPreutter = envPreutter;
        this.envLength = envLength;
        this.widths = envWidths;
        this.heights = envHeights;
    }

    public Optional<Double> getPreutter() {
        return envPreutter;
    }

    public Optional<Double> getLength() {
        return envLength;
    }

    public double[] getWidths() {
        return widths;
    }

    public double[] getHeights() {
        return heights;
    }

    public EnvelopeData deepcopy() {
        return new EnvelopeData(envPreutter, envLength, widths.clone(), heights.clone());
    }
}
