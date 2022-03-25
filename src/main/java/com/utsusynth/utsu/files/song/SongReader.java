package com.utsusynth.utsu.files.song;

import com.utsusynth.utsu.model.song.Song;

/** Read the contents of a file into a Song object. */
public interface SongReader {
    /** Get the number of tracks in a file. Each can be read into a separate Song. */
    int getNumTracks(String fileContents);

    /** Load one track of a file into a song. */
    Song loadSong(String fileContents, int trackNum);
}
