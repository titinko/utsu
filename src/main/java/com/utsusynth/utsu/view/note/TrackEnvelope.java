package com.utsusynth.utsu.view.note;

import javafx.scene.shape.Path;

public class TrackEnvelope {

	private final Path path;

	TrackEnvelope(Path path) {
		this.path = path;
	}

	public Path getElement() {
		return path;
	}
}
