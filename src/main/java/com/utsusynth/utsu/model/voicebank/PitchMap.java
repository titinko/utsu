package com.utsusynth.utsu.model.voicebank;

import java.util.*;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.utsusynth.utsu.common.utils.Pitch;
import com.utsusynth.utsu.common.utils.PitchUtils;

public class PitchMap {
    private final ImmutableList<String> allPitches;
    private final Map<String, String> prefixes;
    private final Set<String> allPrefixes;
    private final Map<String, String> suffixes;
    private final Set<String> allSuffixes;

    public static class Reader {
        private final Map<String, String> readonlyPrefixes;
        private final Map<String, String> readonlySuffixes;
        private Reader(Map<String, String> readonlyPrefixes, Map<String, String> readonlySuffixes) {
            this.readonlyPrefixes = readonlyPrefixes;
            this.readonlySuffixes = readonlySuffixes;
        }

        public String getPrefix(String pitch) {
            if (readonlyPrefixes.containsKey(pitch)) {
                return readonlyPrefixes.get(pitch);
            }
            return "";
        }

        public String getSuffix(String pitch) {
            if (readonlySuffixes.containsKey(pitch)) {
                return readonlySuffixes.get(pitch);
            }
            return "";
        }
    }

    public PitchMap() {
        prefixes = new HashMap<>();
        allPrefixes = new HashSet<>();
        suffixes = new HashMap<>();
        allSuffixes = new HashSet<>();
        ImmutableList.Builder<String> pitchBuilder = ImmutableList.builder();
        for (int octave = 7; octave > 0; octave--) {
            for (String pitch : PitchUtils.REVERSE_PITCHES) {
                pitchBuilder.add(pitch + octave);
            }
        }
        allPitches = pitchBuilder.build();
    }

    public String getPrefix(String pitch) {
        if (prefixes.containsKey(pitch)) {
            return prefixes.get(pitch);
        }
        return "";
    }

    public ImmutableList<String> getAllPrefixes() {
        return ImmutableList.copyOf(allPrefixes);
    }

    public String getSuffix(String pitch) {
        if (suffixes.containsKey(pitch)) {
            return suffixes.get(pitch);
        }
        return "";
    }

    public ImmutableList<String> getAllSuffixes() {
        return ImmutableList.copyOf(allSuffixes);
    }

    public void putPrefix(String pitch, String prefix) {
        prefixes.put(pitch, prefix);
        allPrefixes.add(prefix);
    }

    public void putSuffix(String pitch, String suffix) {
        suffixes.put(pitch, suffix);
        allSuffixes.add(suffix);
    }

    public Iterator<String> getOrderedPitches() {
        return allPitches.iterator();
    }

    /** Returns a readonly view of a pitch map, useful for plugins. */
    public Reader getReader() {
        return new Reader(prefixes, suffixes);
    }
}
