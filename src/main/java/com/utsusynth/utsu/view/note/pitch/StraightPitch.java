package com.utsusynth.utsu.view.note.pitch;

import javafx.beans.property.DoubleProperty;
import javafx.scene.shape.Line;
import javafx.scene.shape.Shape;

/** Represents a pitchbend that is a straight line. */
public class StraightPitch implements Pitch {
	private final Line line;

	public StraightPitch(double startX, double startY, double endX, double endY) {
		line = new Line(startX, startY, endX, endY);
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
	public void bindStart(DoubleProperty xControl, DoubleProperty yControl) {
		xControl.addListener(event -> {
			double centerX = xControl.get() + 2;
			line.setStartX(centerX);
		});
		yControl.addListener(event -> {
			double centerY = yControl.get() + 2;
			line.setStartY(centerY);
		});
	}

	@Override
	public void bindEnd(DoubleProperty xControl, DoubleProperty yControl) {
		xControl.addListener(event -> {
			double centerX = xControl.get() + 2;
			line.setEndX(centerX);
		});
		yControl.addListener(event -> {
			double centerY = yControl.get() + 2;
			line.setEndY(centerY);
		});
	}

	@Override
	public String getType() {
		return "s";
	}
}
