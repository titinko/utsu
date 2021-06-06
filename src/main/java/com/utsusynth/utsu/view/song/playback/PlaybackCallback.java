package com.utsusynth.utsu.view.song.playback;

import com.utsusynth.utsu.view.song.TrackItem;

public interface PlaybackCallback {
    // Move a bar to a new column on the track and remove it from current columns if needed.
    void setBar(TrackItem bar);

    // Remove a bar from the track.
    void removeBar(TrackItem bar);

    // Add a bar to new columns and remove old columns if necessary.
    void readjust(TrackItem bar);
}
