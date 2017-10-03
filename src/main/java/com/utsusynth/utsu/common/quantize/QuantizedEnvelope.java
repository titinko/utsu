package com.utsusynth.utsu.common.quantize;

import com.google.common.collect.ImmutableList;

public class QuantizedEnvelope {
	public static final int QUANTIZATION = 96;

	private final int preutter; // Preutterance in quants
	private final ImmutableList<Integer> width; // "p" in quants
	private final ImmutableList<Integer> height; // "v" in % of total intensity (0-100)

	public QuantizedEnvelope(double[] envelopeWidth, double[] envelopeHeight) {
		this.preutter = 0;
		ImmutableList.Builder<Integer> widthBuilder = ImmutableList.builder();
		for (double width : envelopeWidth) {
			int factor = Quantizer.DEFAULT_NOTE_DURATION / QUANTIZATION;
			widthBuilder.add((int) (width / factor));
		}
		this.width = widthBuilder.build();
		ImmutableList.Builder<Integer> heightBuilder = ImmutableList.builder();
		for (double height : envelopeHeight) {
			heightBuilder.add((int) Math.max(0, Math.min(200, Math.round(height))));
		}
		this.height = heightBuilder.build();
	}

	public int getPreutterance() {
		return preutter;
	}

	public int getWidth(int index) {
		if (width.size() <= index) {
			// Default envelope width.
			return 0;
		}
		return width.get(index);
	}

	public int getHeight(int index) {
		if (height.size() <= index) {
			// Default envelope height.
			return 100;
		}
		return height.get(index);
	}
}
