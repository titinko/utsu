package com.utsusynth.utsu.model.voicebank;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import com.google.common.base.CharMatcher;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

/**
 * In-code representation of a voice bank. Compatible with oto.ini files. TODO: Support oto_ini.txt
 * as well
 */
public class Voicebank {
    // TODO: Once you have a VoicebankManager, make this common to all voicebanks.
    private final DisjointLyricSet conversionSet;

    private final File pathToVoicebank; // Example: "/Library/Iona.utau/"
    private final String name; // Example: "Iona"
    private final String imageName; // Example: "img.bmp"
    private final Map<String, LyricConfig> lyricConfigs;
    private final Map<String, String> pitchMap;

    public Voicebank(
            File pathToVoicebank,
            String name,
            String imageName,
            Map<String, LyricConfig> lyricConfigs,
            Map<String, String> pitchMap,
            DisjointLyricSet conversionSet) {
        this.pathToVoicebank = pathToVoicebank;
        this.name = name;
        this.imageName = imageName;
        this.lyricConfigs = lyricConfigs;
        this.pitchMap = pitchMap;
        this.conversionSet = conversionSet;
    }

    /**
     * Should be called when lyric is expected to have an exact match in voicebank.
     */
    public Optional<LyricConfig> getLyricConfig(String trueLyric) {
        return getLyricConfig("", trueLyric, "");
    }

    public Optional<LyricConfig> getLyricConfig(String prevLyric, String lyric, String pitch) {
        String prefix = getVowel(prevLyric) + " "; // Most common VCV format.
        String suffix = pitchMap.containsKey(pitch) ? pitchMap.get(pitch) : ""; // Pitch suffix.

        // Check all possible prefix/lyric/suffix combinations.
        for (String combo : allCombinations(prefix, lyric, suffix)) {
            if (lyricConfigs.containsKey(combo)) {
                return Optional.of(lyricConfigs.get(combo));
            }
        }

        SortedSet<LyricConfig> matches = new TreeSet<>();
        for (String convertedLyric : conversionSet.getGroup(lyric)) {
            if (convertedLyric.equals(lyric)) {
                // Don't check the same lyric twice.
                continue;
            }

            for (String combo : allCombinations(prefix, convertedLyric, suffix)) {
                if (lyricConfigs.containsKey(combo)) {
                    matches.add(lyricConfigs.get(combo));
                }
            }
        }
        // For now, arbitrarily but consistently return the first match.
        if (!matches.isEmpty()) {
            return Optional.of(matches.first());
        }

        return Optional.absent();
    }

    // Finds the vowel sound of a lyric by converting to ASCII and taking the last character.
    private char getVowel(String prevLyric) {
        for (String convertedLyric : conversionSet.getGroup(prevLyric)) {
            if (CharMatcher.ascii().matchesAllOf(convertedLyric)) {
                return convertedLyric.toLowerCase().charAt(convertedLyric.length() - 1);
            }
        }
        // Return this if no vowel found.
        return '-';
    }

    private List<String> allCombinations(String prefix, String lyric, String suffix) {
        // Exact lyric match is prioritized first.
        return ImmutableList.of(lyric, lyric + suffix, prefix + lyric + suffix, prefix + lyric);
    }

    public String getName() {
        return name;
    }

    public String getImagePath() {
        return new File(pathToVoicebank, imageName).getAbsolutePath();
    }

    public File getPathToVoicebank() {
        return pathToVoicebank;
    }

    @Override
    public String toString() {
        // Crappy string representation of a Voicebank object.
        String result = "";
        for (String lyric : lyricConfigs.keySet()) {
            result += lyric + " = " + lyricConfigs.get(lyric) + "\n";
        }
        return result + " " + pathToVoicebank + " " + name + " " + imageName;
    }
}
