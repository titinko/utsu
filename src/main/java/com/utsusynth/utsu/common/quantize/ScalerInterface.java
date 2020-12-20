package com.utsusynth.utsu.common.quantize;

import javafx.beans.property.ReadOnlyDoubleProperty;

public interface ScalerInterface {
    ReadOnlyDoubleProperty scaleX(int scaleMe);
    ReadOnlyDoubleProperty scaleX(double scaleMe);
    ReadOnlyDoubleProperty scalePos(int scaleMe);
    ReadOnlyDoubleProperty scalePos(double scaleMe);
    ReadOnlyDoubleProperty scaleY(int scaleMe);
    ReadOnlyDoubleProperty scaleY(double scaleMe);
    double unscaleX(double unscaleMe);
    double unscalePos(double unscaleMe);
    double unscaleY(double unscaleMe);
}
