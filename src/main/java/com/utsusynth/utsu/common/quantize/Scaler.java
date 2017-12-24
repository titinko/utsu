package com.utsusynth.utsu.common.quantize;

public class Scaler {
    private double horizontalScale;
    private double verticalScale;

    public Scaler(double defaultHorizontalScale, double defaultVerticalScale) {
        this.horizontalScale = defaultHorizontalScale;
        this.verticalScale = defaultVerticalScale;
    }

    public double scaleHorizontal(int scaleMe) {
        return horizontalScale * scaleMe;
    }

    public double scaleVertical(int scaleMe) {
        return verticalScale * scaleMe;
    }

    public int unscaleHorizontal(double unscaleMe) {
        return (int) Math.round(unscaleMe / horizontalScale);
    }

    public int unscaleVertical(double unscaleMe) {
        return (int) Math.round(unscaleMe / verticalScale);
    }
}
