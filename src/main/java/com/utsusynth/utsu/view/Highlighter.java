package com.utsusynth.utsu.view;

import java.util.LinkedList;

/**
 * Keeps track of notes that are currently highlighted.
 */
public class Highlighter {
	private final LinkedList<TrackNote> highlighted;
	
	Highlighter() {
		highlighted = new LinkedList<>();
	}
	
	void addHighlight(TrackNote note) {
		highlighted.add(note);
		note.setHighlighted(true);
	}
	
	void clearHighlights() {
		for (TrackNote note : highlighted) {
			note.setHighlighted(false);
		}
		highlighted.clear();
	}
	
	boolean isHighlighted(TrackNote note) {
		return highlighted.contains(note);
	}
}
