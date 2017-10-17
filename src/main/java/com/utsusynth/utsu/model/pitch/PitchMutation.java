package com.utsusynth.utsu.model.pitch;

public interface PitchMutation {
	/** Returns pitch value (in 1/10 of a semitone) for this position. */
	double apply(int positionMs);
}
