package com.utsusynth.utsu.common.quantize;

import com.google.common.collect.ImmutableList;

public class Scaler {
    public static final ImmutableList<Double> HORIZONTAL_SCALES =
            ImmutableList.of(0.05, 0.1, 0.15, 0.2, 0.25, 0.3);

    public static final ImmutableList<Double> VERTICAL_SCALES =
            ImmutableList.of(0.85, 1.0, 1.15, 1.3, 1.75);

    private int horizontalRank;
    private int verticalRank;

    public Scaler(int defaultHorizontalRank, int defaultVerticalRank) {
        this.horizontalRank = defaultHorizontalRank;
        this.verticalRank = defaultVerticalRank;
    }

    public double scaleX(int scaleMe) {
        return HORIZONTAL_SCALES.get(horizontalRank) * scaleMe;
    }

    public double scaleX(double scaleMe) {
        return HORIZONTAL_SCALES.get(horizontalRank) * scaleMe;
    }

    public double scalePos(int scaleMe) {
        return HORIZONTAL_SCALES.get(horizontalRank) * (Quantizer.COL_WIDTH * 4 + scaleMe);
    }

    public double scalePos(double scaleMe) {
        return HORIZONTAL_SCALES.get(horizontalRank) * (Quantizer.COL_WIDTH * 4 + scaleMe);
    }

    public double scaleY(int scaleMe) {
        return VERTICAL_SCALES.get(verticalRank) * scaleMe;
    }

    public double scaleY(double scaleMe) {
        return VERTICAL_SCALES.get(verticalRank) * scaleMe;
    }

    public double unscaleX(double unscaleMe) {
        return unscaleMe / HORIZONTAL_SCALES.get(horizontalRank);
    }

    public double unscalePos(double unscaleMe) {
        return (unscaleMe / HORIZONTAL_SCALES.get(horizontalRank)) - (Quantizer.COL_WIDTH * 4);
    }

    public double unscaleY(double unscaleMe) {
        return unscaleMe / VERTICAL_SCALES.get(verticalRank);
    }

    public int getHorizontalRank() {
        return horizontalRank;
    }

    /** Updates horizontal scale and returns whether update was successful. */
    public boolean changeHorizontalScale(int oldRank, int newRank) {
        if (oldRank != horizontalRank) {
            // TODO: Handle this better.
            System.out.println("ERROR: Data race when changing horizontal scale!");
        } else if (newRank < 0 || newRank >= HORIZONTAL_SCALES.size()) {
            return false;
        }
        horizontalRank = newRank;
        return true;
    }

    public int getVerticalRank() {
        return verticalRank;
    }

    /** Updates vertical scale and returns whether update was successful. */
    public boolean changeVerticalScale(int oldRank, int newRank) {
        if (oldRank != verticalRank) {
            // TODO: Handle this better.
            System.out.println("ERROR: Data race when changing vertical scale!");
        } else if (newRank < 0 || newRank >= VERTICAL_SCALES.size()) {
            return false;
        }
        verticalRank = newRank;
        return true;
    }
}
