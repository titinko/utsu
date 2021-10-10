package com.utsusynth.utsu.files;

import com.google.inject.Inject;
import com.utsusynth.utsu.UtsuModule.SettingsPath;
import com.utsusynth.utsu.common.exception.ErrorLogger;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

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
    private static final String VOICE_SOURCE = SOUNDS_SOURCE + "Iona_Beta/";
    private static final String EXECUTABLES_SOURCE = ASSET_SOURCE + "executables/";
    private static final String CONFIG_SOURCE = ASSET_SOURCE + "config/";

    private final File assetPath;
    private final File soundsPath;
    private final File voicePath;
    private final File executablesPath;
    private final File configPath;

    private final Map<String, List<Integer>> currentVersions;
    private final Map<String, List<Integer>> oldVersions;

    @Inject
    public AssetManager(@SettingsPath File settingsPath) {
        assetPath = new File(settingsPath, "assets");
        soundsPath = new File(assetPath, "sounds");
        voicePath = new File(soundsPath, "Iona_Beta");
        executablesPath = new File(assetPath, "executables");
        configPath = new File(assetPath, "config");
        currentVersions = new HashMap<>();
        oldVersions = new HashMap<>();
    }

    /**
     * Should be called once when application loads.
     */
    public boolean initializeAssets() throws IOException, SecurityException, URISyntaxException {
        // Initialize versions.
        parseVersions(currentVersions, IOUtils.toString(getClass().getResource(
                ASSET_SOURCE + "versions.txt"), StandardCharsets.UTF_8));
        File oldVersionsFile = new File(assetPath, "versions.txt");
        if (oldVersionsFile.exists()) {
            parseVersions(oldVersions, FileUtils.readFileToString(
                    new File(assetPath, "versions.txt"), StandardCharsets.UTF_8));
        }

        // Initialize sounds.
        if (!soundsPath.exists() && !soundsPath.mkdirs()) {
            System.out.println("Error: Failed to create sounds path.");
            return false;
        }
        copyFile(SOUNDS_SOURCE, soundsPath, "silence.wav", "SILENCE_WAV");
        initializeVoicebank();

        // Initialize executables.
        if (!executablesPath.exists() && !executablesPath.mkdirs()) {
            System.out.println("Error: Failed to create executables path.");
            return false;
        }
        copyFile(EXECUTABLES_SOURCE, executablesPath, "LICENSES", "LICENSES");
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            String windowsPath = EXECUTABLES_SOURCE + "win64/";
            copyExecFile(windowsPath, executablesPath, "macres.exe", "MACRES");
            copyExecFile(windowsPath, executablesPath, "wavtool-yawu.exe", "WAVTOOL_YAWU");
            copyExecFile(windowsPath, executablesPath, "frq0003gen.exe", "FRQ0003GEN");
        } else if (os.contains("mac")) {
            String macPath = EXECUTABLES_SOURCE + "Mac/";
            copyExecFile(macPath, executablesPath, "macres", "MACRES");
            copyExecFile(macPath, executablesPath, "wavtool-yawu", "WAVTOOL_YAWU");
            copyExecFile(macPath, executablesPath, "frq0003gen", "FRQ0003GEN");
        } else {
            String linuxPath = EXECUTABLES_SOURCE + "linux64/";
            copyExecFile(linuxPath, executablesPath, "macres", "MACRES");
            copyExecFile(linuxPath, executablesPath, "wavtool-yawu", "WAVTOOL_YAWU");
            copyExecFile(linuxPath, executablesPath, "frq0003gen", "FRQ0003GEN");
        }

        // Initialize configs.
        if (!configPath.exists() && !configPath.mkdirs()) {
            System.out.println("Error: Failed to create config path.");
            return false;
        }
        copyFile(CONFIG_SOURCE, configPath, "lyric_conversions.txt", "LYRIC_CONVERSIONS");

        // Write versions.
        writeVersions(currentVersions, assetPath);
        return true;
    }

    private void initializeVoicebank() throws IOException {
        if (!voicePath.exists() && !voicePath.mkdirs()) {
            System.out.println("Error: Failed to create voice path.");
            return;
        }
        copyFile(VOICE_SOURCE, voicePath, "character.txt", "DEFAULT_VOICEBANK");
        copyFile(VOICE_SOURCE, voicePath, "iona.bmp", "DEFAULT_VOICEBANK");
        copyFile(VOICE_SOURCE, voicePath, "oto.ini", "DEFAULT_VOICEBANK");
        copyFile(VOICE_SOURCE, voicePath, "oto_ini.txt", "DEFAULT_VOICEBANK");
        copyFile(VOICE_SOURCE, voicePath, "prefixmap", "DEFAULT_VOICEBANK");

        // Resource directories are tricky to search, so pull filenames from the oto instead.
        InputStream otoStream = getClass().getResourceAsStream(VOICE_SOURCE + "oto.ini");
        Scanner otoScanner = new Scanner(otoStream);
        while (otoScanner.hasNextLine()) {
            String otoLine = otoScanner.nextLine();
            int endIndex = otoLine.indexOf('=');
            if (endIndex > 0) {
                String fileName = otoLine.substring(0, endIndex);
                if (fileName.endsWith(".wav")) {
                    copyFile(VOICE_SOURCE, voicePath, fileName, "DEFAULT_VOICEBANK");
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
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return new File(executablesPath, "macres.exe");
        }
        return new File(executablesPath, "macres");
    }

    public File getWavtoolFile() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return new File(executablesPath, "wavtool-yawu.exe");
        }
        return new File(executablesPath, "wavtool-yawu");
    }

    public File getFrqGeneratorFile() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return new File(executablesPath, "frq0003gen.exe");
        }
        return new File(executablesPath, "frq0003gen");
    }

    public File getLyricConversionFile() {
        return new File(configPath, "lyric_conversions.txt");
    }

    private File copyFile(
            String sourcePath, File destPath, String name, String versionKey) throws IOException {
        File destination = new File(destPath, name);
        if (!destination.exists() || shouldReplace(versionKey)) {
            URL source = getClass().getResource(sourcePath + name);
            FileUtils.copyURLToFile(source, destination);
        }
        return destination;
    }

    private void copyExecFile(
            String sourcePath, File destPath, String name, String versionKey) throws IOException {
        File executable = copyFile(sourcePath, destPath, name, versionKey);
        if (!(executable.setExecutable(true))) {
            System.out.println("Unable to set file to executable.");
        }
    }

    private boolean shouldReplace(String versionKey) {
        if (!currentVersions.containsKey(versionKey)) {
            return false;
        }
        if (!oldVersions.containsKey(versionKey)) {
            return true;
        }
        List<Integer> oldVersion = oldVersions.get(versionKey);
        List<Integer> newVersion = currentVersions.get(versionKey);
        for (int i = 0; i < 3; i++) {
            int oldVersionNumber = oldVersion.size() > i ? oldVersion.get(i) : 0;
            int newVersionNumber = newVersion.size() > i ? newVersion.get(i) : 0;
            if (newVersionNumber > oldVersionNumber) {
                return true;
            }
            if (newVersionNumber < oldVersionNumber) {
                return false;
            }
        }
        return false; // Case where version numbers are identical.
    }

    private static void parseVersions(Map<String, List<Integer>> versionMap, String versionsData) {
        versionMap.clear();
        for (String line : versionsData.split("\n")) {
            if (!line.contains("=")) {
                continue; // Assume this is not a version line.
            }
            String[] mapping = line.trim().split("=");
            if (mapping.length != 2 || mapping[0].isEmpty() || mapping[1].isEmpty()) {
                continue; // Assume this is not a version line.
            }
            if (versionMap.containsKey(mapping[0])) {
                System.out.println(
                        "Warning: Same version mapping appeared twice: " + mapping[0]);
                continue;
            }
            List<Integer> version = new ArrayList<>(3);
            for (String number : mapping[1].trim().split("\\.")) {
                try {
                    version.add(Integer.parseInt(number));
                } catch (NumberFormatException e) {
                    System.out.println("Warning: version formatted incorrectly: " + mapping[1]);
                    break;
                }
            }
            if (!version.isEmpty()) {
                versionMap.put(mapping[0], version);
            }
        }
    }

    private static void writeVersions(Map<String, List<Integer>> versions, File destPath) {
        File destination = new File(destPath, "versions.txt");
        try (PrintStream ps = new PrintStream(destination)) {
            for (String key : versions.keySet()) {
                ps.print(key + "=");
                List<Integer> versionNumbers = versions.get(key);
                if (versionNumbers.size() > 0) { // Major version.
                    ps.print(versionNumbers.get(0));
                }
                if (versionNumbers.size() > 1) { // Minor version.
                    ps.print("." + versionNumbers.get(1));
                }
                if (versionNumbers.size() > 2) { // Bugfix version.
                    ps.print("." + versionNumbers.get(2));
                }
                ps.println();
            }
            ps.flush();
        } catch (FileNotFoundException e) {
            // TODO: Handle this.
            errorLogger.logError(e);
        }
    }
}
