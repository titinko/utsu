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

    private static final String ASSET_SOURCE = "/assets/";
    private static final String SOUNDS_SOURCE = ASSET_SOURCE + "sounds/";
    private static final String EXECUTABLES_SOURCE = ASSET_SOURCE + "executables/";
    private static final String CONFIG_SOURCE = ASSET_SOURCE + "config/";

    private final File assetPath;
    private final File soundsPath;
    private final File executablesPath;
    private final File configPath;

    @Inject
    public AssetManager(@SettingsPath File settingsPath) {
        assetPath = new File(settingsPath, "assets");
        soundsPath = new File(assetPath, "sounds");
        executablesPath = new File(assetPath, "executables");
        configPath = new File(assetPath, "config");
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
        copyFile(SOUNDS_SOURCE, soundsPath, "silence.wav");

        // Initialize executables.
        if (!executablesPath.exists() && !executablesPath.mkdirs()) {
            System.out.println("Error: Failed to create executables path.");
        }
        copyFile(EXECUTABLES_SOURCE, executablesPath, "LICENSES");
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            copyExecFile(EXECUTABLES_SOURCE + "win64/", executablesPath, "macres");
            copyExecFile(EXECUTABLES_SOURCE + "win64/", executablesPath, "wavtool-yawu");
            copyExecFile(EXECUTABLES_SOURCE + "win64/", executablesPath, "frq0003gen");
        } else if (os.contains("mac")) {
            copyExecFile(EXECUTABLES_SOURCE + "Mac/", executablesPath, "macres");
            copyExecFile(EXECUTABLES_SOURCE + "Mac/", executablesPath, "wavtool-yawu");
            copyExecFile(EXECUTABLES_SOURCE + "Mac/", executablesPath, "frq0003gen");
        } else {
            copyExecFile(EXECUTABLES_SOURCE + "linux64/", executablesPath, "macres");
            copyExecFile(EXECUTABLES_SOURCE + "linux64/", executablesPath, "wavtool-yawu");
            copyExecFile(EXECUTABLES_SOURCE + "linux64/", executablesPath, "frq0003gen");
        }

        // Initialize configs.
        if (!configPath.exists() && !configPath.mkdirs()) {
            System.out.println("Error: Failed to create config path.");
            return;
        }
        copyFile(CONFIG_SOURCE, configPath, "lyric_conversions.txt");
    }

    public File getSilenceFile() {
        return new File(soundsPath, "silence.wav");
    }

    public File getResamplerFile() {
        return new File(executablesPath, "macres");
    }

    public File getWavtoolFile() {
        return new File(executablesPath, "wavtool-yawu");
    }

    public File getFrqGeneratorFile() {
        return new File(executablesPath, "frq0003gen");
    }

    public File getLyricConversionFile() {
        return new File(configPath, "lyric_conversions.txt");
    }

    private File copyFile(String sourcePath, File destPath, String name) throws IOException {
        File destination = new File(destPath, name);
        if (!destination.exists()) {
            InputStream source =
                    getClass().getResourceAsStream(sourcePath + name);
            FileUtils.copyInputStreamToFile(source, destination);
        }
        return destination;
    }

    private void copyExecFile(String sourcePath, File destPath, String name) throws IOException {
        File executable = copyFile(sourcePath, destPath, name);
        executable.setExecutable(true);
    }
}
