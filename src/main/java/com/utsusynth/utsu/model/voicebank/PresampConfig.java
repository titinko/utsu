package com.utsusynth.utsu.model.voicebank;

import com.google.common.collect.ImmutableList;

import java.util.*;

/**
 * Configuration parsed from a voicebank's presamp.ini file, if present.
 */
public class PresampConfig {
    // If a lyric ends with the key, the value is the vowel used in the succeeding VC.
    private final Map<String, String> vowelMappings;
    // Key is a vowel, value is the volume of any note with that vowel.
    private final Map<String, Integer> vowelVolumes;
    // If a lyric is exactly the key, the value is the consonant used in the preceding VC.
    private final Map<String, String> consonantMappings;
    // Key is a consonant, value is whether that consonant should overlap preceding VC or skip it.
    // True -> Overlap with preceding VC. (default)
    // False -> Don't overlap preceding vc.
    private final Map<String, Boolean> consonantOverlaps;
    // Set of all lyric replacements specified in the presamp file. Can be merged with the larger
    // voicebank's official lyric conversion set.
    private final Set<String[]> lyricReplacements;

    // If this is set to false, if the vowel of a CV is <20ms, a VC will not be inserted.
    // If set to true, a VC will be inserted no matter what.
    private boolean mustVC = false;

    private String[] vcvFormat = new String[] {"%v%%VCVPAD%%CV%"}; // "a ka"
    private String[] beginningCvFormat = new String[] {"-%VCVPAD%%CV%"}; // "- ka"
    private String[] crossCvFormat = new String[] {"*%VCVPAD%%CV%"}; // "* ka"
    private String[] vcFormat = new String[] {"%v%%vcpad%%c%", "%c%%vcpad%%c%"}; // "a k", "k k"
    private String[] cvFormat = new String[] {"%CV%", "%c%%V%"}; // "ka", "ka"
    private String[] cFormat = new String[] {"%c%"}; // "k"
    private String[] longVFormat = new String[] {"%V%-"}; // "a-"
    private String[] vcpadFormat = new String[] {" "}; // Padding used in VC lyrics.
    private String[] vcvpadFormat = new String[] {" "}; // Padding used in VCV lyrics.
    private String[] ending1Format = new String[] {"%v%%VCPAD%R"}; // "a R"
    private String[] ending2Format = new String[] {"-"};

    public static class Builder {
        private final PresampConfig newConfig;

        private Builder(PresampConfig newConfig) {
            this.newConfig = newConfig;
        }

        public void addVowelMapping(String key, String vowel) {
            newConfig.vowelMappings.put(key, vowel);
        }

        public void addVowelVolume(String vowel, Integer volume) {
            newConfig.vowelVolumes.put(vowel, volume);
        }

        public void addConsonantMapping(String key, String consonant) {
            newConfig.consonantMappings.put(key, consonant);
        }

        public void addConsonantOverlap(String consonant, Boolean overlap) {
            newConfig.consonantOverlaps.put(consonant, overlap);
        }

        public void addLyricReplacement(String[] replacementSet) {
            newConfig.lyricReplacements.add(replacementSet);
        }

        public Builder setVcvFormat(String[] vcvFormat) {
            newConfig.vcvFormat = vcvFormat;
            return this;
        }

        public Builder setBeginningCvFormat(String[] beginningCvFormat) {
            newConfig.beginningCvFormat = beginningCvFormat;
            return this;
        }

        public Builder setCrossCvFormat(String[] crossCvFormat) {
            newConfig.crossCvFormat = crossCvFormat;
            return this;
        }

        public Builder setVcFormat(String[] vcFormat) {
            newConfig.vcFormat = vcFormat;
            return this;
        }

        public Builder setCvFormat(String[] cvFormat) {
            newConfig.cvFormat = cvFormat;
            return this;
        }

        public Builder setCFormat(String[] cFormat) {
            newConfig.cFormat = cFormat;
            return this;
        }

        public Builder setLongVFormat(String[] longVFormat) {
            newConfig.longVFormat = longVFormat;
            return this;
        }

        public Builder setVcpadFormat(String[] vcpadFormat) {
            newConfig.vcpadFormat = vcpadFormat;
            return this;
        }

        public Builder setVcvpadFormat(String[] vcvpadFormat) {
            newConfig.vcvpadFormat = vcvpadFormat;
            return this;
        }

        public Builder setEnding1Format(String[] ending1Format) {
            newConfig.ending1Format = ending1Format;
            return this;
        }

        public Builder setEnding2Format(String[] ending2Format) {
            newConfig.ending2Format = ending2Format;
            return this;
        }

        public Builder setMustVC(boolean mustVC) {
            newConfig.mustVC = mustVC;
            return this;
        }

        public PresampConfig build() {
            return newConfig;
        }
    }

    public PresampConfig(
            Map<String, String> vowelMappings,
            Map<String, Integer> vowelVolumes,
            Map<String, String> consonantMappings,
            Map<String, Boolean> consonantOverlaps,
            Set<String[]> lyricReplacements) {
        this.vowelMappings = vowelMappings;
        this.vowelVolumes = vowelVolumes;
        this.consonantMappings = consonantMappings;
        this.consonantOverlaps = consonantOverlaps;
        this.lyricReplacements = lyricReplacements;
    }

    public Builder toBuilder() {
        // Returns the builder of a new PresampConfig with this one's attributes.
        // The old config's final fields are used--the objects are not regenerated.
        return new Builder(new PresampConfig(
                vowelMappings,
                vowelVolumes,
                consonantMappings,
                consonantOverlaps,
                lyricReplacements))
                .setVcvFormat(vcvFormat)
                .setBeginningCvFormat(beginningCvFormat)
                .setCrossCvFormat(crossCvFormat)
                .setVcFormat(vcFormat)
                .setCvFormat(cvFormat)
                .setCFormat(cFormat)
                .setLongVFormat(longVFormat)
                .setVcpadFormat(vcpadFormat)
                .setVcvpadFormat(vcvpadFormat)
                .setEnding1Format(ending1Format)
                .setEnding2Format(ending2Format)
                .setMustVC(mustVC);
    }
}
