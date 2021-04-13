package com.utsusynth.utsu.view.song.note.pitch.portamento;

import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;

/** Portamento-specific wrapper for a line/curve. */
public interface Curve {
	Shape redraw(double offsetX);

	double getStartX();

	double getStartY();

	double getEndX();

	double getEndY();

	void bindStart(Rectangle controlPoint, double offsetX);

	void bindEnd(Rectangle controlPoint, double offsetX);

	// TODO: Make this an enum.
	String getType();
}
