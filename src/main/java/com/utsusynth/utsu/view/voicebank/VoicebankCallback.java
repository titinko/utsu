package com.utsusynth.utsu.view.voicebank;

import java.util.Iterator;
import com.utsusynth.utsu.common.data.LyricConfigData;

public interface VoicebankCallback {
    Iterator<LyricConfigData> getLyricData(String category);

    /**
     * Adds a new lyric, unless it would replace an existing one.
     * 
     * @return Whether the new lyric had a name collision.
     */
    boolean addLyric(LyricConfigData lyricData);

    void removeLyric(String lyric);

    void modifyLyric(LyricConfigData lyricData);
}
