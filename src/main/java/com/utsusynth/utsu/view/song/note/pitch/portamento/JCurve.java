package com.utsusynth.utsu.view.song.note.pitch.portamento;

import javafx.scene.paint.Color;
import javafx.scene.shape.CubicCurve;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;

/** Represents a portamento that is a j-shaped curve. */
public class JCurve implements Curve {
    private final CubicCurve curve;

    JCurve(double startX, double startY, double endX, double endY) {
        double halfX = (startX + endX) / 2;
        double halfY = (startY + endY) / 2;
        this.curve = new CubicCurve(startX, startY, halfX, startY, endX, halfY, endX, endY);
        this.curve.setStroke(Color.DARKSLATEBLUE);
        this.curve.setFill(Color.TRANSPARENT);
    }

    @Override
    public Shape getElement() {
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
    public void bindStart(Rectangle controlPoint) {
        controlPoint.xProperty().addListener(event -> {
            double centerX = controlPoint.getX() + 2;
            curve.setStartX(centerX);
            curve.setControlX1((centerX + curve.getEndX()) / 2);
        });
        controlPoint.yProperty().addListener(event -> {
            double centerY = controlPoint.getY() + 2;
            curve.setStartY(centerY);
            curve.setControlY1(centerY);
            curve.setControlY2((centerY + curve.getEndY()) / 2);
        });
    }

    @Override
    public void bindEnd(Rectangle controlPoint) {
        controlPoint.xProperty().addListener(event -> {
            double centerX = controlPoint.getX() + 2;
            curve.setEndX(centerX);
            curve.setControlX1((curve.getStartX() + centerX) / 2);
            curve.setControlX2(centerX);
        });
        controlPoint.yProperty().addListener(event -> {
            double centerY = controlPoint.getY() + 2;
            curve.setEndY(centerY);
            curve.setControlY2((curve.getStartY() + centerY) / 2);
        });
    }

    @Override
    public String getType() {
        return "j";
    }
}
