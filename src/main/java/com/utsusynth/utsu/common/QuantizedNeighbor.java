package com.utsusynth.utsu.common;

public class QuantizedNeighbor {
	private final int quantizedDelta;
	private final int quantization;

	public QuantizedNeighbor(int quantizedDelta, int quantization) {
		this.quantizedDelta = quantizedDelta;
		this.quantization = quantization;
	}

	public int getDelta() {
		return this.quantizedDelta;
	}

	public int getQuantization() {
		return this.quantization;
	}
}
