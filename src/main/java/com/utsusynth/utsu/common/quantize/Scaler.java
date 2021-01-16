package com.utsusynth.utsu.common.quantize;

import javafx.beans.binding.DoubleBinding;

public interface Scaler {
    DoubleBinding scaleX(int scaleMe);
    DoubleBinding scaleX(double scaleMe);
    DoubleBinding scalePos(int scaleMe);
    DoubleBinding scalePos(double scaleMe);
    DoubleBinding scaleY(int scaleMe);
    DoubleBinding scaleY(double scaleMe);
    double unscaleX(double unscaleMe);
    double unscalePos(double unscaleMe);
    double unscaleY(double unscaleMe);
    Scaler derive(double horizontalMultiplier, double verticalMultiplier);
}
