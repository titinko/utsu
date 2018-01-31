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
    // TODO: Once you have a VoicebankManager, consider sharing between voicebanks.
    private final DisjointLyricSet conversionSet;
    private final LyricConfigMap lyricConfigs;
    private final Map<String, String> pitchMap;

    private File pathToVoicebank; // Example: "/Library/Iona.utau/"
    private String name; // Example: "Iona"
    private String author; // Example: "Lethe"
    private String description; // Contents of readme.txt
    private String imageName; // Example: "img.bmp"

    public class Builder {
        private final Voicebank newVoicebank;

        private Builder(Voicebank newVoicebank) {
            this.newVoicebank = newVoicebank;
        }

        public Builder setPathToVoicebank(File pathToVoicebank) {
            newVoicebank.pathToVoicebank = pathToVoicebank;
            return this;
        }

        public Builder setName(String name) {
            newVoicebank.name = name;
            return this;
        }

        public Builder setAuthor(String author) {
            newVoicebank.author = author;
            return this;
        }

        public Builder setDescription(String description) {
            newVoicebank.description = description;
            return this;
        }

        public Builder setImageName(String imageName) {
            newVoicebank.imageName = imageName;
            return this;
        }

        public Builder addLyric(LyricConfig config) {
            lyricConfigs.addConfig(config);
            return this;
        }

        public Builder addPitchSuffix(String pitch, String suffix) {
            pitchMap.put(pitch, suffix);
            return this;
        }

        public Builder addConversionGroup(String... members) {
            conversionSet.addGroup(members);
            return this;
        }

        public Voicebank build() {
            return newVoicebank;
        }
    }

    public Voicebank(
            LyricConfigMap lyricConfigs,
            Map<String, String> pitchMap,
            DisjointLyricSet conversionSet) {
        this.lyricConfigs = lyricConfigs;
        this.pitchMap = pitchMap;
        this.conversionSet = conversionSet;
    }

    public Builder toBuilder() {
        // Returns the builder of a new Voicebank with this one's attributes.
        // The old Voicebank's lyricConfigs, pitchMap, and conversionSet objects are used.
        return new Builder(new Voicebank(this.lyricConfigs, this.pitchMap, this.conversionSet))
                .setPathToVoicebank(this.pathToVoicebank).setName(this.name).setAuthor(this.author)
                .setDescription(this.description).setImageName(this.imageName);
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
            if (lyricConfigs.hasLyric(combo)) {
                return Optional.of(lyricConfigs.getConfig(combo));
            }
        }

        SortedSet<LyricConfig> matches = new TreeSet<>();
        for (String convertedLyric : conversionSet.getGroup(lyric)) {
            if (convertedLyric.equals(lyric)) {
                // Don't check the same lyric twice.
                continue;
            }

            for (String combo : allCombinations(prefix, convertedLyric, suffix)) {
                if (lyricConfigs.hasLyric(combo)) {
                    matches.add(lyricConfigs.getConfig(combo));
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

    public String getAuthor() {
        return author;
    }

    public String getImageName() {
        return imageName;
    }

    public String getImagePath() {
        return new File(pathToVoicebank, imageName).getAbsolutePath();
    }

    public String getDescription() {
        return description;
    }

    public File getPathToVoicebank() {
        return pathToVoicebank;
    }

    @Override
    public String toString() {
        // Crappy string representation of a Voicebank object.
        String result = "";
        return result + " " + pathToVoicebank + " " + name + " " + imageName;
    }
}
