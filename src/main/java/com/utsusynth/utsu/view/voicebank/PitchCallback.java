package com.utsusynth.utsu.view.voicebank;

import com.utsusynth.utsu.common.data.PitchMapData;

public interface PitchCallback {
    void setPitch(PitchMapData pitchData);

    /** Records an action so it can be undone or redone later. */
    void recordAction(Runnable redoAction, Runnable undoAction);
}
