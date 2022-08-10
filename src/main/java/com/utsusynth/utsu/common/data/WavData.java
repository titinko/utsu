package com.utsusynth.utsu.common.data;

/** Data for a single wav file. */
public class WavData {
    private final double lengthMs;
    private final double[] samples;

    public WavData(double lengthMs, double[] samples) {
        this.lengthMs = lengthMs;
        this.samples = samples;
    }

    public double getLengthMs() {
        return lengthMs;
    }

    public double[] getSamples() {
        return samples;
    }

    public double getSamplesPerMs() {
        return samples.length / lengthMs;
    }
}
