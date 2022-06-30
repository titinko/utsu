package com.utsusynth.utsu.model.voicebank;

import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableList;
import com.utsusynth.utsu.common.data.LyricConfigData;
import com.utsusynth.utsu.common.data.LyricConfigData.FrqStatus;
import com.utsusynth.utsu.common.data.PitchMapData;
import com.utsusynth.utsu.common.data.VoicebankData;
import com.utsusynth.utsu.common.utils.Pitch;
import com.utsusynth.utsu.common.utils.PitchUtils;
import com.utsusynth.utsu.engine.FrqGenerator;
import com.utsusynth.utsu.files.PreferencesManager;
import com.utsusynth.utsu.files.PreferencesManager.GuessAliasMode;

import java.io.File;
import java.util.*;

/**
 * In-code representation of a voice bank. Compatible with oto.ini files. TODO: Support oto_ini.txt
 * as well
 */
public class Voicebank {
    // TODO: Once you have a VoicebankManager, consider sharing between voicebanks.
    // private final DisjointLyricSet conversionSet;
    private final LyricConfigMap lyricConfigs;
    private final PitchMap pitchMap;
    private final Set<File> soundFiles;
    private final FrqGenerator frqGenerator;
    private final PreferencesManager preferencesManager;
    private PresampConfig presampConfig; // Immutable value.

    private File pathToVoicebank; // Example: "/Library/Iona.utau/"
    private String name; // Example: "Iona"
    private String author; // Example: "Lethe"
    private String description; // Contents of readme.txt
    private String imageName; // Example: "img.bmp"
    private String portraitName; // Example: "portrait.bmp"
    private double portraitOpacity; // Example: 0.67

    public static class Builder {
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

        public Builder setPortraitName(String portraitName) {
            newVoicebank.portraitName = portraitName;
            return this;
        }

        public Builder setPortraitOpacity(double portraitOpacity) {
            newVoicebank.portraitOpacity = portraitOpacity;
            return this;
        }

        public void addLyric(LyricConfig config, boolean hasFrq) {
            newVoicebank.lyricConfigs.addConfig(config);
            if (hasFrq) {
                newVoicebank.soundFiles.add(config.getPathToFile());
            }
        }

        public void addPitchPrefix(String pitch, String prefix) {
            newVoicebank.pitchMap.putPrefix(pitch, prefix);
        }

        public void addPitchSuffix(String pitch, String suffix) {
            newVoicebank.pitchMap.putSuffix(pitch, suffix);
        }

        public void setPresampConfig(PresampConfig presampConfig) {
            newVoicebank.presampConfig = presampConfig;
        }

        public Voicebank build() {
            if (newVoicebank.pathToVoicebank == null) {
                // TODO: Handle this.
                System.out.println("Tried to build an empty voicebank!");
            }
            return newVoicebank;
        }
    }

    public Voicebank(
            LyricConfigMap lyricConfigs,
            PitchMap pitchMap,
            Set<File> soundFiles,
            FrqGenerator frqGenerator,
            PreferencesManager preferencesManager,
            PresampConfig presampConfig) {
        this.lyricConfigs = lyricConfigs;
        this.pitchMap = pitchMap;
        this.soundFiles = soundFiles;
        this.frqGenerator = frqGenerator;
        this.preferencesManager = preferencesManager;
        this.presampConfig = presampConfig;

        // Default values.
        this.name = "";
        this.author = "";
        this.description = "";
        this.imageName = "";
        this.portraitName = "";
        this.portraitOpacity = 0.5;
    }

    public Builder toBuilder() {
        // Returns the builder of a new Voicebank with this one's attributes.
        // The old Voicebank's final fields are used--the objects are not regenerated.
        return new Builder(
                new Voicebank(
                        this.lyricConfigs,
                        this.pitchMap,
                        this.soundFiles,
                        this.frqGenerator,
                        this.preferencesManager,
                        this.presampConfig))
                .setPathToVoicebank(this.pathToVoicebank)
                .setName(this.name)
                .setAuthor(this.author)
                .setDescription(this.description)
                .setImageName(this.imageName)
                .setPortraitName(this.portraitName)
                .setPortraitOpacity(this.portraitOpacity);
    }

    public List<String> getPrefixes() {
        return sortPrefixSuffix(pitchMap.getAllPrefixes());
    }

    public List<String> getSuffixes() {
        return sortPrefixSuffix(pitchMap.getAllSuffixes());
    }

    private List<String> sortPrefixSuffix(List<String> toSort) {
        ArrayList<String> notPitches = new ArrayList<>();
        TreeSet<Pitch> pitches = new TreeSet<>();
        for (String prefixSuffix : toSort) {
            if (PitchUtils.looksLikePitch(prefixSuffix)) {
                pitches.add(PitchUtils.pitchStringToPitch(prefixSuffix));
            } else {
                notPitches.add(prefixSuffix);
            }
        }
        // Pitches on top, with higher pitches higher on the list.
        ImmutableList.Builder<String> result = ImmutableList.builder();
        for (Pitch pitch : pitches.descendingSet()) {
            result.add(pitch.toString());
        }
        // Miscellaneous prefixes/suffixes on the bottom.
        return result.addAll(notPitches).build();
    }

    /**
     * Should be called when lyric is expected to have an exact match in voicebank.
     */
    public Optional<LyricConfig> getLyricConfig(String trueLyric) {
        // If the lyric matches here, don't bother searching any further.
        if (lyricConfigs.hasLyric(trueLyric)) {
            return Optional.of(lyricConfigs.getConfig(trueLyric));
        }
        return getLyricConfig("", trueLyric, "");
    }

    public Optional<LyricConfig> getLyricConfig(String prevLyric, String lyric, String pitch) {
        // If alias guessing is disabled, allow an exact match only.
        if (preferencesManager.getGuessAlias().equals(GuessAliasMode.DISABLED)) {
            return lyricConfigs.hasLyric(lyric)
                    ? Optional.of(lyricConfigs.getConfig(lyric)) : Optional.empty();
        }
        String vcvPrefix = getVowel(prevLyric) + " "; // Most common VCV format.
        String prefix = pitchMap.getPrefix(pitch); // Pitch prefix.
        String suffix = pitchMap.getSuffix(pitch); // Pitch suffix.

        // Check all possible prefix/lyric/suffix combinations.
        for (String combo : allCombinations(prefix, vcvPrefix, lyric, suffix)) {
            if (lyricConfigs.hasLyric(combo)) {
                return Optional.of(lyricConfigs.getConfig(combo));
            }
        }

        SortedSet<LyricConfig> matches = new TreeSet<>();
        String strippedLyric = PitchUtils.removePitches(lyric);
        for (String candidate : presampConfig.getLyricConversions().getGroup(strippedLyric)) {
            String convertedLyric =
                    PitchUtils.extractStartPitch(lyric)
                            + candidate
                            + PitchUtils.extractEndPitch(lyric);
            if (convertedLyric.equals(lyric)) {
                // Don't check the same lyric twice.
                continue;
            }

            for (String combo : allCombinations(prefix, vcvPrefix, convertedLyric, suffix)) {
                if (lyricConfigs.hasLyric(combo)) {
                    matches.add(lyricConfigs.getConfig(combo));
                }
            }
        }
        // For now, arbitrarily but consistently return the first match.
        if (!matches.isEmpty()) {
            return Optional.of(matches.first());
        }

        return Optional.empty();
    }

    // Finds the vowel sound of a lyric by converting to ASCII and taking the last character.
    private char getVowel(String prevLyric) {
        if (prevLyric.isEmpty()) {
            return '-'; // Return dash if there appears to be no previous note.
        }
        String strippedLyric = PitchUtils.removePitches(prevLyric);
        for (String convertedLyric : presampConfig.getLyricConversions().getGroup(strippedLyric)) {
            if (CharMatcher.ascii().matchesAllOf(convertedLyric)) {
                return convertedLyric.toLowerCase().charAt(convertedLyric.length() - 1);
            }
        }
        // Return dummy character if no vowel found.
        return ' ';
    }

    private List<String> allCombinations(
            String prefix, String vcvPrefix, String lyric, String suffix) {
        // Try to get the lyric with as much detail as possible.
        return ImmutableList.of(
                prefix + vcvPrefix + lyric + suffix,
                prefix + vcvPrefix + lyric,
                vcvPrefix + lyric + suffix,
                prefix + lyric + suffix,
                lyric + suffix,
                prefix + lyric,
                vcvPrefix + lyric,
                lyric);
    }

    /**
     * Returns a list of sub-folders for WAV files in the voicebank.
     */
    public Set<String> getCategories() {
        return lyricConfigs.getCategories();
    }

    public Iterator<LyricConfig> getLyricConfigs(String category) {
        return lyricConfigs.getConfigs(category);
    }

    /**
     * Gets iterator of lyric config data sets to print to the frontend.
     */
    public Iterator<LyricConfigData> getAllLyricData(String category) {
        Iterator<LyricConfig> configIterator = lyricConfigs.getConfigs(category);
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return configIterator.hasNext();
            }

            @Override
            public LyricConfigData next() {
                LyricConfig config = configIterator.next();
                if (config != null) {
                    return config.getData(soundFiles.contains(config.getPathToFile()));
                }
                return null;
            }
        };
    }

    public Optional<LyricConfigData> getLyricData(String trueLyric) {
        Optional<LyricConfig> config = getLyricConfig("", trueLyric, "");
        return config.map(lyricConfig -> lyricConfig.getData(
                soundFiles.contains(lyricConfig.getPathToFile())));
    }

    public boolean addLyricData(LyricConfigData data) {
        LyricConfig newConfig = new LyricConfig(
                pathToVoicebank,
                data.getLyric(),
                data.getFileName(),
                data.getConfigValues());
        return lyricConfigs.addConfig(newConfig);
    }

    public void removeLyricConfig(String lyric) {
        lyricConfigs.removeConfig(lyric);
    }

    public void modifyLyricData(LyricConfigData data) {
        LyricConfig newConfig = new LyricConfig(
                pathToVoicebank,
                data.getLyric(),
                data.getFileName(),
                data.getConfigValues());
        lyricConfigs.setConfig(newConfig);
    }

    public Iterator<PitchMapData> getPitchData() {
        Iterator<String> pitchIterator = pitchMap.getOrderedPitches();
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return pitchIterator.hasNext();
            }

            @Override
            public PitchMapData next() {
                String pitch = pitchIterator.next();
                return new PitchMapData(
                        pitch, pitchMap.getPrefix(pitch), pitchMap.getSuffix(pitch));
            }
        };
    }

    public void setPitchData(PitchMapData data) {
        // Replace value that has changed, leave others the same.
        pitchMap.putPrefix(data.getPitch(), data.getPrefix());
        pitchMap.putSuffix(data.getPitch(), data.getSuffix());
    }

    private boolean generateFrq(File wavFile) {
        String wavName = wavFile.getName();
        String frqName = wavName.substring(0, wavName.length() - 4) + "_wav.frq";
        File frqFile = wavFile.getParentFile().toPath().resolve(frqName).toFile();
        frqGenerator.genFrqFile(wavFile, frqFile);
        if (frqFile.canRead()) {
            soundFiles.remove(frqFile); // Removes existing frq file, if present.
            soundFiles.add(frqFile);
            return true;
        }
        return false;
    }

    /**
     * Generates the specified frq files and updates each piece of data.
     */
    public void generateFrqs(Iterator<LyricConfigData> dataIterator) {
        while (dataIterator.hasNext()) {
            LyricConfigData data = dataIterator.next();
            if (data == null) {
                continue;
            }
            data.setFrqStatus(FrqStatus.LOADING);
            if (generateFrq(data.getPathToFile())) {
                data.setFrqStatus(FrqStatus.VALID);
            } else {
                data.setFrqStatus(FrqStatus.INVALID);
            }
        }
    }

    /** Get readonly data about the voicebank. Useful for plugins. */
    public VoicebankData getReadonlyData() {
        return new VoicebankData(
                lyricConfigs.getReader(), pitchMap.getReader(), presampConfig.getReader());
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

    public String getPortraitPath() {
        return new File(pathToVoicebank, portraitName).getAbsolutePath();
    }

    public double getPortraitOpacity() {
        return portraitOpacity;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        // Crappy string representation of a Voicebank object.
        String result = "";
        return result + " " + pathToVoicebank + " " + name + " " + imageName + " " + portraitName;
    }
}
