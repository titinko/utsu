package com.utsusynth.utsu.view.song.note.pitch.portamento;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.shape.Rectangle;

public class ControlPoint {
    private static final double RADIUS = 2;
    private final DoubleProperty centerX;
    private final DoubleProperty centerY;

    ControlPoint(double centerX, double centerY) {
        this.centerX = new SimpleDoubleProperty(centerX);
        this.centerY = new SimpleDoubleProperty(centerY);
    }

    Rectangle redraw(double offsetX) {
        Rectangle square = new Rectangle();
        square.xProperty().bind(centerX.subtract(offsetX + RADIUS));
        square.yProperty().bind(centerY.subtract(RADIUS));
        square.setWidth(RADIUS * 2);
        square.setHeight(RADIUS * 2);
        return square;
    }

    ReadOnlyDoubleProperty centerXProperty() {
        return centerX;
    }

    ReadOnlyDoubleProperty centerYProperty() {
        return centerY;
    }
}
