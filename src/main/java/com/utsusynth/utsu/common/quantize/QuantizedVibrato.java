package com.utsusynth.utsu.common.quantize;

public class QuantizedVibrato {
	public static final int QUANTIZATION = 32;

	private final int breadth; // In percent of note.
	private final int cycleLength; // In quants.
	private final int amplitude; // In cents.
	private final int phaseIn; // In percent of breadth.
	private final int phaseOut; // In percent of breadth.
	private final int phase; // 0 to 100.
	private final double pitch; // In cents, -50 to 50.
	private final int freqSlope; // -100 to 100. Freq limits are 1/2 and 3/2 base frequency.

	public QuantizedVibrato(int[] ustVibrato) {
		assert (ustVibrato.length == 10);
		this.breadth = ustVibrato[0];
		this.cycleLength = ustVibrato[1] / (Quantizer.DEFAULT_NOTE_DURATION / QUANTIZATION);
		this.amplitude = ustVibrato[2];
		this.phaseIn = ustVibrato[3];
		this.phaseOut = ustVibrato[4];
		this.phase = ustVibrato[5];
		this.pitch = ustVibrato[6] / 2.0;
		this.freqSlope = ustVibrato[8];
	}

	public int[] toUstVibrato() {
		int[] ustVibrato = new int[10];
		ustVibrato[0] = breadth;
		ustVibrato[1] = cycleLength * (Quantizer.DEFAULT_NOTE_DURATION / QUANTIZATION);
		ustVibrato[2] = amplitude;
		ustVibrato[3] = phaseIn;
		ustVibrato[4] = phaseOut;
		ustVibrato[5] = phase;
		ustVibrato[6] = (int) (pitch * 2);
		ustVibrato[7] = 0;
		ustVibrato[8] = freqSlope;
		ustVibrato[9] = 0;
		return ustVibrato;
	}
}
