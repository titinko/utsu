package com.utsusynth.utsu.common.quantize;

import javafx.beans.binding.DoubleBinding;

public interface Scaler {
    double scaleX(int scaleMe);
    double scaleX(double scaleMe);
    double scalePos(int scaleMe);
    double scalePos(double scaleMe);
    double scaleY(int scaleMe);
    double scaleY(double scaleMe);
    double unscaleX(double unscaleMe);
    double unscalePos(double unscaleMe);
    double unscaleY(double unscaleMe);
    Scaler derive(double horizontalMultiplier, double verticalMultiplier);
}
