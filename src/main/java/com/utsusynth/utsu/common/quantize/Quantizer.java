package com.utsusynth.utsu.common.quantize;

public class Quantizer {
    public static final int DEFAULT_NOTE_DURATION = 480;
    public static final int COL_WIDTH = DEFAULT_NOTE_DURATION;
    public static final int ROW_HEIGHT = 20;

    /* The number of ms in one quant. */
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
