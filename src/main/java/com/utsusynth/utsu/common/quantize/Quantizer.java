package com.utsusynth.utsu.common.quantize;

public class Quantizer {
	public static final int SMALLEST = 32;
	public static final int LARGEST = 1;

	public static final int DEFAULT_NOTE_DURATION = 480;
	public static final int ROW_HEIGHT = 20;

	public static final int MIN_COL_WIDTH = SMALLEST;
	public static final int COL_WIDTH_INDREMENT = MIN_COL_WIDTH;
	public static final int MAX_COL_WIDTH = 288;

	private int quantization;
	private int colWidth;

	public Quantizer(int defaultQuantization, int defaultColWidth) {
		this.quantization = defaultQuantization;
		this.colWidth = defaultColWidth;
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

	public int getColWidth() {
		return colWidth;
	}

	public void changeColWidth(int oldColWidth, int newColWidth) {
		if (oldColWidth != colWidth) {
			// TODO: Handle this better.
			System.out.println("ERROR: Data race when changing column width!");
		}
		colWidth = newColWidth;
	}
}
