package com.utsusynth.utsu.files;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.MalformedInputException;
import java.nio.charset.UnmappableCharacterException;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.Iterator;
import com.google.common.collect.ImmutableSet;
import com.utsusynth.utsu.common.data.PitchMapData;
import com.utsusynth.utsu.common.exception.ErrorLogger;
import com.utsusynth.utsu.model.voicebank.LyricConfig;
import com.utsusynth.utsu.model.voicebank.LyricConfigMap;
import com.utsusynth.utsu.model.voicebank.Voicebank;

public class VoicebankWriter {
    private static final ErrorLogger errorLogger = ErrorLogger.getLogger();
    private static CharsetEncoder sjisEncoder =
            Charset.forName("SJIS").newEncoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT);

    public void writeVoicebankToDirectory(Voicebank voicebank, File saveDir) {
        // Save character.txt.
        File characterFile = saveDir.toPath().resolve("character.txt").toFile();
        String voiceData = voicebank.getName() + voicebank.getAuthor() + voicebank.getImagePath();
        try (PrintStream ps = new PrintStream(characterFile, getCharset(voiceData))) {
            ps.println("name=" + voicebank.getName());
            ps.println("author=" + voicebank.getAuthor());
            ps.println("image=" + voicebank.getImageName());
            ps.flush();
            ps.close();
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            // TODO: Handle this.
            errorLogger.logError(e);
        }

        // Save readme.txt.
        File readmeFile = saveDir.toPath().resolve("readme.txt").toFile();
        String description = voicebank.getDescription();
        try (PrintStream ps = new PrintStream(readmeFile, getCharset(description))) {
            ps.print(description);
            ps.flush();
            ps.close();
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            // TODO: Handle this.
            errorLogger.logError(e);
        }

        // Save lyric configs. Include blank file in main directory if necessary.
        File utfOtoFile = saveDir.toPath().resolve("oto_ini.txt").toFile();
        try (PrintStream utfPs = new PrintStream(utfOtoFile, "UTF-8")) {
            utfPs.println("#Charset:UTF-8");
            ImmutableSet<String> categories = ImmutableSet.<String>builder()
                    .addAll(voicebank.getCategories()).add(LyricConfigMap.MAIN_CATEGORY).build();
            for (String category : categories) {
                // For now, always use foldered oto structure for oto.ini.
                Path categoryDir = category.equals(LyricConfigMap.MAIN_CATEGORY) ? saveDir.toPath()
                        : saveDir.toPath().resolve(category);
                File otoFile = categoryDir.resolve("oto.ini").toFile();
                PrintStream ps = new PrintStream(otoFile, "SJIS");
                Iterator<LyricConfig> iterator = voicebank.getLyricConfigs(category);
                while (iterator.hasNext()) {
                    LyricConfig config = iterator.next();
                    if (config == null) {
                        continue;
                    }
                    for (PrintStream stream : ImmutableSet.of(utfPs, ps)) {
                        String charset = getCharset(config.getFilename() + config.getTrueLyric());
                        if (!charset.equals("SJIS") && stream.equals(ps)) {
                            continue;
                        }
                        if (stream.equals(utfPs)) {
                            stream.print(config.getFilename() + "=");
                        } else {
                            stream.print(config.getPathToFile().getName() + "=");
                        }
                        stream.print(config.getTrueLyric() + ",");
                        stream.print(roundDecimal(config.getOffset(), "#.#") + ",");
                        stream.print(roundDecimal(config.getConsonant(), "#.#") + ",");
                        stream.print(roundDecimal(config.getCutoff(), "#.#") + ",");
                        stream.print(roundDecimal(config.getPreutterance(), "#.#") + ",");
                        stream.print(roundDecimal(config.getOverlap(), "#.#") + "\n");
                    }
                    ps.flush();
                    ps.close();
                }
            }
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            // TODO: Handle this.
            errorLogger.logError(e);
        }

        // Save pitch map.
        for (String prefixmapName : ImmutableSet.of("prefixmap", "prefix.map")) {
            File pitchFile = saveDir.toPath().resolve(prefixmapName).toFile();
            String charset = prefixmapName.equals("prefix.map") ? "SJIS" : "UTF-8";
            try (PrintStream ps = new PrintStream(pitchFile, charset)) {
                if (charset.equals("UTF-8")) {
                    ps.println("#Charset:UTF-8");
                }
                Iterator<PitchMapData> iterator = voicebank.getPitchData();
                while (iterator.hasNext()) {
                    PitchMapData data = iterator.next();
                    if (data == null) {
                        continue;
                    }
                    ps.print(data.getPitch() + "\t\t" + data.getSuffix() + "\n");
                    ps.flush();
                    ps.close();
                }
            } catch (FileNotFoundException | UnsupportedEncodingException e) {
                // TODO: Handle this.
                errorLogger.logError(e);
            }
        }
    }

    private String getCharset(String toRender) {
        // Default to Shift-JIS unless there are Unicode-only characters.
        String charset = "SJIS";
        try {
            sjisEncoder.encode(CharBuffer.wrap(toRender.toCharArray()));
        } catch (MalformedInputException | UnmappableCharacterException e) {
            charset = "UTF-8";
        } catch (IOException e) {
            // TODO: Handle this.
            errorLogger.logError(e);
        }
        return charset;
    }

    private String roundDecimal(double number, String roundFormat) {
        int formatNumPlaces = roundFormat.length() - roundFormat.indexOf(".") - 1;
        String formatted = new DecimalFormat(roundFormat).format(number);
        if (formatted.contains(".")) {
            int numPlaces = formatted.length() - formatted.indexOf(".") - 1;
            for (int i = numPlaces; i < formatNumPlaces; i++) {
                formatted = formatted + "0";
            }
        } else {
            formatted = formatted + ".";
            for (int i = 0; i < formatNumPlaces; i++) {
                formatted = formatted + "0";
            }
        }
        return formatted;
    }
}
