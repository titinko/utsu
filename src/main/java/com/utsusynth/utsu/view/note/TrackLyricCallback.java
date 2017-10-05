package com.utsusynth.utsu.view.note;

import javafx.scene.Node;

/** Callback from TrackLyric to TrackNote. */
public interface TrackLyricCallback {

	// TODO: Make this call the track instead.
	void setLyricElement(Node lyricElement);

	void setHighlighted(boolean highlighted);

	void setSongLyric(String newLyric);

	void adjustColumnSpan();
}
