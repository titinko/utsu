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
    public DoubleBinding scaleX(int scaleMe) {
        return horizontalScale.multiply(scaleMe);
    }

    @Override
    public DoubleBinding scaleX(double scaleMe) {
        return horizontalScale.multiply(scaleMe);
    }

    @Override
    public DoubleBinding scalePos(int scaleMe) {
        return horizontalScale.multiply(Quantizer.COL_WIDTH * 4 + scaleMe);
    }

    @Override
    public DoubleBinding scalePos(double scaleMe) {
        return horizontalScale.multiply(Quantizer.COL_WIDTH * 4 + scaleMe);
    }

    @Override
    public DoubleBinding scaleY(int scaleMe) {
        return verticalScale.multiply(scaleMe);
    }

    @Override
    public DoubleBinding scaleY(double scaleMe) {
        return verticalScale.multiply(scaleMe);
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
