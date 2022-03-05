package com.utsusynth.utsu.common.utils;

import com.utsusynth.utsu.common.data.VoicebankData;
import com.utsusynth.utsu.model.voicebank.PresampConfig;
import com.utsusynth.utsu.model.voicebank.PresampConfig.SuffixType;

import java.util.Optional;

/** Common operations to perform on a lyric. */
public class LyricUtils {
    /** Extract common prefixes. */
    public static String guessJpPrefix(String lyric, VoicebankData voicebankData) {
        for (String prefix : voicebankData.getPresampConfig().getPrefixes()) {
            if (lyric.startsWith(prefix)) {
                return prefix;
            }
        }
        return "";
    }

    /** Extract common suffixes. */
    public static String guessJpSuffix(String lyric, VoicebankData voicebankData) {
        PresampConfig.Reader presampConfig = voicebankData.getPresampConfig();
        String curLyric = lyric;
        StringBuilder suffix = new StringBuilder();
        for (SuffixType suffixType : presampConfig.getSuffixOrder().reverse()) {
            String newSuffix = guessJpSuffix(curLyric, suffixType, voicebankData);
            suffix.append(newSuffix);
            curLyric = curLyric.substring(0, curLyric.length() - newSuffix.length());
            if (!presampConfig.excludesRepeatSuffix(suffixType)) {
                // Get repeat suffixes if necessary.
                while (!newSuffix.isEmpty()) {
                    newSuffix = guessJpSuffix(curLyric, suffixType, voicebankData);
                    suffix.append(newSuffix);
                    curLyric = curLyric.substring(0, curLyric.length() - newSuffix.length());
                }
            }
        }
        return suffix.toString();
    }

    private static String guessJpSuffix(
            String lyric, SuffixType suffixType, VoicebankData voicebankData) {
        for (String suffix : voicebankData.getPresampConfig().getSuffixes(suffixType)) {
            if (voicebankData.getPresampConfig().allowsUnderbarSuffix(suffixType)
                    && lyric.endsWith("_" + suffix)) {
                return "_" + suffix;
            }
            if (lyric.endsWith(suffix)) {
                return suffix;
            }
        }
        return "";
    }

    /** Guess vowel for a japanese lyric. */
    public static Optional<String> guessJpVowel(String lyric, VoicebankData voicebankData) {
        PresampConfig.Reader presampConfig = voicebankData.getPresampConfig();
        for (String converted : voicebankData.getLyricConversions().getGroup(lyric)) {
            for (int i = converted.length() - 1; i >= 0; i--) {
                String potentialVowel = converted.substring(i);
                if (presampConfig.hasVowelMapping(potentialVowel)) {
                    return Optional.of(presampConfig.getVowelMapping(potentialVowel));
                }
            }
        }
        // No vowel found.
        return Optional.empty();
    }

    // Guess consonant.
    public static Optional<String> guessJpConsonant(String lyric, VoicebankData voicebankData) {
        PresampConfig.Reader presampConfig = voicebankData.getPresampConfig();
        for (String converted : voicebankData.getLyricConversions().getGroup(lyric)) {
            if (presampConfig.hasConsonantMapping(converted)) {
                return Optional.of(presampConfig.getConsonantMapping(converted));
            } else if (presampConfig.hasVowelMapping(converted)) {
                return Optional.of(presampConfig.getVowelMapping(converted));
            }
        }
        // No consonant found.
        return Optional.empty();
    }
}
