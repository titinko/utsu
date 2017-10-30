package com.utsusynth.utsu.common;

import com.google.common.collect.ImmutableList;

public class PitchUtils {
	public static final ImmutableList<String> PITCHES = ImmutableList.of("C", "C#", "D", "D#", "E",
			"F", "F#", "G", "G#", "A", "A#", "B");
	public static final ImmutableList<String> REVERSE_PITCHES = PITCHES.reverse();

	private PitchUtils() {
	}

	/**
	 * Convert a note num (where 24 = C1) to the string description of what pitch it represents.
	 */
	public static String noteNumToPitch(int noteNum) {
		return PITCHES.get(noteNum % 12) + Integer.toString(noteNum / 12 - 1);
	}

	/**
	 * Convert a pitch string into a note number (where 24 = C1).
	 */
	public static int pitchToNoteNum(String fullPitch) {
		int octave = Integer.parseInt(fullPitch.substring(fullPitch.length() - 1));
		int pitchNum = PITCHES.indexOf(fullPitch.substring(0, fullPitch.length() - 1));
		return (octave + 1) * 12 + pitchNum;
	}

	/**
	 * Convert the row number in the UI (where the top is 0) into a pitch string.
	 */
	public static String rowNumToPitch(int rowNum) {
		return REVERSE_PITCHES.get(rowNum % 12) + Integer.toString(7 - (rowNum / 12));
	}

	/**
	 * Convert a pitch string into the row number in the UI (where the top is 0).
	 */
	public static int pitchToRowNum(String fullPitch) {
		int octave = Integer.parseInt(fullPitch.substring(fullPitch.length() - 1));
		int pitchNum = REVERSE_PITCHES.indexOf(fullPitch.substring(0, fullPitch.length() - 1));
		return 12 * (7 - octave) + pitchNum;
	}
}
