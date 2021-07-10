package com.utsusynth.utsu.view.song.note.lyric;

/** Callback from Lyric to Note. */
public interface LyricCallback {
    void setSongLyric(String newLyric);

    void replaceSongLyric(String oldLyric, String newLyric);
}
