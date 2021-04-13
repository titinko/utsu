package com.utsusynth.utsu.view.song.note.pitch.portamento;

import javafx.scene.paint.Color;
import javafx.scene.shape.CubicCurve;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;

/** Represents a portamento that is an r-shaped curve. */
public class RCurve implements Curve {
    private final CubicCurve curve;

    RCurve(double startX, double startY, double endX, double endY) {
        double halfX = (startX + endX) / 2;
        double halfY = (startY + endY) / 2;
        this.curve = new CubicCurve(startX, startY, startX, halfY, halfX, endY, endX, endY);
        this.curve.getStyleClass().add("pitchbend");
    }

    @Override
    public Shape redraw(double offsetX) {
        return curve;
    }

    @Override
    public double getStartX() {
        return curve.getStartX();
    }

    @Override
    public double getStartY() {
        return curve.getStartY();
    }

    @Override
    public double getEndX() {
        return curve.getEndX();
    }

    @Override
    public double getEndY() {
        return curve.getEndY();
    }

    @Override
    public void bindStart(Rectangle controlPoint, double offsetX) {
        controlPoint.xProperty().addListener(event -> {
            double centerX = controlPoint.getX() + 2;
            curve.setStartX(centerX);
            curve.setControlX1(centerX);
            curve.setControlX2((centerX + curve.getEndX()) / 2);
        });
        controlPoint.yProperty().addListener(event -> {
            double centerY = controlPoint.getY() + 2;
            curve.setStartY(centerY);
            curve.setControlY1((centerY + curve.getEndY()) / 2);
        });
    }

    @Override
    public void bindEnd(Rectangle controlPoint, double offsetX) {
        controlPoint.xProperty().addListener(event -> {
            double centerX = controlPoint.getX() + 2;
            curve.setEndX(centerX);
            curve.setControlX2((curve.getStartX() + centerX) / 2);
        });
        controlPoint.yProperty().addListener(event -> {
            double centerY = controlPoint.getY() + 2;
            curve.setEndY(centerY);
            curve.setControlY1((curve.getStartY() + centerY) / 2);
            curve.setControlY2(centerY);
        });
    }

    @Override
    public String getType() {
        return "r";
    }
}
