package com.utsusynth.utsu.view.note;

import javafx.scene.Node;

public interface TrackNoteCallback {

	void setLyricElement(Node lyricElement);

	void setHighlighted(boolean highlighted);

	void setSongLyric(String newLyric);

	void adjustColumnSpan();
}
