package com.utsusynth.utsu.view.song.playback;

import com.utsusynth.utsu.view.song.TrackItem;

public interface PlaybackCallback {
    // Move a bar to a new column on the track and remove it from current columns.
    void moveBar(TrackItem bar);
}
