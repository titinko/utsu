package com.utsusynth.utsu.view.song.note.pitch.portamento;

import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.paint.Color;
import javafx.scene.shape.CubicCurve;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;

/** Represents a portamento that is a j-shaped curve. */
public class JCurve implements Curve {
    private final DoubleProperty startX;
    private final DoubleProperty startY;
    private final DoubleProperty endX;
    private final DoubleProperty endY;

    JCurve(double startX, double startY, double endX, double endY) {
        this.startX = new SimpleDoubleProperty(startX);
        this.startY = new SimpleDoubleProperty(startY);
        this.endX = new SimpleDoubleProperty(endX);
        this.endY = new SimpleDoubleProperty(endY);
    }

    @Override
    public Shape redraw(double offsetX) {
        DoubleBinding halfX = startX.add(endX).divide(2);
        DoubleBinding halfY = startY.add(endY).divide(2);

        CubicCurve curve = new CubicCurve();
        curve.getStyleClass().add("pitchbend");
        curve.startXProperty().bind(startX.subtract(offsetX));
        curve.startYProperty().bind(startY);
        curve.controlX1Property().bind(halfX.subtract(offsetX));
        curve.controlY1Property().bind(startY);
        curve.controlX2Property().bind(endX.subtract(offsetX));
        curve.controlY2Property().bind(halfY);
        curve.endXProperty().bind(endX.subtract(offsetX));
        curve.endYProperty().bind(endY);
        return curve;
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
        return "j";
    }
}
