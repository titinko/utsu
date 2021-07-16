package com.utsusynth.utsu.common.quantize;

import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

public class ContinuousScaler implements Scaler {
    private final DoubleProperty horizontalScale;
    private final DoubleProperty verticalScale;

    public ContinuousScaler(double horizontalScale, double verticalScale) {
        this.horizontalScale = new SimpleDoubleProperty(horizontalScale);
        this.verticalScale = new SimpleDoubleProperty(verticalScale);
    }

    @Override
    public double scaleX(int scaleMe) {
        return horizontalScale.get() * scaleMe;
    }

    @Override
    public double scaleX(double scaleMe) {
        return horizontalScale.get() * scaleMe;
    }

    @Override
    public double scalePos(int scaleMe) {
        return horizontalScale.get() * (Quantizer.COL_WIDTH * 4 + scaleMe);
    }

    @Override
    public double scalePos(double scaleMe) {
        return horizontalScale.get() * (Quantizer.COL_WIDTH * 4 + scaleMe);
    }

    @Override
    public double scaleY(int scaleMe) {
        return verticalScale.get() * scaleMe;
    }

    @Override
    public double scaleY(double scaleMe) {
        return verticalScale.get() * scaleMe;
    }

    @Override
    public double unscaleX(double unscaleMe) {
        return unscaleMe / horizontalScale.get();
    }

    @Override
    public double unscalePos(double unscaleMe) {
        return (unscaleMe / horizontalScale.get()) - (Quantizer.COL_WIDTH * 4);
    }

    @Override
    public double unscaleY(double unscaleMe) {
        return unscaleMe / verticalScale.get();
    }

    @Override
    public Scaler derive(double horizontalMultiplier, double verticalMultiplier) {
        return new ContinuousScaler(
                horizontalScale.get() * horizontalMultiplier,
                verticalScale.get() * verticalMultiplier);
    }
}
