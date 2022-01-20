package com.utsusynth.utsu.common.data;

import com.utsusynth.utsu.model.voicebank.DisjointLyricSet;
import com.utsusynth.utsu.model.voicebank.LyricConfigMap;
import com.utsusynth.utsu.model.voicebank.PitchMap;

import java.util.Optional;

/** Readonly data about a voicebank. */
public class VoicebankData {
    // Readonly lyric conversions.
    private final DisjointLyricSet.Reader lyricConversions;
    // Readonly lyric configs.
    private final LyricConfigMap.Reader lyricConfigs;
    // Readonly pitch map.
    private final PitchMap.Reader pitchMap;

    public VoicebankData(DisjointLyricSet.Reader lyricConversions, LyricConfigMap.Reader lyricConfigs, PitchMap.Reader pitchMap) {
        this.lyricConversions = lyricConversions;
        this.lyricConfigs = lyricConfigs;
        this.pitchMap = pitchMap;
    }

    public DisjointLyricSet.Reader getLyricConversions() {
        return lyricConversions;
    }

    public LyricConfigMap.Reader getLyricConfigs() {
        return lyricConfigs;
    }

    public PitchMap.Reader getPitchMap() {
        return pitchMap;
    }
}
