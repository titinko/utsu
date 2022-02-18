package com.utsusynth.utsu.common.utils;

import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableList;
import com.utsusynth.utsu.common.data.VoicebankData;
import com.utsusynth.utsu.model.voicebank.DisjointLyricSet;
import com.utsusynth.utsu.model.voicebank.PresampConfig;

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
    public static String guessJpVowel(String lyric, VoicebankData voicebankData) {
        PresampConfig.Reader presampConfig = voicebankData.getPresampConfig();
        for (String converted : voicebankData.getLyricConversions().getGroup(lyric)) {
            for (int i = converted.length() - 1; i >= 0; i--) {
                String potentialVowel = converted.substring(i);
                if (presampConfig.hasVowelMapping(potentialVowel)) {
                    return presampConfig.getVowelMapping(potentialVowel);
                }
            }
            // Convert to ASCII and check last character.
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
    public static String guessJpConsonant(String lyric, VoicebankData voicebankData) {
        PresampConfig.Reader presampConfig = voicebankData.getPresampConfig();
        for (String converted : voicebankData.getLyricConversions().getGroup(lyric)) {
            if (presampConfig.hasConsonantMapping(converted)) {
                return presampConfig.getConsonantMapping(converted);
            } else if (presampConfig.hasVowelMapping(converted)) {
                return presampConfig.getVowelMapping(converted);
            }
        }
        // No consonant found.
        return "";
    }
}
