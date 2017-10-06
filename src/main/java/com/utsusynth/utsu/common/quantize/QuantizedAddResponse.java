package com.utsusynth.utsu.common.quantize;

import com.google.common.base.Optional;

public class QuantizedAddResponse {
	private final Optional<String> trueLyric;
	private final Optional<QuantizedEnvelope> envelope;
	private final Optional<QuantizedPitchbend> pitchbend;
	private final Optional<QuantizedNeighbor> prevNote;
	private final Optional<QuantizedNeighbor> nextNote;

	public QuantizedAddResponse(
			Optional<String> trueLyric,
			Optional<QuantizedEnvelope> envelope,
			Optional<QuantizedPitchbend> pitchbend,
			Optional<QuantizedNeighbor> prevNote,
			Optional<QuantizedNeighbor> nextNote) {
		this.trueLyric = trueLyric;
		this.envelope = envelope;
		this.pitchbend = pitchbend;
		this.prevNote = prevNote;
		this.nextNote = nextNote;
	}

	public Optional<String> getTrueLyric() {
		return this.trueLyric;
	}

	public Optional<QuantizedEnvelope> getEnvelope() {
		return this.envelope;
	}

	public Optional<QuantizedPitchbend> getPitchbend() {
		return this.pitchbend;
	}

	public Optional<QuantizedNeighbor> getPrevNote() {
		return this.prevNote;
	}

	public Optional<QuantizedNeighbor> getNextNote() {
		return this.nextNote;
	}
}
