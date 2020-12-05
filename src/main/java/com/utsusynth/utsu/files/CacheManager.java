package com.utsusynth.utsu.files;

import com.google.inject.Inject;
import com.utsusynth.utsu.UtsuModule.SettingsPath;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

public class CacheManager {
    private final File cachePath;

    @Inject
    public CacheManager(@SettingsPath File settingsPath) {
        cachePath = new File(settingsPath, "cache");
    }

    /**
     * Should be called once when application loads.
     */
    public boolean initializeCache() {
        if (!cachePath.exists() && !cachePath.mkdirs()) {
            System.out.println("Error: Failed to create cache path.");
            return false;
        }
        clearAllCacheValues();
        return true;
    }

    public File createRenderedCache() {
        File renderedCache = new File(cachePath, UUID.randomUUID() + "_rendered.wav");
        renderedCache.deleteOnExit();
        return renderedCache;
    }

    public File createNoteCache() {
        File noteCache = new File(cachePath, UUID.randomUUID() + "_note.wav");
        noteCache.deleteOnExit();
        return noteCache;
    }

    public File createSilenceCache() {
        File silenceCache = new File(cachePath, UUID.randomUUID() + "_silence.wav");
        silenceCache.deleteOnExit();
        return silenceCache;
    }

    public boolean clearCache(File clearMe) {
        if (clearMe.exists()) {
            try {
                Files.delete(clearMe.toPath());
                return true;
            } catch (IOException e) {
                // This is expected if user tries to clear cache while file is in use.
                return false;
            }
        } else {
            System.out.println("Tried to delete cache file that no longer exists.");
            return false;
        }
    }

    public void clearNotes() {
        File[] silences = cachePath.listFiles((dir, name) -> name.endsWith("note.wav"));
        if (silences != null) {
            for (File silence : silences) {
                clearCache(silence);
            }
        }
    }

    public void clearSilences() {
        File[] silences = cachePath.listFiles((dir, name) -> name.endsWith("silence.wav"));
        if (silences != null) {
            for (File silence : silences) {
                clearCache(silence);
            }
        }
    }

    public void clearAllCacheValues() {
        File[] files = cachePath.listFiles();
        if (files != null) {
            for (File file : files) {
                clearCache(file);
            }
        }
    }
}
