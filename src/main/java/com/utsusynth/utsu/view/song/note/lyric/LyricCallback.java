package com.utsusynth.utsu.view.song.note.lyric;

import javafx.scene.layout.AnchorPane;

/** Callback from Lyric to Note. */
public interface LyricCallback {
    void saveChanges();

    void replaceSongLyric(String oldLyric, String newLyric);

    AnchorPane getLyricPane();
}
