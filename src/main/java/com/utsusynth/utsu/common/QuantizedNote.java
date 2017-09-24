package com.utsusynth.utsu.common;

public class QuantizedNote {
	private final int quantizedStart;
	private final int quantizedDuration;
	private final int quantization;

	public QuantizedNote(int quantizedStart, int quantizedDuration, int quantization) {
		this.quantizedStart = quantizedStart;
		this.quantizedDuration = quantizedDuration;
		this.quantization = quantization;
	}

	public int getStart() {
		return this.quantizedStart;
	}

	public int getDuration() {
		return this.quantizedDuration;
	}

	public int getQuantization() {
		return this.quantization;
	}
}
