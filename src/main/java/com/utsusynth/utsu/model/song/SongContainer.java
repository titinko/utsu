package com.utsusynth.utsu.model.song;

import java.io.File;
import com.google.inject.Inject;

/** Manages a single song and its save settings. */
public class SongContainer {
    private File location;
    private String saveFormat;
    private boolean hasPermanentLocation;

    private final SongManager songManager;

    @Inject
    public SongContainer(SongManager songManager, Song song) {
        this.songManager = songManager;
        location = songManager.addSong(song);
        saveFormat = "UST 2.0 (UTF-8)";
        hasPermanentLocation = false;
    }

    public Song getSong() {
        return songManager.getSong(location);
    }

    public void setSong(Song newSong) {
        songManager.setSong(location, newSong);
    }

    public File getLocation() {
        return location;
    }

    public void setLocation(File newLocation) {
        if (!newLocation.equals(location)) {
            songManager.moveSong(location, newLocation);
        }
        location = newLocation;
        hasPermanentLocation = true;
    }

    public String getSaveFormat() {
        return saveFormat;
    }

    public void setSaveFormat(String saveFormat) {
        this.saveFormat = saveFormat;
    }

    public boolean hasPermanentLocation() {
        return hasPermanentLocation;
    }
}
