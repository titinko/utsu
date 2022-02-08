package com.utsusynth.utsu.common.utils;

import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableList;
import com.utsusynth.utsu.model.voicebank.DisjointLyricSet;

import java.util.Optional;

/** Common operations to perform on a lyric. */
public class LyricUtils {
    public static final ImmutableList<Character> JP_VOWELS =
            ImmutableList.of('a', 'i', 'u', 'e', 'o', 'n');
    public static final ImmutableList<String> JP_CONSONANTS =
            ImmutableList.of("k", "s", "t", "n", "h", "f", "m", "r", "y", "w", "ch", "gy", "py",
                    "ry", "ng", "ny", "r", "hy", "by", "b", "d", "g", "j", "p", "sh", "ky", "z",
                    "my");
    /** Extract common prefixes. */
    public static String guessPrefix(String lyric) {
        String curLyric = lyric;
        StringBuilder curPrefix = new StringBuilder();
        // JP VCV prefix.
        for (Character vowel : JP_VOWELS) {
            String vcvPrefix = vowel + " ";
            if (curLyric.startsWith(vcvPrefix)) {
                curPrefix.append(vcvPrefix);
                break;
            }
        }
        curLyric = curLyric.substring(curPrefix.length());
        // Pitch.
        for (String pitch : PitchUtils.PITCHES) {
            for (int octave = 1; octave <= PitchUtils.NUM_OCTAVES; octave++) {
                String pitchPrefix = pitch + octave;
                if (curLyric.startsWith(pitchPrefix)) {
                    curPrefix.append(pitchPrefix);
                    break;
                }
            }
        }
        return curPrefix.toString();
    }

    /** Extract common suffixes. */
    public static String guessSuffix(String lyric) {
        // Pitch.
        for (String pitch : PitchUtils.PITCHES) {
            for (int octave = 1; octave <= PitchUtils.NUM_OCTAVES; octave++) {
                String pitchSuffix = pitch + octave;
                if (lyric.endsWith(pitchSuffix)) {
                    return pitchSuffix;
                }
            }
        }
        return "";
    }

    /** Guess vowel for a japanese lyric. */
    public static String guessJpVowel(String lyric, DisjointLyricSet.Reader conversionSet) {
        // Convert to ASCII and check last character.
        for (String converted : conversionSet.getGroup(lyric)) {
            if (CharMatcher.ascii().matchesAllOf(converted) && !converted.isEmpty()) {
                char lastChar = converted.toLowerCase().charAt(converted.length() - 1);
                if (JP_VOWELS.contains(lastChar)) {
                    return String.valueOf(lastChar);
                }
            }
        }
        // No vowel found.
        return "";
    }

    // Guess consonant.
    public static String guessJpConsonant(String lyric, DisjointLyricSet.Reader conversionSet) {
        // Convert to ASCII and check last character.
        for (String converted : conversionSet.getGroup(lyric)) {
            if (CharMatcher.ascii().matchesAllOf(converted) && !converted.isEmpty()) {
                if (converted.length() > 1) {
                    String firstTwoChars = converted.substring(0, 2);
                    if (JP_CONSONANTS.contains(firstTwoChars)) {
                        return firstTwoChars;
                    }
                }
                String firstChar = converted.substring(0, 1);
                if (JP_CONSONANTS.contains(firstChar)) {
                    return firstChar;
                }
            }
        }
        // No consonant found.
        return "";
    }
}
