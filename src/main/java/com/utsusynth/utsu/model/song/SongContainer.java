package com.utsusynth.utsu.model.song;

import java.io.File;
import com.google.inject.Inject;
import com.utsusynth.utsu.common.exception.FileAlreadyOpenException;

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

    public Song get() {
        return songManager.getSong(location);
    }

    public void reset() {
        Song song = songManager.getSong(location);
        songManager.removeSong(location);
        location = songManager.addSong(song);
        saveFormat = "UST 2.0 (UTF-8)";
        hasPermanentLocation = false;
    }

    public void setSong(Song newSong) {
        songManager.setSong(location, newSong);
    }

    public File getLocation() {
        return location;
    }

    public void setLocation(File newLocation) throws FileAlreadyOpenException {
        if (!newLocation.equals(location)) {
            songManager.moveSong(location, newLocation);
        }
        location = newLocation;
        hasPermanentLocation = true;
    }

    public void removeSong() {
        // Editors opening this song in the future will have to reload it from file.
        songManager.removeSong(location);
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
