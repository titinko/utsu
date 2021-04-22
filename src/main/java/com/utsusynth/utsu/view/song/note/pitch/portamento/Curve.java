package com.utsusynth.utsu.view.song.note.pitch.portamento;

import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;

/** Portamento-specific wrapper for a line/curve. */
public interface Curve {
	Shape redraw(double offsetX);

	double getStartX();

	double getStartY();

	double getEndX();

	double getEndY();

	void bindStart(ReadOnlyDoubleProperty xProperty, ReadOnlyDoubleProperty yProperty);

	void bindEnd(ReadOnlyDoubleProperty xProperty, ReadOnlyDoubleProperty yProperty);

	// TODO: Make this an enum.
	String getType();
}
