package com.utsusynth.utsu.common.quantize;

public class QuantizedNeighbor {
	private final int quantizedDelta;
	private final int quantization;
	private final QuantizedEnvelope envelope;

	public QuantizedNeighbor(int quantizedDelta, int quantization, QuantizedEnvelope envelope) {
		this.quantizedDelta = quantizedDelta;
		this.quantization = quantization;
		this.envelope = envelope;
	}

	public int getDelta() {
		return this.quantizedDelta;
	}

	public int getQuantization() {
		return this.quantization;
	}

	public QuantizedEnvelope getEnvelope() {
		return this.envelope;
	}
}
