package com.utsusynth.utsu.files.song;

import com.utsusynth.utsu.model.song.Song;

import java.io.File;

/** Read the contents of a file into a Song object. */
public interface SongReader {
    /** Guess the save format for a file. File is assumed to be compatible with this SongReader. */
    String getSaveFormat(File file);

    /** Get the number of tracks in a file. Each can be read into a separate Song. */
    int getNumTracks(File file);

    /** Load one track of a file into a song. */
    Song loadSong(File file, int trackNum);
}
