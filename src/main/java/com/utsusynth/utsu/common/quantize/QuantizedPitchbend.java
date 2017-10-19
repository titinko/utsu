package com.utsusynth.utsu.common.quantize;

import com.google.common.base.Optional;

public class QuantizedPitchbend {
	private final QuantizedPortamento portamento;
	private final Optional<QuantizedVibrato> vibrato;

	public QuantizedPitchbend(QuantizedPortamento portamento, Optional<QuantizedVibrato> vibrato) {
		this.portamento = portamento;
		this.vibrato = vibrato;
	}

	public QuantizedPortamento getPortamento() {
		return portamento;
	}

	public Optional<QuantizedVibrato> getVibrato() {
		return vibrato;
	}
}
