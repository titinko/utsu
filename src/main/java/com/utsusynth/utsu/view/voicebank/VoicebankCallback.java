package com.utsusynth.utsu.view.voicebank;

import java.util.Iterator;
import com.utsusynth.utsu.common.data.LyricConfigData;

public interface VoicebankCallback {
    Iterator<LyricConfigData> getLyricData(String category);

    /**
     * Displays a lyric in the lyric config editor.
     */
    void displayLyric(LyricConfigData lyricData);

    /**
     * Adds a new lyric, unless it would replace an existing one.
     * 
     * @return False if the new lyric had a name collision, true otherwise.
     */
    boolean addLyric(LyricConfigData lyricData);

    void removeLyric(String lyric);

    void modifyLyric(LyricConfigData lyricData);

    void generateFrqFiles(Iterator<LyricConfigData> configData);

    /** Records an action so it can be undone or redone later. */
    void recordAction(Runnable redoAction, Runnable undoAction);
}
