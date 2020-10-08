package com.utsusynth.utsu.files;

import com.utsusynth.utsu.UtsuModule.SettingsPath;
import com.utsusynth.utsu.common.exception.ErrorLogger;
import org.apache.commons.io.FileUtils;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Manages assets, defined in this context as built-in resources that need to be stored and
 * accessed as files, not just as input streams.
 *
 * <p>Storing these in the settings directory for now, but most of them will eventually be moved
 * to a user-visible workspace.
 */
public class AssetManager {
    private static final ErrorLogger errorLogger = ErrorLogger.getLogger();

    private final File assetPath;
    private final File soundsPath;

    @Inject
    public AssetManager(@SettingsPath File settingsPath) {
        assetPath = new File(settingsPath, "assets");
        soundsPath = new File(assetPath, "sounds");
    }

    /**
     * Should be called once when application loads.
     */
    public void initialize() throws IOException, SecurityException {
        // Initialize sounds.
        if (!soundsPath.exists() && !soundsPath.mkdirs()) {
            System.out.println("Error: Failed to create sounds path.");
            return;
        }

        File silenceDest = new File(assetPath, "sounds/silence.wav");
        if (!silenceDest.exists()) {
            InputStream silenceSource =
                    getClass().getResourceAsStream("/assets/sounds/silence.wav");
            FileUtils.copyInputStreamToFile(silenceSource, silenceDest);
        }
    }

    public File getSilenceFile() {
        return new File(assetPath, "sounds/silence.wav");
    }
}
