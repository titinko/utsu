package com.utsusynth.utsu.common.quantize;

public class Scaler {
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
}
