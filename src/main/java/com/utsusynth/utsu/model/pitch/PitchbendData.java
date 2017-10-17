package com.utsusynth.utsu.model.pitch;

import com.google.common.collect.ImmutableList;
import com.utsusynth.utsu.common.quantize.QuantizedPitchbend;
import com.utsusynth.utsu.common.quantize.Quantizer;

public class PitchbendData {
	private final ImmutableList<Double> pbs; // Pitch bend start.
	private final ImmutableList<Double> pbw; // Pitch bend widths
	private final ImmutableList<Double> pby; // Pitch bend shifts
	private final ImmutableList<String> pbm; // Pitch bend curves
	private final int[] vibrato;

	public static PitchbendData fromQuantized(QuantizedPitchbend qPitchbend) {
		double quantSize = Quantizer.DEFAULT_NOTE_DURATION / QuantizedPitchbend.QUANTIZATION;
		ImmutableList<Double> pbs = ImmutableList.of(qPitchbend.getStart() * quantSize, 0.0);
		ImmutableList.Builder<Double> pbwBuilder = ImmutableList.builder();
		for (int i = 0; i < qPitchbend.getNumWidths(); i++) {
			pbwBuilder.add(qPitchbend.getWidth(i) * quantSize);
		}
		return new PitchbendData(
				pbs,
				pbwBuilder.build(),
				qPitchbend.getShifts(),
				qPitchbend.getCurves(),
				new int[8]);
	}

	public PitchbendData(
			ImmutableList<Double> pbs,
			ImmutableList<Double> pbw,
			ImmutableList<Double> pby,
			ImmutableList<String> pbm,
			int[] vibrato) {
		this.pbs = pbs;
		this.pbw = pbw;
		this.pby = pby;
		this.pbm = pbm;
		this.vibrato = vibrato;
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

	public int getVibrato(int index) {
		if (index < 0 || index >= vibrato.length) {
			return 0;
		}
		return vibrato[index];
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
