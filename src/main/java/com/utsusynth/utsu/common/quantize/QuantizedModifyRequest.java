package com.utsusynth.utsu.common.quantize;

import com.google.common.base.Optional;

public class QuantizedModifyRequest {
	private final QuantizedNote note;
	private final Optional<QuantizedEnvelope> envelope;

	public QuantizedModifyRequest(QuantizedNote note, Optional<QuantizedEnvelope> envelope) {
		this.note = note;
		this.envelope = envelope;
	}

	public QuantizedNote getNote() {
		return note;
	}

	public Optional<QuantizedEnvelope> getEnvelope() {
		return this.envelope;
	}
}
