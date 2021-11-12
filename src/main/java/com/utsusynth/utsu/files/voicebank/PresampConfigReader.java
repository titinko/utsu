package com.utsusynth.utsu.files.voicebank;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.utsusynth.utsu.model.voicebank.PresampConfig;

import java.util.regex.Pattern;

/**
 * Reads a presamp.ini file for a voicebank.
 */
public class PresampConfigReader {
    private static final Pattern HEADER_PATTERN = Pattern.compile("\\[[A-Z0-9]+\\]");

    private final Provider<PresampConfig> presampConfigProvider;

    @Inject
    public PresampConfigReader(Provider<PresampConfig> presampConfigProvider) {
        this.presampConfigProvider = presampConfigProvider;
    }

    public PresampConfig loadPresampConfig(String fileContents) {
        String[] lines = fileContents.split("\n");
        PresampConfig.Builder presampBuilder = presampConfigProvider.get().toBuilder();
        int curLine = 0;
        while (curLine >= 0 && curLine < lines.length) {
            curLine = parseSection(lines, curLine, presampBuilder);
        }
        return new PresampConfig();
    }

    private int parseSection(String[] lines, int sectionStart, PresampConfig.Builder builder) {
        String header = lines[sectionStart].trim();
        if (!HEADER_PATTERN.matcher(header).matches()) {
            // Report parse section not called on section header warning.
            System.out.println("Parse header not called on section header.");
            return -1;
        }
        switch (header) {
            case "[VOWEL]":
                return parseVowel(lines, sectionStart + 1, builder);
            case "[CONSONANT]":
                return parseConsonant(lines, sectionStart + 1, builder);
            case "[PRIORITY]":
                return parsePriority(lines, sectionStart + 1, builder);
            case "[REPLACE]":
                return parseReplace(lines, sectionStart + 1, builder);
            case "[ALIAS]":
                return parseAlias(lines, sectionStart + 1, builder);
            case "[PRE]":
                //return parsePre(lines, sectionStart + 1, builder);
            case "[SU]":
                //return parseSu(lines, sectionStart + 1, builder);
            default:
                // Catch-all for all unknown or uninteresting headers.
                return skipSection(lines, sectionStart + 1);
        }
    }

    private int parseVowel(String[] lines, int sectionStart, PresampConfig.Builder builder) {
        for (int i = sectionStart; i < lines.length; i++) {
            String line = lines[i].trim();
            if (HEADER_PATTERN.matcher(line).matches()) {
                return i;
            }
            String[] splitLine = line.split("=");
            if (splitLine.length < 4) {
                printWarning("[VOWEL]");
                continue;
            }
            String vowel = splitLine[0];
            if (vowel.isEmpty()) {
                printWarning("[VOWEL]");
                continue;
            }
            String[] matchingSuffixes = splitLine[2].split(",");
            for (String suffix : matchingSuffixes) {
                if (!suffix.isEmpty()) {
                    builder.addVowelMapping(suffix, vowel);
                }
            }
            try {
                builder.addVowelVolume(vowel, Integer.valueOf(splitLine[3]));
            } catch (NumberFormatException e) {
                printWarning("[VOWEL]");
                builder.addVowelVolume(vowel, Integer.valueOf(splitLine[3]));
            }
        }
        return -1;
    }

    private int parseConsonant(String[] lines, int sectionStart, PresampConfig.Builder builder) {
        for (int i = sectionStart; i < lines.length; i++) {
            String line = lines[i].trim();
            if (HEADER_PATTERN.matcher(line).matches()) {
                return i;
            }
        }
        return -1;
    }

    private int parsePriority(String[] lines, int sectionStart, PresampConfig.Builder builder) {
        for (int i = sectionStart; i < lines.length; i++) {
            String line = lines[i].trim();
            if (HEADER_PATTERN.matcher(line).matches()) {
                return i;
            }
        }
        return -1;
    }

    private int parseReplace(String[] lines, int sectionStart, PresampConfig.Builder builder) {
        for (int i = sectionStart; i < lines.length; i++) {
            String line = lines[i].trim();
            if (HEADER_PATTERN.matcher(line).matches()) {
                return i;
            }
        }
        return -1;
    }

    private int parseAlias(String[] lines, int sectionStart, PresampConfig.Builder builder) {
        for (int i = sectionStart; i < lines.length; i++) {
            String line = lines[i].trim();
            if (HEADER_PATTERN.matcher(line).matches()) {
                return i;
            }
        }
        return -1;
    }

    private int skipSection(String[] lines, int sectionStart) {
        for (int i = sectionStart; i < lines.length; i++) {
            if (HEADER_PATTERN.matcher(lines[i].trim()).matches()) {
                return i;
            }
        }
        return -1;
    }

    private static void printWarning(String section) {
        System.out.println("Warning: Unexpected value in " + section + " section of presamp.ini.");
    }
}
