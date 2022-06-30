package com.utsusynth.utsu.common.utils;

import com.google.common.collect.ImmutableList;

import java.util.Locale;

public class PitchUtils {
    public static final ImmutableList<String> PITCHES =
            ImmutableList.of("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B");
    public static final ImmutableList<String> REVERSE_PITCHES = PITCHES.reverse();

    public static final int NUM_OCTAVES = 7;
    public static final int TOTAL_NUM_PITCHES = NUM_OCTAVES * PITCHES.size();

    private PitchUtils() {}

    public static boolean looksLikePitch(String pitchString) {
        if (pitchString.length() < 2 || pitchString.length() > 3) {
            return false;
        }
        String key = pitchString.substring(0, pitchString.length() - 1).toUpperCase();
        if (!PITCHES.contains(key)) {
            return false;
        }
        try {
            int octave = Integer.parseInt(pitchString.substring(pitchString.length() - 1));
            return octave >= 1 && octave <= 7;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static Pitch pitchStringToPitch(String pitchString) {
        if (!looksLikePitch(pitchString)) {
            throw new IllegalArgumentException("Not a correct pitch string: " + pitchString);
        }
        String key = pitchString.substring(0, pitchString.length() - 1).toUpperCase();
        int octave = Integer.parseInt(pitchString.substring(pitchString.length() - 1));
        return new Pitch(key, octave);
    }

    /**
     * Convert a note num (where 24 = C1) to the string description of what pitch it represents.
     */
    public static String noteNumToPitch(int noteNum) {
        return PITCHES.get(noteNum % 12) + (noteNum / 12 - 1);
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

    public static String extractStartPitch(String lyric) {
        if (lyric.length() < 3) {
            return "";
        }
        char first = lyric.charAt(0);
        char second = lyric.charAt(1);
        char third = lyric.charAt(2);
        if (!(first >= 'A' && first <= 'G') && !(first >= 'a' && first <= 'g')) {
            return "";
        }
        if (second == '#') {
            if (third < '1' || third > '7') {
                return "";
            }
            return "" + first + second + third;
        }
        if (second < '1' || second > '7') {
            return "";
        }
        return "" + first + second;
    }

    public static String extractEndPitch(String lyric) {
        if (lyric.length() < 3) {
            return "";
        }
        char first = lyric.charAt(lyric.length() - 3);
        char second = lyric.charAt(lyric.length() - 2);
        char third = lyric.charAt(lyric.length() - 1);
        if (third < '1' || third > '7') {
            return "";
        }
        if (second == '#') {
            if (!(first >= 'A' && first <= 'G') && !(first >= 'a' && first <= 'g')) {
                return "";
            }
            return "" + first + second + third;
        }
        if (!(second >= 'A' && second <= 'G') && !(second >= 'a' && second <= 'g')) {
            return "";
        }
        return "" + second + third;
    }

    public static String removePitches(String lyric) {
        String startPitch = extractStartPitch(lyric);
        String endPitch = extractEndPitch(lyric);
        return lyric.substring(startPitch.length(), lyric.length() - endPitch.length());
    }
}
