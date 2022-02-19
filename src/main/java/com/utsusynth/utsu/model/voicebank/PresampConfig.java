package com.utsusynth.utsu.model.voicebank;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

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
    // Key is a consonant, value is whether that consonant should overlap upcoming CV or skip it.
    // True -> Overlap with following CV. (default)
    // False -> Don't overlap following CV.
    private final Map<String, Boolean> consonantOverlaps;
    // Key is a consonant, value is the VC length for VCs with that consonant.
    // If a consonant is not present, default to the voicebank-wide VC length.
    private final Map<String, VcLength> vcLengthOverrides;

    // Set of lyrics for which VCV should never be used even if present. CVVC is fine.
    private final Set<String> neverVcv;

    // Official lyric conversion sets for this voicebank.
    private final DisjointLyricSet lyricConversions;

    public enum AliasType {
        VCV, // "a ka"
        CVVC, // "ka a k". This option shouldn't have its own format, but should use CV + VC.
        BEGINNING_CV, // "- ka"
        CROSS_CV, // "* ka"
        VC, // "a k", "k k"
        CV, // "ka", "ka"
        C, // "k"
        LONG_V, // "a-"
        VCPAD, // Padding used in VC lyrics.
        VCVPAD, // Padding used in VCV lyrics.
        ENDING_1, // "a R"
        ENDING_2, // "a-"
    }
    private final Map<AliasType, ImmutableList<String>> aliasFormats;

    // List of prefixes to ignore when modifying lyrics.
    private final Set<String> prefixes;
    // List of each type of suffix.
    public enum SuffixType {
        NUM,
        APPEND,
        PITCH,
    }
    private final Map<SuffixType, Set<String>> suffixes;
    // Suffix types for which underbar is considered part of suffix.
    private final Set<SuffixType> allowUnderbarSuffixes;
    // Suffix types for which repeats are excluded, i.e. "ka22" -> "ka2" (2 not repeated).
    private final Set<SuffixType> excludeRepeatsSuffixes;
    // Suffix ordering. Default: number (if duplicates exist) -> append (W) -> pitch (C4)
    private String suffixOrder = "%num%%append%%pitch%";

    private ImmutableList<AliasType> aliasPriority = ImmutableList.of(
            AliasType.VCV, AliasType.CVVC, AliasType.CROSS_CV, AliasType.CV, AliasType.BEGINNING_CV
    );
    private ImmutableList<AliasType> aliasPriorityDifappend = ImmutableList.of(
            AliasType.CVVC, AliasType.VCV, AliasType.CROSS_CV, AliasType.CV, AliasType.BEGINNING_CV
    );
    private ImmutableList<AliasType> aliasPriorityDifpitch = ImmutableList.of(
            AliasType.CVVC, AliasType.VCV, AliasType.CROSS_CV, AliasType.CV, AliasType.BEGINNING_CV
    );

    // If set to true, when a note contains multiple consonants and values, presamp will
    // automatically split them into more notes.
    // If set to false, presamp will not try to split notes.
    private boolean enableSplitting = true;

    // If this is set to false, if the vowel of a CV is <20ms, a VC will not be inserted.
    // If set to true, a VC will be inserted no matter what.
    private boolean mustVC = false;

    // Flag string that presamp would automatically apply to consonant-only notes.
    // A consonant is recognized if specified as a key in consonantOverlaps.
    private String consonantFlags = "p0";

    public enum VcLength {
        PREUTTERANCE, // Length of preutterance in the following Cv.
        OTO, // Length of the whole VC OTO.
    }
    private VcLength vcLength = VcLength.PREUTTERANCE; // Can be overriden by vcLengthOverrides.

    public enum EndFlag {
        NO_AUTOMATIC_ENDINGS, // Flag value 0. Don't add automatic ending notes.
        USE_ENDING_1, // Use ending 1. Convert [ka][R] -> [ka]    [a R].
        USE_ENDING_2, // Use ending 2. Convert [ka][R] -> [ka][a-][R].
        USE_BOTH_ENDINGS, // Use both endings. [ka][R] -> [ka][a-][a R].
    }
    private EndFlag endFlag = EndFlag.USE_ENDING_1;

    public static class Builder {
        private final PresampConfig newConfig;

        private Builder(PresampConfig newConfig) {
            this.newConfig = newConfig;
        }

        public void clearVowelMappings() {
            newConfig.vowelMappings.clear();
        }

        public void addVowelMapping(String key, String vowel) {
            newConfig.vowelMappings.put(key, vowel);
        }

        public void clearVowelVolumes() {
            newConfig.vowelVolumes.clear();
        }

        public void addVowelVolume(String vowel, Integer volume) {
            newConfig.vowelVolumes.put(vowel, volume);
        }

        public void clearConsonantMappings() {
            newConfig.consonantMappings.clear();
        }

        public void addConsonantMapping(String key, String consonant) {
            newConfig.consonantMappings.put(key, consonant);
        }

        public void clearConsonantOverlaps() {
            newConfig.consonantOverlaps.clear();
        }

        public void addConsonantOverlap(String consonant, Boolean overlap) {
            newConfig.consonantOverlaps.put(consonant, overlap);
        }

        public void clearVcLengthOverrides() {
            newConfig.vcLengthOverrides.clear();
        }

        public void addVcLengthOverride(String consonant, VcLength override) {
            newConfig.vcLengthOverrides.put(consonant, override);
        }

        public void clearNeverVcv() {
            newConfig.neverVcv.clear();
        }

        public void addNeverVcv(String lyric) {
            newConfig.neverVcv.add(lyric);
        }

        public void clearLyricConversions() {
            newConfig.lyricConversions.clear();
        }

        public void addLyricConversionSet(ImmutableSet<String> replacementSet) {
            newConfig.lyricConversions.addGroup(replacementSet);
        }

        public void clearAliasFormats() {
            newConfig.aliasFormats.clear();
        }

        public void setAliasFormat(AliasType aliasType, ImmutableList<String> format) {
            newConfig.aliasFormats.put(aliasType, format);
        }

        public void clearPrefixes() {
            newConfig.prefixes.clear();
        }

        public void addPrefix(String prefix) {
            newConfig.prefixes.add(prefix);
        }

        public void clearSuffixes() {
            newConfig.suffixes.clear();
        }

        public void addSuffix(SuffixType suffixType, String suffix) {
            if (!newConfig.suffixes.containsKey(suffixType)) {
                newConfig.suffixes.put(suffixType, new HashSet<>());
            }
            newConfig.suffixes.get(suffixType).add(suffix);
        }

        public void clearAllowUnderbarSuffixes() {
            newConfig.allowUnderbarSuffixes.clear();
        }

        public void addAllowUnderbarSuffix(SuffixType suffixType) {
            newConfig.allowUnderbarSuffixes.add(suffixType);
        }

        public void clearExcludeRepeatsSuffixes() {
            newConfig.excludeRepeatsSuffixes.clear();
        }

        public void addExcludeRepeatsSuffix(SuffixType suffixType) {
            newConfig.excludeRepeatsSuffixes.add(suffixType);
        }

        public Builder setAliasPriorityDifpitch(ImmutableList<AliasType> aliasPriorityDifpitch) {
            newConfig.aliasPriorityDifpitch = aliasPriorityDifpitch;
            return this;
        }

        public Builder setAliasPriorityDifappend(ImmutableList<AliasType> aliasPriorityDifappend) {
            newConfig.aliasPriorityDifappend = aliasPriorityDifappend;
            return this;
        }

        public Builder setAliasPriority(ImmutableList<AliasType> aliasPriority) {
            newConfig.aliasPriority = aliasPriority;
            return this;
        }

        public Builder setEnableSplitting(boolean enableSplitting) {
            newConfig.enableSplitting = enableSplitting;
            return this;
        }

        public Builder setMustVC(boolean mustVC) {
            newConfig.mustVC = mustVC;
            return this;
        }

        public Builder setConsonantFlags(String consonantFlags) {
            newConfig.consonantFlags = consonantFlags;
            return this;
        }

        public Builder setVcLength(VcLength vcLength) {
            newConfig.vcLength = vcLength;
            return this;
        }

        public Builder setEndFlag(EndFlag endFlag) {
            newConfig.endFlag = endFlag;
            return this;
        }

        public Builder setSuffixOrder(String suffixOrder) {
            newConfig.suffixOrder = suffixOrder;
            return this;
        }

        public PresampConfig build() {
            return newConfig;
        }
    }

    public static class Reader {
        private final PresampConfig readonlyConfig;

        private Reader(PresampConfig readonlyConfig) {
            this.readonlyConfig = readonlyConfig;
        }

        public boolean hasVowelMapping(String key) {
            return readonlyConfig.vowelMappings.containsKey(key);
        }

        public String getVowelMapping(String key) {
            return readonlyConfig.vowelMappings.get(key);
        }

        public int getVowelVolume(String vowel) {
            return readonlyConfig.vowelVolumes.get(vowel);
        }

        public boolean hasConsonantMapping(String key) {
            return readonlyConfig.consonantMappings.containsKey(key);
        }

        public String getConsonantMapping(String key) {
            return readonlyConfig.consonantMappings.get(key);
        }

        public boolean getConsonantOverlap(String consonant) {
            return readonlyConfig.consonantOverlaps.get(consonant);
        }

        public VcLength getVcLengthOverride(String consonant) {
            return readonlyConfig.vcLengthOverrides.get(consonant);
        }

        public boolean isNeverVcv(String lyric) {
            return readonlyConfig.neverVcv.contains(lyric);
        }

        public DisjointLyricSet.Reader getLyricConversions() {
            return readonlyConfig.lyricConversions.getReader();
        }

        public ImmutableList<String> getAliasFormat(AliasType aliasType) {
            if (!readonlyConfig.aliasFormats.containsKey(aliasType)) {
                return ImmutableList.of();
            }
            return readonlyConfig.aliasFormats.get(aliasType);
        }

        public ImmutableSet<String> getPrefixes() {
            return ImmutableSet.copyOf(readonlyConfig.prefixes);
        }

        public ImmutableSet<SuffixType> getSuffixTypes() {
            return ImmutableSet.copyOf(readonlyConfig.suffixes.keySet());
        }

        public ImmutableSet<String> getSuffixes(SuffixType suffixType) {
            return ImmutableSet.copyOf(readonlyConfig.suffixes.get(suffixType));
        }

        public boolean allowsUnderbarSuffix(SuffixType suffixType) {
            return readonlyConfig.allowUnderbarSuffixes.contains(suffixType);
        }

        public boolean excludesRepeatSuffix(SuffixType suffixType) {
            return readonlyConfig.excludeRepeatsSuffixes.contains(suffixType);
        }


        public ImmutableList<AliasType> getAliasPriorityDifpitch() {
            return readonlyConfig.aliasPriorityDifpitch;
        }

        public ImmutableList<AliasType> getAliasPriorityDifappend() {
            return readonlyConfig.aliasPriorityDifappend;
        }

        public ImmutableList<AliasType> getAliasPriority() {
            return readonlyConfig.aliasPriority;
        }

        public boolean getEnableSplitting() {
            return readonlyConfig.enableSplitting;
        }

        public boolean getMustVc() {
            return readonlyConfig.mustVC;
        }

        public String getConsonantFlags() {
            return readonlyConfig.consonantFlags;
        }

        public VcLength getVcLength() {
            return readonlyConfig.vcLength;
        }

        public EndFlag getEndFlag() {
            return readonlyConfig.endFlag;
        }

        public String getSuffixOrder() {
            return readonlyConfig.suffixOrder;
        }
    }

    public PresampConfig(
            Map<String, String> vowelMappings,
            Map<String, Integer> vowelVolumes,
            Map<String, String> consonantMappings,
            Map<String, Boolean> consonantOverlaps,
            Map<String, VcLength> vcLengthOverrides,
            Set<String> neverVcv,
            DisjointLyricSet lyricConversions,
            Map<AliasType, ImmutableList<String>> aliasFormats,
            Set<String> prefixes,
            Map<SuffixType, Set<String>> suffixes,
            Set<SuffixType> allowUnderbarSuffixes,
            Set<SuffixType> excludeRepeatsSuffixes) {
        this.vowelMappings = vowelMappings;
        this.vowelVolumes = vowelVolumes;
        this.consonantMappings = consonantMappings;
        this.consonantOverlaps = consonantOverlaps;
        this.vcLengthOverrides = vcLengthOverrides;
        this.neverVcv = neverVcv;
        this.lyricConversions = lyricConversions;
        this.aliasFormats = aliasFormats;
        this.prefixes = prefixes;
        this.suffixes = suffixes;
        this.allowUnderbarSuffixes = allowUnderbarSuffixes;
        this.excludeRepeatsSuffixes = excludeRepeatsSuffixes;
    }

    public Builder toBuilder() {
        // Returns the builder of a new PresampConfig with this one's attributes.
        // Shallow copies work only for data structures containing immutable values.
        Map<SuffixType, Set<String>> newSuffixes = new HashMap<>();
        for (SuffixType suffixType : suffixes.keySet()) {
            newSuffixes.put(suffixType, new HashSet<>(suffixes.get(suffixType)));
        }
        return new Builder(new PresampConfig(
                new HashMap<>(vowelMappings),
                new HashMap<>(vowelVolumes),
                new HashMap<>(consonantMappings),
                new HashMap<>(consonantOverlaps),
                new HashMap<>(vcLengthOverrides),
                new HashSet<>(neverVcv),
                lyricConversions.deepcopy(),
                new HashMap<>(aliasFormats),
                new HashSet<>(prefixes),
                newSuffixes,
                new HashSet<>(allowUnderbarSuffixes),
                new HashSet<>(excludeRepeatsSuffixes)))
                .setAliasPriority(aliasPriority)
                .setAliasPriorityDifappend(aliasPriorityDifappend)
                .setAliasPriorityDifpitch(aliasPriorityDifpitch)
                .setEnableSplitting(enableSplitting)
                .setMustVC(mustVC)
                .setConsonantFlags(consonantFlags)
                .setVcLength(vcLength)
                .setEndFlag(endFlag)
                .setSuffixOrder(suffixOrder);
    }

    public DisjointLyricSet getLyricConversions() {
        return lyricConversions;
    }

    /** Returns a readonly view of a presamp config, useful for plugins. */
    public Reader getReader() {
        return new Reader(this);
    }
}
