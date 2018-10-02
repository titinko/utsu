package com.utsusynth.utsu.view.voicebank;

public interface LyricConfigCallback {
    /** Records an action so it can be undone or redone later. */
    void recordAction(Runnable redoAction, Runnable undoAction);
}
