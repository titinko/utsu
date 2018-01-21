package com.utsusynth.utsu.model.song;

import com.google.inject.Inject;

public class SongManager {
	private Song song;

	@Inject
	public SongManager(Song song) {
		this.song = song;
	}

	public void setSong(Song song) {
		this.song = song;
	}

	public Song getSong() {
		return song;
	}
}
