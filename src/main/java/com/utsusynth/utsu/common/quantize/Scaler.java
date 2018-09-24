package com.utsusynth.utsu.common.quantize;

public class Scaler {
    public static final double MIN_HORIZONTAL_SCALE = 0.15;
    public static final double HORIZONTAL_SCALE_INDREMENT = 0.1;
    public static final double MAX_HORIZONTAL_SCALE = 0.45;

    public static final double MIN_VERTICAL_SCALE = 0.85;
    public static final double VERTICAL_SCALE_INDREMENT = 0.2;
    public static final double MAX_VERTICAL_SCALE = 1.95;

    private double horizontalScale;
    private double verticalScale;

    public Scaler(double defaultHorizontalScale, double defaultVerticalScale) {
        this.horizontalScale = defaultHorizontalScale;
        this.verticalScale = defaultVerticalScale;
    }

    public double scaleX(int scaleMe) {
        return horizontalScale * scaleMe;
    }

    public double scaleX(double scaleMe) {
        return horizontalScale * scaleMe;
    }

    public double scaleY(int scaleMe) {
        return verticalScale * scaleMe;
    }

    public double scaleY(double scaleMe) {
        return verticalScale * scaleMe;
    }

    public double unscaleX(double unscaleMe) {
        return unscaleMe / horizontalScale;
    }

    public double unscaleY(double unscaleMe) {
        return unscaleMe / verticalScale;
    }

    public double getHorizontalScale() {
        return horizontalScale;
    }

    public void changeHorizontalScale(double oldScale, double newScale) {
        if (oldScale != horizontalScale) {
            // TODO: Handle this better.
            System.out.println("ERROR: Data race when changing horizontal scale!");
        }
        horizontalScale = newScale;
    }

    public double getVerticalScale() {
        return verticalScale;
    }

    public void changeVerticalScale(double oldScale, double newScale) {
        if (oldScale != verticalScale) {
            // TODO: Handle this better.
            System.out.println("ERROR: Data race when changing vertical scale!");
        }
        verticalScale = newScale;
    }
}
