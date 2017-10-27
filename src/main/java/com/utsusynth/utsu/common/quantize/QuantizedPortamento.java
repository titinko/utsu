package com.utsusynth.utsu.common.quantize;

import com.google.common.collect.ImmutableList;

public class QuantizedPortamento {
	public static final int QUANTIZATION = 32;

	private final String prevPitch; // Example: C#4
	private final int start; // In quants.
	private final ImmutableList<Integer> widths; // In quants.
	private final ImmutableList<Double> pitchShifts; // In 1/10 a semitone.
	private final ImmutableList<String> curves; // "s" or "r" or "j" or ""

	public QuantizedPortamento(
			String prevPitch,
			int start,
			ImmutableList<Integer> widths,
			ImmutableList<Double> pitchShifts,
			ImmutableList<String> curves) {
		this.prevPitch = prevPitch;
		this.start = start;
		this.widths = widths;
		this.pitchShifts = pitchShifts;
		this.curves = curves;
	}

	public String getPrevPitch() {
		return prevPitch;
	}

	public int getStart() {
		return start;
	}

	public int getNumWidths() {
		return widths.size();
	}

	public int getWidth(int index) {
		// TODO: Handle index out of bounds exception.
		return widths.get(index);
	}

	public ImmutableList<Double> getShifts() {
		return pitchShifts;
	}

	public double getShift(int index) {
		// TODO: Handle index out of bounds exception.
		return pitchShifts.get(index);
	}

	public ImmutableList<String> getCurves() {
		return curves;
	}

	public String getCurve(int index) {
		if (index >= curves.size()) {
			return "";
		}
		return curves.get(index);
	}
}
