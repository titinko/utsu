package com.utsusynth.utsu.view.note;

/** Callback from TrackLyric to TrackNote. */
public interface TrackLyricCallback {
	void setHighlighted(boolean highlighted);

	void setSongLyric(String newLyric);

	void adjustColumnSpan();
}
