package com.utsusynth.utsu.view.note.pitch;

import javafx.beans.property.DoubleProperty;
import javafx.scene.shape.Shape;

/** Pitch-specific wrapper for a line/curve. */
public interface Pitch {
	Shape getElement();

	double getStartX();

	double getStartY();

	double getEndX();

	double getEndY();

	void bindStart(DoubleProperty xControl, DoubleProperty yControl);

	void bindEnd(DoubleProperty xControl, DoubleProperty yControl);

	// TODO: Make this an enum.
	String getType();
}
