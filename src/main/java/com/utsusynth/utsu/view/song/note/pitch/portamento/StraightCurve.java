package com.utsusynth.utsu.view.song.note.pitch.portamento;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;

/** Represents a portamento that is a straight line. */
public class StraightCurve implements Curve {
    private final DoubleProperty startX;
    private final DoubleProperty startY;
    private final DoubleProperty endX;
    private final DoubleProperty endY;

    StraightCurve(double startX, double startY, double endX, double endY) {
        this.startX = new SimpleDoubleProperty(startX);
        this.startY = new SimpleDoubleProperty(startY);
        this.endX = new SimpleDoubleProperty(endX);
        this.endY = new SimpleDoubleProperty(endY);
    }

    @Override
    public Shape redraw(double offsetX) {
        Line line = new Line();
        line.getStyleClass().add("pitchbend");
        line.startXProperty().bind(startX.subtract(offsetX));
        line.startYProperty().bind(startY);
        line.endXProperty().bind(endX.subtract(offsetX));
        line.endYProperty().bind(endY);
        return line;
    }

    @Override
    public double getStartX() {
        return startX.get();
    }

    @Override
    public double getStartY() {
        return startY.get();
    }

    @Override
    public double getEndX() {
        return endX.get();
    }

    @Override
    public double getEndY() {
        return endY.get();
    }

    @Override
    public void bindStart(ReadOnlyDoubleProperty xProperty, ReadOnlyDoubleProperty yProperty) {
        startX.bind(xProperty);
        startY.bind(yProperty);
    }

    @Override
    public void bindEnd(ReadOnlyDoubleProperty xProperty, ReadOnlyDoubleProperty yProperty) {
        endX.bind(xProperty);
        endY.bind(yProperty);
    }

    @Override
    public String getType() {
        return "s";
    }
}
