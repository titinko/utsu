package com.utsusynth.utsu.view.voicebank;

public interface LyricConfigCallback {
    void highlight(LyricConfig lyric);

    boolean isHighlighted(LyricConfig lyric);
}
