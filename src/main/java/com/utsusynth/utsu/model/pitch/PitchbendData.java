package com.utsusynth.utsu.model.pitch;

import com.google.common.collect.ImmutableList;

public class PitchbendData {
	private final ImmutableList<Double> pbs; // Pitch bend start.
	private final ImmutableList<Double> pbw; // Pitch bend widths
	private final ImmutableList<Double> pby; // Pitch bend shifts
	private final ImmutableList<String> pbm; // Pitch bend curves
	// TODO: Add vibrato
	
	public PitchbendData(
			ImmutableList<Double> pbs,
			ImmutableList<Double> pbw,
			ImmutableList<Double> pby,
			ImmutableList<String> pbm) {
		this.pbs = pbs;
		this.pbw = pbw;
		this.pby = pby;
		this.pbm = pbm;
	}
	
	public ImmutableList<Double> getPBS() {
		return pbs;
	}
	
	public ImmutableList<Double> getPBW() {
		return pbw;
	}
	
	public ImmutableList<Double> getPBY() {
		return pby;
	}
	
	public ImmutableList<String> getPBM() {
		return pbm;
	}
}
