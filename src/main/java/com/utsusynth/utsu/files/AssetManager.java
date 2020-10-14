package com.utsusynth.utsu.files;

import com.utsusynth.utsu.UtsuModule.SettingsPath;
import org.apache.commons.io.FileUtils;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Scanner;

/**
 * Manages assets, defined in this context as built-in resources that need to be stored and
 * accessed as files, not just as input streams.
 *
 * <p>Storing these in the settings directory for now, but most of them will eventually be moved
 * to a user-visible workspace.
 */
public class AssetManager {
    private static final String ASSET_SOURCE = "/assets/";
    private static final String SOUNDS_SOURCE = ASSET_SOURCE + "sounds/";
    private static final String VOICE_SOURCE = SOUNDS_SOURCE + "Iona_Beta/";
    private static final String EXECUTABLES_SOURCE = ASSET_SOURCE + "executables/";
    private static final String CONFIG_SOURCE = ASSET_SOURCE + "config/";

    private final File assetPath;
    private final File soundsPath;
    private final File voicePath;
    private final File executablesPath;
    private final File configPath;

    @Inject
    public AssetManager(@SettingsPath File settingsPath) {
        assetPath = new File(settingsPath, "assets");
        soundsPath = new File(assetPath, "sounds");
        voicePath = new File(soundsPath, "Iona_Beta");
        executablesPath = new File(assetPath, "executables");
        configPath = new File(assetPath, "config");
    }

    /**
     * Should be called once when application loads.
     */
    public boolean initializeAssets() throws IOException, SecurityException, URISyntaxException {
        // Initialize sounds.
        if (!soundsPath.exists() && !soundsPath.mkdirs()) {
            System.out.println("Error: Failed to create sounds path.");
            return false;
        }
        copyFile(SOUNDS_SOURCE, soundsPath, "silence.wav");
        initializeVoicebank();

        // Initialize executables.
        if (!executablesPath.exists() && !executablesPath.mkdirs()) {
            System.out.println("Error: Failed to create executables path.");
            return false;
        }
        copyFile(EXECUTABLES_SOURCE, executablesPath, "LICENSES");
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            copyExecFile(EXECUTABLES_SOURCE + "win64/", executablesPath, "macres.exe");
            copyExecFile(EXECUTABLES_SOURCE + "win64/", executablesPath, "wavtool-yawu.exe");
            copyExecFile(EXECUTABLES_SOURCE + "win64/", executablesPath, "frq0003gen.exe");
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
            return false;
        }
        copyFile(CONFIG_SOURCE, configPath, "lyric_conversions.txt");
        return true;
    }

    private void initializeVoicebank() throws URISyntaxException, IOException {
        if (!voicePath.exists() && !voicePath.mkdirs()) {
            System.out.println("Error: Failed to create voice path.");
            return;
        }
        copyFile(VOICE_SOURCE, voicePath, "character.txt");
        copyFile(VOICE_SOURCE, voicePath, "iona.bmp");
        copyFile(VOICE_SOURCE, voicePath, "oto.ini");
        copyFile(VOICE_SOURCE, voicePath, "oto_ini.txt");
        copyFile(VOICE_SOURCE, voicePath, "prefixmap");

        // Resource directories are tricky to search, so pull filenames from the oto instead.
        InputStream otoStream = getClass().getResourceAsStream(VOICE_SOURCE + "oto.ini");
        Scanner otoScanner = new Scanner(otoStream);
        while (otoScanner.hasNextLine()) {
            String otoLine = otoScanner.nextLine();
            int endIndex = otoLine.indexOf('=');
            if (endIndex > 0) {
                String fileName = otoLine.substring(0, endIndex);
                if (fileName.endsWith(".wav")) {
                    copyFile(VOICE_SOURCE, voicePath, fileName);
                }
            }
        }
    }

    public File getSilenceFile() {
        return new File(soundsPath, "silence.wav");
    }

    public File getVoicePath() {
        return voicePath;
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
            URL source = getClass().getResource(sourcePath + name);
            FileUtils.copyURLToFile(source, destination);
        }
        return destination;
    }

    private void copyExecFile(String sourcePath, File destPath, String name) throws IOException {
        File executable = copyFile(sourcePath, destPath, name);
        if (!(executable.setExecutable(true))) {
            System.out.println("Unable to set file to executable.");
        }
    }
}
