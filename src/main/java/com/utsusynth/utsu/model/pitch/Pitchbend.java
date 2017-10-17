package com.utsusynth.utsu.model.pitch;

import com.google.common.base.Optional;
import com.utsusynth.utsu.model.pitch.portamento.Portamento;

class Pitchbend implements PitchMutation {
	private Optional<Portamento> portamento;
	private Optional<Vibrato> vibrato;

	static Pitchbend makePitchbend(Portamento portamento) {
		Pitchbend pitchbend = new Pitchbend();
		pitchbend.portamento = Optional.of(portamento);
		return pitchbend;
	}

	static Pitchbend makePitchbend(Vibrato vibrato) {
		Pitchbend pitchbend = new Pitchbend();
		pitchbend.vibrato = Optional.of(vibrato);
		return pitchbend;
	}

	private Pitchbend() {
		portamento = Optional.absent();
		vibrato = Optional.absent();
	}

	void addPortamento(Portamento portamento) {
		if (this.portamento.isPresent()) {
			// TODO: Handle this.
			System.out.println("Error: tried to add overlapping portamento.");
		} else {
			this.portamento = Optional.of(portamento);
		}
	}

	void removePortamento() {
		this.portamento = Optional.absent();
	}

	Optional<Portamento> getPortamento() {
		return portamento;
	}

	void addVibrato(Vibrato vibrato) {
		if (this.vibrato.isPresent()) {
			// TODO: Handle this.
			System.out.println("Error: tried to add overlapping vibrato.");
		} else {
			this.vibrato = Optional.of(vibrato);
		}
	}

	void removeVibrato() {
		this.vibrato = Optional.absent();
	}

	boolean isEmpty() {
		return !this.portamento.isPresent() && !this.vibrato.isPresent();
	}

	@Override
	public double apply(int positionMs) {
		double portamentoVal = 0;
		if (portamento.isPresent()) {
			// Portamento pitch is absolute.
			portamentoVal = portamento.get().apply(positionMs);
		}
		double vibratoVal = 0;
		if (vibrato.isPresent()) {
			// Vibrato pitch is centered on zero, meant to modify portamento pitch.
			vibratoVal = vibrato.get().apply(positionMs);
		}
		return portamentoVal + vibratoVal;
	}
}
