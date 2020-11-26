package com.utsusynth.utsu.view.song.note;

/** Callback from TrackLyric to TrackNote. */
public interface LyricCallback {
    void setSongLyric(String newLyric);

    void replaceSongLyric(String oldLyric, String newLyric);

    void adjustColumnSpan();

    void bringToFront();
}
