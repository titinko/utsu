package com.utsusynth.utsu.common.data;

import com.utsusynth.utsu.model.voicebank.DisjointLyricSet;

import java.util.Optional;

/** Readonly data about a voicebank. */
public class VoicebankData {
    // Readonly lyric conversions.
    private DisjointLyricSet.Reader lyricConversions;

    public VoicebankData(DisjointLyricSet.Reader lyricConversions) {
        this.lyricConversions = lyricConversions;
    }

    public DisjointLyricSet.Reader getLyricConversions() {
        return lyricConversions;
    }
}
