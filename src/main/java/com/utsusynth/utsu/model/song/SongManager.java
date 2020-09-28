package com.utsusynth.utsu.model.song;

import com.google.common.io.Files;
import com.utsusynth.utsu.common.exception.ErrorLogger;
import com.utsusynth.utsu.common.exception.FileAlreadyOpenException;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages all songs in use by Utsu. This class is a singleton to ensure the same song does not open
 * on two editors.
 */
public class SongManager {
    private static final ErrorLogger errorLogger = ErrorLogger.getLogger();

    private final Map<File, Song> songs;
    private final File tempDir;

    private int untitledCounter;

    public SongManager() {
        tempDir = Files.createTempDir();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                FileUtils.deleteDirectory(tempDir);
            } catch (IOException e) {
                errorLogger.logError(e);
            }
        }));
        songs = new HashMap<>();
        untitledCounter = 1;
    }

    public boolean hasSong(File location) {
        File normalized = normalize(location);
        return songs.containsKey(normalized);
    }

    public Song getSong(File location) {
        File normalized = normalize(location);
        return songs.get(normalized);
    }

    /**
     * Writes a new song into this location, replacing any old songs.
     */
    public void setSong(File location, Song song) {
        File normalized = normalize(location);
        songs.put(normalized, song);
    }

    public File addSong(Song song) {
        File tempLocation = normalize(new File(tempDir, "Untitled_" + untitledCounter++));
        songs.put(tempLocation, song);
        return tempLocation;
    }

    public void moveSong(File oldLocation, File newLocation) throws FileAlreadyOpenException {
        File normalizedOld = normalize(oldLocation);
        File normalizedNew = normalize(newLocation);
        if (songs.containsKey(normalizedNew)) {
            // No two tabs should point at the same file, to prevent headaches.
            throw new FileAlreadyOpenException(normalizedNew);
        }

        Song songToMove = songs.get(normalizedOld);
        songs.remove(normalizedOld);
        songs.put(normalizedNew, songToMove);
    }

    public void removeSong(File location) {
        File normalized = normalize(location);
        songs.remove(normalized);
    }

    private File normalize(File rawFile) {
        try {
            return rawFile.getCanonicalFile();
        } catch (IOException e) {
            // TODO: Handle this.
            errorLogger.logError(e);
        }
        // Return raw file if it cannot be normalized.
        return rawFile;
    }
}
