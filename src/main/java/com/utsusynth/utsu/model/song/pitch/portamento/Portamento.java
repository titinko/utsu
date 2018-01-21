package com.utsusynth.utsu.model.song.pitch.portamento;

import com.utsusynth.utsu.model.song.pitch.PitchMutation;

public abstract class Portamento implements PitchMutation {
	// Gets the first pitch in the portamento.
	public abstract double getStartPitch();

	// Gets the last pitch in the portamento.
	public abstract double getEndPitch();
}
