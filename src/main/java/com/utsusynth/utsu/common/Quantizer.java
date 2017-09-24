package com.utsusynth.utsu.common;

public class Quantizer {
	public static final int SMALLEST = 32;
	public static final int LARGEST = 1;

	public static final int DEFAULT_NOTE_DURATION = 480;

	private int quantization;

	public Quantizer(int defaultQuantization) {
		this.quantization = defaultQuantization;
	}

	public int getQuant() {
		return quantization;
	}

	public void changeQuant(int oldQuant, int newQuant) {
		if (oldQuant != quantization) {
			// TODO: Handle this better.
			System.out.println("ERROR: Data race when changing quantization!");
		}
		quantization = newQuant;
	}
}
