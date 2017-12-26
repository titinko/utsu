package com.utsusynth.utsu.view.note.portamento;

import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;

/** Represents a portamento that is a straight line. */
public class StraightCurve implements Curve {
    private final Line line;

    StraightCurve(double startX, double startY, double endX, double endY) {
        this.line = new Line(startX, startY, endX, endY);
        this.line.setStroke(Color.DARKSLATEBLUE);
        this.line.setFill(Color.TRANSPARENT);
    }

    @Override
    public Shape getElement() {
        return line;
    }

    @Override
    public double getStartX() {
        return line.getStartX();
    }

    @Override
    public double getStartY() {
        return line.getStartY();
    }

    @Override
    public double getEndX() {
        return line.getEndX();
    }

    @Override
    public double getEndY() {
        return line.getEndY();
    }

    @Override
    public void bindStart(Rectangle controlPoint) {
        controlPoint.xProperty().addListener(event -> {
            double centerX = controlPoint.getX() + 2;
            line.setStartX(centerX);
        });
        controlPoint.yProperty().addListener(event -> {
            double centerY = controlPoint.getY() + 2;
            line.setStartY(centerY);
        });
    }

    @Override
    public void bindEnd(Rectangle controlPoint) {
        controlPoint.xProperty().addListener(event -> {
            double centerX = controlPoint.getX() + 2;
            line.setEndX(centerX);
        });
        controlPoint.yProperty().addListener(event -> {
            double centerY = controlPoint.getY() + 2;
            line.setEndY(centerY);
        });
    }

    @Override
    public String getType() {
        return "s";
    }
}
