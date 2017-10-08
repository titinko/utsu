package com.utsusynth.utsu.common.quantize;

import com.google.common.base.Optional;

public class QuantizedModifyRequest {
	private final QuantizedNote note;
	private final Optional<QuantizedEnvelope> envelope;
	private final Optional<QuantizedPitchbend> pitchbend;

	public QuantizedModifyRequest(QuantizedNote note, QuantizedEnvelope envelope) {
		this.note = note;
		this.envelope = Optional.of(envelope);
		this.pitchbend = Optional.absent();
	}

	public QuantizedModifyRequest(QuantizedNote note, QuantizedPitchbend pitchbend) {
		this.note = note;
		this.envelope = Optional.absent();
		this.pitchbend = Optional.of(pitchbend);
	}

	public QuantizedNote getNote() {
		return note;
	}

	public Optional<QuantizedEnvelope> getEnvelope() {
		return this.envelope;
	}

	public Optional<QuantizedPitchbend> getPitchbend() {
		return this.pitchbend;
	}
}
