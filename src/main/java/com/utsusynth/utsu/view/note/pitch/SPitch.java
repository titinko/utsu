package com.utsusynth.utsu.view.note.pitch;

import javafx.beans.property.DoubleProperty;
import javafx.scene.shape.CubicCurve;
import javafx.scene.shape.Shape;

/** Represents a pitchbend that is an s-shaped curve. */
public class SPitch implements Pitch {
	private final CubicCurve curve;

	public SPitch(double startX, double startY, double endX, double endY) {
		double halfX = (startX + endX) / 2;
		curve = new CubicCurve(startX, startY, halfX, startY, halfX, endY, endX, endY);
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
	public void bindStart(DoubleProperty xControl, DoubleProperty yControl) {
		xControl.addListener(event -> {
			double centerX = xControl.get() + 2;
			curve.setStartX(centerX);
			double halfX = (centerX + curve.getEndX()) / 2;
			curve.setControlX1(halfX);
			curve.setControlX2(halfX);
		});
		yControl.addListener(event -> {
			double centerY = yControl.get() + 2;
			curve.setStartY(centerY);
			curve.setControlY1(centerY);
		});
	}

	@Override
	public void bindEnd(DoubleProperty xControl, DoubleProperty yControl) {
		xControl.addListener(event -> {
			double centerX = xControl.get() + 2;
			curve.setEndX(centerX);
			double halfX = (curve.getStartX() + centerX) / 2;
			curve.setControlX1(halfX);
			curve.setControlX2(halfX);
		});
		yControl.addListener(event -> {
			double centerY = yControl.get() + 2;
			curve.setEndY(centerY);
			curve.setControlY2(centerY);
		});
	}

	@Override
	public String getType() {
		return "";
	}
}
