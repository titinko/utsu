package com.utsusynth.utsu.view.note.pitch;

import javafx.beans.property.DoubleProperty;
import javafx.scene.shape.CubicCurve;
import javafx.scene.shape.Shape;

public class RPitch implements Pitch {
	private final CubicCurve curve;

	public RPitch(double startX, double startY, double endX, double endY) {
		double halfX = (startX + endX) / 2;
		double halfY = (startY + endY) / 2;
		this.curve = new CubicCurve(startX, startY, startX, halfY, halfX, endY, endX, endY);
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
			curve.setControlX1(centerX);
			curve.setControlX2((centerX + curve.getEndX()) / 2);
		});
		yControl.addListener(event -> {
			double centerY = yControl.get() + 2;
			curve.setStartY(centerY);
			curve.setControlY1((centerY + curve.getEndY()) / 2);
		});
	}

	@Override
	public void bindEnd(DoubleProperty xControl, DoubleProperty yControl) {
		xControl.addListener(event -> {
			double centerX = xControl.get() + 2;
			curve.setEndX(centerX);
			curve.setControlX2((curve.getStartX() + centerX) / 2);
		});
		yControl.addListener(event -> {
			double centerY = yControl.get() + 2;
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
