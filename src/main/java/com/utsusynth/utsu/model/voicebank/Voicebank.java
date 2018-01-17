package com.utsusynth.utsu.model.voicebank;

import java.io.File;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import com.google.common.base.Optional;
import com.utsusynth.utsu.common.PitchUtils;

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

    Voicebank(
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

    public Optional<LyricConfig> getLyricConfig(String lyric) {
        return getLyricConfig(lyric, "");
    }

    public Optional<LyricConfig> getLyricConfig(String lyric, int noteNum) {
        return getLyricConfig(lyric, PitchUtils.noteNumToPitch(noteNum));
    }

    public Optional<LyricConfig> getLyricConfig(String lyric, String pitch) {
        // Exact lyric match is prioritized first.
        if (lyricConfigs.keySet().contains(lyric)) {
            return Optional.of(lyricConfigs.get(lyric));
        }

        // Attempt to add pitch suffix.
        String suffix = pitchMap.containsKey(pitch) ? pitchMap.get(pitch) : "";
        if (lyricConfigs.containsKey(lyric + suffix)) {
            return Optional.of(lyricConfigs.get(lyric + suffix));
        }

        SortedSet<LyricConfig> matches = new TreeSet<>();
        for (String convertedLyric : conversionSet.getGroup(lyric)) {
            if (lyricConfigs.keySet().contains(convertedLyric)) {
                matches.add(lyricConfigs.get(convertedLyric));
            }

            // Attempt to add pitch suffix.
            // TODO: Dot product this with lyric conversion group and later VCV group.
            if (lyricConfigs.containsKey(convertedLyric + suffix)) {
                matches.add(lyricConfigs.get(convertedLyric + suffix));
            }
        }
        // For now, arbitrarily but consistently return the first match.
        if (!matches.isEmpty()) {
            return Optional.of(matches.first());
        }

        return Optional.absent();
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
