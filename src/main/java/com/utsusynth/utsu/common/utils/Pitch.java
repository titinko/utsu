package com.utsusynth.utsu.common.utils;

/**
 * A utility class that represents a pitch and can be attached to a lyric.
 */
public class Pitch implements Comparable<Pitch> {
    private final String key;
    private final int octave;

    /** Create a pitch. Default to "C4" if invalid inputs are given. */
    public Pitch(String key, int octave) {
        this.key = standardizeKey(key);
        this.octave = standardizeOctave(octave);
    }

    private static String standardizeKey(String key) {
        String upperCaseKey = key.toUpperCase();
        if (PitchUtils.PITCHES.contains(upperCaseKey)) {
            return upperCaseKey;
        }
        System.out.println("Warning: Pitch created with incorrect key:" + key);
        return "C";
    }

    private static int standardizeOctave(int octave) {
        if (octave >= 1 && octave <= 7) {
            return octave;
        }
        System.out.println("Warning: Pitch created with incorrect octave:" + octave);
        return 4;
    }

    @Override
    public String toString() {
        return "" + key + octave;
    }

    @Override
    public int compareTo(Pitch other) {
        if (this.octave < other.octave) {
            return -1;
        }
        if (this.octave > other.octave) {
            return 1;
        }
        Integer thisKeyPosition = PitchUtils.PITCHES.indexOf(this.key);
        Integer otherKeyPosition = PitchUtils.PITCHES.indexOf(other.key);
        return thisKeyPosition.compareTo(otherKeyPosition);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Pitch)) {
            return false;
        }
        Pitch otherPitch = (Pitch) other;
        return this.key.equals(otherPitch.key)
                && this.octave == otherPitch.octave;
    }
}
