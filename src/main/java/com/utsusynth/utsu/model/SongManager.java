package com.utsusynth.utsu.model;

public class SongManager {
	private Song song;
	
	public SongManager() {
		song = Song.createEmptySong();
	}

	public void setSong(Song song) {
		this.song = song;
	}
	
	public Song getSong() {
		return song;
	}
}
