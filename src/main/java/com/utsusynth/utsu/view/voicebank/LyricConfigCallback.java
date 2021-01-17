package com.utsusynth.utsu.view.voicebank;

import com.utsusynth.utsu.common.data.LyricConfigData;

public interface LyricConfigCallback {
    /** Records an action so it can be undone or redone later. */
    void recordAction(Runnable redoAction, Runnable undoAction);

    void refreshEditor(LyricConfigData lyricData);

    void playLyricWithResampler(LyricConfigData lyricData, boolean modulation);

}
