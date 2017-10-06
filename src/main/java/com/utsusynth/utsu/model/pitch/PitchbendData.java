package com.utsusynth.utsu.model.pitch;

import com.google.common.collect.ImmutableList;
import com.utsusynth.utsu.common.quantize.QuantizedPitchbend;
import com.utsusynth.utsu.common.quantize.Quantizer;

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

	public QuantizedPitchbend quantize(String prevPitch) {
		int quantSize = Quantizer.DEFAULT_NOTE_DURATION / QuantizedPitchbend.QUANTIZATION;
		int start = (int) Math.ceil(pbs.get(0) / quantSize);
		ImmutableList.Builder<Integer> widths = ImmutableList.builder();
		for (double width : pbw) {
			widths.add((int) Math.floor(width / quantSize));
		}
		return new QuantizedPitchbend(prevPitch, start, widths.build(), pby, pbm);
	}
}
