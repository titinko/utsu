package com.utsusynth.utsu.common;

import com.google.common.base.Optional;

public class QuantizedAddRequest {
	private final QuantizedNote note;
	private final String pitch; // Example: C#4
	private final String lyric;
	private final Optional<String> trueLyric;

	public QuantizedAddRequest(
			QuantizedNote note, String pitch, String lyric, Optional<String> trueLyric) {
		this.note = note;
		this.pitch = pitch;
		this.lyric = lyric;
		this.trueLyric = trueLyric;
	}
	
	public QuantizedNote getNote() {
		return this.note;
	}
	
	public String getPitch() {
		return this.pitch;
	}
	
	public String getLyric() {
		return this.lyric;
	}
	
	public Optional<String> getTrueLyric() {
		return this.trueLyric;
	}
}
