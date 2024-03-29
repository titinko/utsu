package com.utsusynth.utsu.files.voicebank;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.utsusynth.utsu.common.utils.UtsuFileUtils;
import com.utsusynth.utsu.files.AssetManager;
import com.utsusynth.utsu.files.PreferencesManager;
import com.utsusynth.utsu.model.voicebank.PresampConfig;
import org.apache.commons.io.FileUtils;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Provider;
import com.utsusynth.utsu.common.exception.ErrorLogger;
import com.utsusynth.utsu.model.voicebank.LyricConfig;
import com.utsusynth.utsu.model.voicebank.Voicebank;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;

public class VoicebankReader {
    private static final ErrorLogger errorLogger = ErrorLogger.getLogger();

    private static final Pattern LYRIC_PATTERN = Pattern.compile("(.+\\.wav)=([^,]*),");
    private static final Pattern LYRIC_CONFIG_PATTERN =
            Pattern.compile("([^,]*),([^,]*),([^,]*),([^,]*),([^,]*)");
    private static final Pattern PREFIX_PATTERN =
            Pattern.compile("([a-gA-G]#?[1-7])\\t(\\S.*)");
    private static final Pattern SUFFIX_PATTERN =
            Pattern.compile("([a-gA-G]#?[1-7])\\t\\S*\\t(\\S.*)");

    private final AssetManager assetManager;
    private final PreferencesManager preferencesManager;
    private final PresampConfigReader presampConfigReader;
    private final Provider<Voicebank> voicebankProvider;
    private final Provider<PresampConfig> presampConfigProvider;

    // Cache the default presamp.ini file here to avoid rereading every time.
    private PresampConfig defaultPresampConfig;

    @Inject
    public VoicebankReader(
            AssetManager assetManager,
            PreferencesManager preferencesManager,
            PresampConfigReader presampConfigReader,
            Provider<Voicebank> voicebankProvider,
            Provider<PresampConfig> presampConfigProvider) {
        this.assetManager = assetManager;
        this.preferencesManager = preferencesManager;
        this.presampConfigReader = presampConfigReader;
        this.voicebankProvider = voicebankProvider;
        this.presampConfigProvider = presampConfigProvider;
    }

    public File getDefaultPath() {
        return preferencesManager.getVoicebank();
    }

    public PresampConfig getDefaultPresampConfig() {
        if (defaultPresampConfig != null) {
            return defaultPresampConfig;
        }
        // Read default presamp.ini data if needed.
        String defaultData = UtsuFileUtils.readConfigFile(assetManager.getDefaultPresampIniFile());
        PresampConfig.Builder presampBuilder = presampConfigProvider.get().toBuilder();
        defaultPresampConfig =
                presampConfigReader.loadPresampConfig(presampBuilder, defaultData);
        return defaultPresampConfig;
    }

    public Voicebank loadVoicebankFromDirectory(File sourceDir) {
        Voicebank.Builder builder = voicebankProvider.get().toBuilder();

        File pathToVoicebank;
        if (!sourceDir.exists()) {
            pathToVoicebank = preferencesManager.getVoicebank();
        } else {
            if (!sourceDir.isDirectory()) {
                pathToVoicebank = sourceDir.getParentFile();
            } else {
                pathToVoicebank = sourceDir;
            }
        }
        builder.setPathToVoicebank(pathToVoicebank);
        System.out.println("Parsed voicebank as " + pathToVoicebank);

        // Parse character data in English or Japanese.
        parseCharacterData(pathToVoicebank, builder);

        // Parse description.
        File readmeFile = pathToVoicebank.toPath().resolve("readme.txt").toFile();
        builder.setDescription(UtsuFileUtils.readConfigFile(readmeFile));

        // Parse all oto_ini.txt and oto.ini files in arbitrary order.
        try {
            Files.walkFileTree(
                    pathToVoicebank.toPath(),
                    EnumSet.of(FileVisitOption.FOLLOW_LINKS),
                    10,
                    new SimpleFileVisitor<>() {
                        @Override
                        public FileVisitResult visitFile(Path path, BasicFileAttributes attr) {
                            for (String otoName : ImmutableSet.of("oto.ini", "oto_ini.txt")) {
                                if (path.endsWith(otoName)) {
                                    Path pathToFile = path.toFile().getParentFile().toPath();
                                    parseOtoIni(pathToVoicebank, pathToFile, otoName, builder);
                                    break;
                                }
                            }
                            return FileVisitResult.CONTINUE;
                        }
                    });
        } catch (IOException e) {
            // TODO: Handle this.
            errorLogger.logError(e);
        }

        // Parse pitch map in arbitrary order, if present.
        for (String pitchMapName : ImmutableSet.of("prefixmap", "prefix.map")) {
            parsePitchMap(pathToVoicebank.toPath().resolve(pitchMapName).toFile(), builder);
        }

        // Parse presamp.ini file if present, otherwise use default presamp config.
        parsePresampConfig(pathToVoicebank.toPath().resolve("presamp.ini").toFile(), builder);

        return builder.build();
    }

    private void parseCharacterData(File pathToVoicebank, Voicebank.Builder builder) {
        String characterData = UtsuFileUtils.readConfigFile(
                pathToVoicebank.toPath().resolve("character.txt").toFile());
        for (String rawLine : characterData.split("\n")) {
            String line = rawLine.trim();
            if (line.startsWith("name=")) {
                builder.setName(line.substring("name=".length()));
            } else if (line.startsWith("名前：")) {
                builder.setName(line.substring("名前：".length()));
            } else if (line.startsWith("author=")) {
                builder.setAuthor(line.substring("author=".length()));
            } else if (line.startsWith("CV：")) {
                builder.setAuthor(line.substring("CV：".length()));
            } else if (line.startsWith("image=")) {
                builder.setImageName(line.substring("image=".length()));
            } else if (line.startsWith("画像：")) {
                builder.setImageName(line.substring("画像：".length()));
            }
        }

        // Some voicebanks have extra character data in a yaml file.
        Node yamlNode = UtsuFileUtils.readYamlFile(
                pathToVoicebank.toPath().resolve("character.yaml").toFile());
        for (NodeTuple yamlEntry : UtsuFileUtils.getYamlMapEntries(yamlNode)) {
            String keyName = UtsuFileUtils.getYamlStringValue(yamlEntry.getKeyNode(), "");
            Node value = yamlEntry.getValueNode();
            switch (keyName) {
                case "name":
                    builder.setName(UtsuFileUtils.getYamlStringValue(value, ""));
                    break;
                case "image":
                    builder.setImageName(UtsuFileUtils.getYamlStringValue(value, ""));
                    break;
                case "author":
                    builder.setAuthor(UtsuFileUtils.getYamlStringValue(value, ""));
                    break;
                case "voice":
                    builder.setAuthor(UtsuFileUtils.getYamlStringValue(value, ""));
                    break;
                case "portrait":
                    builder.setPortraitName(UtsuFileUtils.getYamlStringValue(value, ""));
                    break;
                case "portrait_opacity":
                    builder.setPortraitOpacity(UtsuFileUtils.getYamlDoubleValue(value, 0.5));
                    break;
                case "text_file_encoding":
                case "web":
                case "subbanks":
                default:
                    // Do nothing.
            }
        }
    }

    private void parseOtoIni(
            File pathToVoicebank,
            Path pathToOtoFile,
            String otoFile,
            Voicebank.Builder builder) {
        String otoData = UtsuFileUtils.readConfigFile(pathToOtoFile.resolve(otoFile).toFile());
        for (String rawLine : otoData.split("\n")) {
            String line = rawLine.trim();
            Matcher matcher = LYRIC_PATTERN.matcher(line);
            if (matcher.find()) {
                String fileName = matcher.group(1); // Assuming this is a .wav file
                String lyricName = matcher.group(2);
                if (lyricName.isEmpty()) {
                    // If no alias provided, use the file name as an adhoc alias.
                    lyricName = fileName.substring(0, fileName.length() - 4);
                }
                Matcher configMatcher = LYRIC_CONFIG_PATTERN.matcher(line.substring(matcher.end()));
                if (!configMatcher.find() || configMatcher.groupCount() != 5 || fileName == null) {
                    System.out.println("Received unexpected results while parsing oto.ini");
                    continue;
                }
                String[] configValues = new String[5];
                for (int i = 0; i < 5; i++) {
                    configValues[i] = configMatcher.group(i + 1).equals("")
                            ? "0" : configMatcher.group(i + 1);
                }
                // Search for a frq file.
                String frqName = fileName.substring(0, fileName.length() - 4) + "_wav.frq";
                File frqFile = pathToOtoFile.resolve(frqName).toFile();
                builder.addLyric(
                        new LyricConfig(
                                pathToVoicebank,
                                pathToOtoFile.resolve(fileName).toFile(),
                                lyricName,
                                configValues),
                        frqFile.canRead());
            }
        }
    }

    private void parsePitchMap(File pitchMapFile, Voicebank.Builder builder) {
        String pitchData = UtsuFileUtils.readConfigFile(pitchMapFile);
        for (String rawLine : pitchData.split("\n")) {
            String line = rawLine.trim();
            Matcher prefixMatcher = PREFIX_PATTERN.matcher(line);
            if (prefixMatcher.find()) {
                String pitch = prefixMatcher.group(1);
                String prefix = prefixMatcher.group(2);
                builder.addPitchPrefix(pitch, prefix);
            }
            Matcher suffixMatcher = SUFFIX_PATTERN.matcher(line);
            if (suffixMatcher.find()) {
                String pitch = suffixMatcher.group(1);
                String suffix = suffixMatcher.group(2);
                builder.addPitchSuffix(pitch, suffix);
            }
        }
    }

    private void parsePresampConfig(File presampConfigFile, Voicebank.Builder builder) {
        // Override any fields populated in the voicebank's presamp.ini file.
        String presampData = UtsuFileUtils.readConfigFile(presampConfigFile);
        PresampConfig presampConfig = presampConfigReader.loadPresampConfig(
                getDefaultPresampConfig().toBuilder(), presampData);
        builder.setPresampConfig(presampConfig);
    }

    /**
     * Parses a file path, and replaces the strings "${DEFAULT}" and "${HOME}" with their
     * corresponding directories.
     */
    public File parseFilePath(String line, String property) {
        String pathString = line.substring(property.length());
        pathString = pathString
                .replaceFirst(
                        "\\$\\{DEFAULT\\}",
                        preferencesManager.getVoicebank().getAbsolutePath())
                .replaceFirst("\\$\\{HOME\\}", System.getProperty("user.home"));
        return new File(pathString);
    }
}
