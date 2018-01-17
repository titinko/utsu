package com.utsusynth.utsu.model.voicebank;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.MalformedInputException;
import java.nio.charset.UnmappableCharacterException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.utsusynth.utsu.common.exception.ErrorLogger;

public class VoicebankReader {
    private static final ErrorLogger errorLogger = ErrorLogger.getLogger();

    private static final Pattern LYRIC_PATTERN = Pattern.compile("(.+\\.wav)=([^,]*),");
    private static final Pattern PITCH_PATTERN = Pattern.compile("([a-gA-G]#?[1-7])\\s+(\\S.*)");

    private final File defaultVoicePath;
    private final File lyricConversionPath;

    @Inject
    public VoicebankReader(File defaultVoicePath, File lyricConversionPath) {
        this.defaultVoicePath = defaultVoicePath;
        this.lyricConversionPath = lyricConversionPath;
    }

    public File getDefaultPath() {
        return defaultVoicePath;
    }

    public Voicebank loadVoicebankFromDirectory(File sourceDir) {
        File pathToVoicebank;
        String name = "";
        String imageName = "";
        Map<String, LyricConfig> lyricConfigs = new HashMap<>();
        Map<String, String> pitchMap = new HashMap<>();

        if (!sourceDir.exists()) {
            pathToVoicebank = defaultVoicePath;
        } else {
            if (!sourceDir.isDirectory()) {
                pathToVoicebank = sourceDir.getParentFile();
            } else {
                pathToVoicebank = sourceDir;
            }
        }
        System.out.println("Parsed voicebank as " + pathToVoicebank);

        // Parse character data.
        String characterData =
                readConfigFile(pathToVoicebank.toPath().resolve("character.txt").toFile());
        for (String rawLine : characterData.split("\n")) {
            String line = rawLine.trim();
            if (line.startsWith("name=")) {
                name = line.substring("name=".length());
            } else if (line.startsWith("image=")) {
                imageName = line.substring("image=".length());
            }
        }

        // Parse all oto_ini.txt and oto.ini files in arbitrary order.
        try {
            Files.walkFileTree(
                    pathToVoicebank.toPath(),
                    EnumSet.of(FileVisitOption.FOLLOW_LINKS),
                    10,
                    new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path path, BasicFileAttributes attr) {
                            for (String otoName : ImmutableSet.of("oto.ini", "oto_ini.txt")) {
                                if (path.endsWith(otoName)) {
                                    Path pathToFile = path.toFile().getParentFile().toPath();
                                    parseOtoIni(pathToFile, otoName, lyricConfigs);
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
            // For some reason, "prefix.map" is a list of pitch suffixes.
            parsePitchMap(pathToVoicebank.toPath().resolve(pitchMapName).toFile(), pitchMap);
        }

        // Parse conversion set for romaji-hiragana-katakana conversion.
        DisjointLyricSet conversionSet = readLyricConversionsFromFile();

        return new Voicebank(
                pathToVoicebank,
                name,
                imageName,
                lyricConfigs,
                pitchMap,
                conversionSet);
    }

    private void parseOtoIni(
            Path pathToOtoFile,
            String otoFile,
            Map<String, LyricConfig> lyricConfigs) {
        String otoData = readConfigFile(pathToOtoFile.resolve(otoFile).toFile());
        for (String rawLine : otoData.split("\n")) {
            String line = rawLine.trim();
            Matcher matcher = LYRIC_PATTERN.matcher(line);
            if (matcher.find()) {
                String fileName = matcher.group(1); // Assuming this is a .wav file
                String lyricName = matcher.group(2);
                String[] configValues = line.substring(matcher.end()).split(",");
                if (configValues.length != 5 || fileName == null || lyricName == null) {
                    System.out.println("Received unexpected results while parsing oto.ini");
                    continue;
                }
                lyricConfigs.put(
                        lyricName,
                        new LyricConfig(
                                pathToOtoFile.resolve(fileName).toFile().getAbsolutePath(),
                                lyricName,
                                configValues));
            }
        }
    }

    private void parsePitchMap(File pitchMapFile, Map<String, String> pitchMap) {
        String pitchData = readConfigFile(pitchMapFile);
        for (String rawLine : pitchData.split("\n")) {
            String line = rawLine.trim();
            // TODO: Handle the case of prefixes rather than suffixes.
            Matcher matcher = PITCH_PATTERN.matcher(line);
            if (matcher.find()) {
                String pitch = matcher.group(1);
                String suffix = matcher.group(2);
                pitchMap.put(pitch, suffix);
            }
        }
    }

    /* Gets disjoint set used for romaji-hiragana-katakana conversions. */
    private DisjointLyricSet readLyricConversionsFromFile() {
        DisjointLyricSet conversionSet = new DisjointLyricSet();
        String conversionData = readConfigFile(lyricConversionPath);
        for (String line : conversionData.split("\n")) {
            conversionSet.addGroup(line.trim().split(","));
        }
        return conversionSet;
    }

    private String readConfigFile(File file) {
        if (!file.canRead()) {
            // This is often okay.
            return "";
        }
        try {
            String charset = "UTF-8";
            CharsetDecoder utf8Decoder =
                    Charset.forName("UTF-8").newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                            .onUnmappableCharacter(CodingErrorAction.REPORT);
            try {
                utf8Decoder.decode(ByteBuffer.wrap(FileUtils.readFileToByteArray(file)));
            } catch (MalformedInputException | UnmappableCharacterException e) {
                charset = "SJIS";
            }
            return FileUtils.readFileToString(file, charset);
        } catch (IOException e) {
            // TODO Handle this.
            errorLogger.logError(e);
        }
        return "";
    }

    /**
     * Parses a file path, and replaces the strings "${DEFAULT}" and "${HOME}" with their
     * corresponding directories.
     */
    public File parseFilePath(String line, String property) {
        String pathString = line.substring(property.length());
        pathString = pathString.replaceFirst("\\$\\{DEFAULT\\}", defaultVoicePath.getAbsolutePath())
                .replaceFirst("\\$\\{HOME\\}", System.getProperty("user.home"));
        return new File(pathString);
    }
}
