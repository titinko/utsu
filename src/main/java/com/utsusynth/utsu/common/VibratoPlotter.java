package com.utsusynth.utsu.common;

/** Plots vibrato spec data into a sinusoidal wave. */
public class VibratoPlotter {
    private final double lengthMs;
    private final double phaseInMs;
    private final double phaseOutMs;
    private final double amplitudeCents;

    private final double baseFreq; // Frequency in radians/ms
    private final double startFreq; // Frequency at start of vibrato.
    private final double freqSlope; // Slope to apply to frequency, usually 0.

    public VibratoPlotter(
            double lengthMs,
            int cycleMs,
            int amplitude,
            int phaseIn,
            int phaseOut,
            int phasePercent,
            int pitchChange,
            int baseFreqSlope) {
        this.lengthMs = lengthMs;
        this.phaseInMs = phaseIn / 100.0 * lengthMs;
        this.phaseOutMs = phaseOut / 100.0 * lengthMs;
        this.amplitudeCents = amplitude;
        this.baseFreq = 2 * Math.PI / cycleMs;

        // Current min cycle length is .2 * base, current max is 1.8 * base.
        this.startFreq = baseFreq * (-1 * baseFreqSlope / 800.0 + 1);
        double endFreq = baseFreq * (baseFreqSlope / 800.0 + 1);
        this.freqSlope = lengthMs == 0 ? 0 : (endFreq - startFreq) / lengthMs;
    }

}
