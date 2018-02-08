package com.utsusynth.utsu.common.data;

/** Frequency data for a single wav file. */
public class FrequencyData {
    private final double averageFreq; // Average F0 (pitch) value.
    private final int samplesPerFreqValue; // This is always 256.
    private final double[] frequencies;
    private final double[] amplitudes;

    public FrequencyData(
            double averageFreq,
            int samplesPerFreqValue,
            double[] frequencies,
            double[] amplitudes) {
        this.averageFreq = averageFreq;
        this.samplesPerFreqValue = samplesPerFreqValue;
        this.frequencies = frequencies;
        this.amplitudes = amplitudes;
    }

    public double getAverageFreq() {
        return averageFreq;
    }

    public int getSamplesPerFreqValue() {
        return samplesPerFreqValue;
    }

    public double[] getFrequencies() {
        return frequencies;
    }

    public double[] getAmplitudes() {
        return amplitudes;
    }
}
