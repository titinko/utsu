package com.utsusynth.utsu.common.quantize;

import com.google.common.base.Optional;

public class QuantizedNeighbor {
	private final int quantizedDelta;
	private final int quantization;
	private final QuantizedEnvelope envelope;
	private final Optional<QuantizedPitchbend> pitchbend;

	public QuantizedNeighbor(
			int quantizedDelta,
			int quantization,
			QuantizedEnvelope envelope,
			Optional<QuantizedPitchbend> pitchbend) {
		this.quantizedDelta = quantizedDelta;
		this.quantization = quantization;
		this.envelope = envelope;
		this.pitchbend = pitchbend;
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

	public Optional<QuantizedPitchbend> getPitchbend() {
		return this.pitchbend;
	}
}
