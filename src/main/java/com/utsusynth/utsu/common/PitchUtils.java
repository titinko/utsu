package com.utsusynth.utsu.common;

import com.google.common.collect.ImmutableList;

public class PitchUtils {
	public static final ImmutableList<String> PITCHES = ImmutableList.of(
			"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B");
	public static final ImmutableList<String> REVERSE_PITCHES = PITCHES.reverse();
	
	private PitchUtils() {}
	
	public static String noteNumToPitch(int noteNum) {
		return PITCHES.get(noteNum % 12) + Integer.toString(noteNum / 12 - 1);
	}
	
	public static int pitchToNoteNum(String fullPitch) {
		int octave = Integer.parseInt(fullPitch.substring(fullPitch.length() - 1));
		int pitchNum = PITCHES.indexOf(fullPitch.substring(0, fullPitch.length() - 1));
		return (octave + 1) * 12 + pitchNum;
	}
	
	public static String rowNumToPitch(int rowNum) {
		return REVERSE_PITCHES.get(rowNum % 12) + Integer.toString(7 - (rowNum / 12));
	}
	
	public static int pitchToRowNum(String fullPitch) {
		int octave = Integer.parseInt(fullPitch.substring(fullPitch.length() - 1));
		int pitchNum = REVERSE_PITCHES.indexOf(fullPitch.substring(0, fullPitch.length() - 1));
		return 12 * (7 - octave) + pitchNum;
	}
}
