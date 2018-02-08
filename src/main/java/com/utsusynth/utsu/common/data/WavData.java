package com.utsusynth.utsu.common.data;

/** Data for a single wav file. */
public class WavData {
    private final double lengthMs;
    private final int[] samples;

    public WavData(double lengthMs, int[] samples) {
        this.lengthMs = lengthMs;
        this.samples = samples;
    }

    public double getLengthMs() {
        return lengthMs;
    }

    public int[] getSamples() {
        return samples;
    }
}
