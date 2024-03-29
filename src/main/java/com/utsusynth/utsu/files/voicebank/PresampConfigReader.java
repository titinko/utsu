package com.utsusynth.utsu.files.voicebank;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.utsusynth.utsu.common.utils.PresampConfigUtils;
import com.utsusynth.utsu.model.voicebank.PresampConfig;
import com.utsusynth.utsu.model.voicebank.PresampConfig.AliasType;
import com.utsusynth.utsu.model.voicebank.PresampConfig.EndFlag;
import com.utsusynth.utsu.model.voicebank.PresampConfig.SuffixType;
import com.utsusynth.utsu.model.voicebank.PresampConfig.VcLength;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads a presamp.ini file for a voicebank.
 */
public class PresampConfigReader {
    private static final Pattern HEADER_PATTERN = Pattern.compile("\\[[A-Z0-9]+\\]");

    public PresampConfig loadPresampConfig(PresampConfig.Builder builder, String fileContents) {
        String[] lines = fileContents.split("\n");
        int curLine = 0;
        while (curLine >= 0 && curLine < lines.length) {
            curLine = parseSection(lines, curLine, builder);
        }
        return builder.build();
    }

    private int parseSection(String[] lines, int sectionStart, PresampConfig.Builder builder) {
        String header = lines[sectionStart].trim();
        if (header.isEmpty()) {
            // This is sometimes expected.
            return sectionStart + 1;
        } else if (!HEADER_PATTERN.matcher(header).matches()) {
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
                return parsePre(lines, sectionStart + 1, builder);
            case "[SU]":
                return parseSu(lines, sectionStart + 1, builder);
            case "[NUM]":
                return parseSuffixList(lines, sectionStart + 1, builder, SuffixType.NUM);
            case "[APPEND]":
                return parseSuffixList(lines, sectionStart + 1, builder, SuffixType.APPEND);
            case "[PITCH]":
                return parseSuffixList(lines, sectionStart + 1, builder, SuffixType.PITCH);
            case "[ALIAS_PRIORITY]":
                return parseAliasPriority(lines, sectionStart + 1, builder);
            case "[ALIAS_PRIORITY_DIFAPPEND]":
                return parseAliasPriorityDifappend(lines, sectionStart + 1, builder);
            case "[ALIAS_PRIORITY_DIFPITCH]":
                return parseAliasPriorityDifpitch(lines, sectionStart + 1, builder);
            case "[SPLIT]":
                return parseSplit(lines, sectionStart + 1, builder);
            case "[MUSTVC]":
                return parseMustVc(lines, sectionStart + 1, builder);
            case "[CFLAGS]":
                return parseCFlags(lines, sectionStart + 1, builder);
            case "[VCLENGTH]":
                return parseVcLength(lines, sectionStart + 1, builder);
            case "[ENDTYPE1]":
            case "[ENDTYPE]":
                return parseAlias(lines, sectionStart + 1, builder, AliasType.ENDING_1);
            case "[ENDTYPE2]":
                return parseAlias(lines, sectionStart + 1, builder, AliasType.ENDING_2);
            case "[VCPAD]":
                return parseAlias(lines, sectionStart + 1, builder, AliasType.VCPAD);
            case "[ENDFLAG]":
                return parseEndFlag(lines, sectionStart + 1, builder);
            default:
                // Catch-all for all unknown or uninteresting headers.
                return skipSection(lines, sectionStart + 1);
        }
    }

    private int parseVowel(String[] lines, int sectionStart, PresampConfig.Builder builder) {
        builder.clearVowelMappings();
        builder.clearVowelVolumes();

        for (int i = sectionStart; i < lines.length; i++) {
            String line = lines[i].trim();
            if (HEADER_PATTERN.matcher(line).matches()) {
                return i;
            }
            String[] splitLine = line.split("=");
            if (splitLine.length < 4) {
                printWarning("[VOWEL]", line);
                continue;
            }
            String vowel = splitLine[0];
            if (vowel.isEmpty()) {
                printWarning("[VOWEL]", line);
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
                printWarning("[VOWEL]", line);
                builder.addVowelVolume(vowel, Integer.valueOf(splitLine[3]));
            }
        }
        return -1;
    }

    private int parseConsonant(String[] lines, int sectionStart, PresampConfig.Builder builder) {
        builder.clearConsonantMappings();
        builder.clearConsonantOverlaps();
        builder.clearVcLengthOverrides();

        for (int i = sectionStart; i < lines.length; i++) {
            String line = lines[i].trim();
            if (HEADER_PATTERN.matcher(line).matches()) {
                return i;
            }
            String[] splitLine = line.split("=");
            if (splitLine.length < 3) {
                printWarning("[CONSONANT]", line);
                continue;
            }
            String consonant = splitLine[0];
            if (consonant.isEmpty()) {
                printWarning("[CONSONANT]", line);
                continue;
            }
            String[] matchingLyrics = splitLine[1].split(",");
            for (String lyric : matchingLyrics) {
                if (!lyric.isEmpty()) {
                    builder.addConsonantMapping(lyric, consonant);
                }
            }
            builder.addConsonantOverlap(consonant, splitLine[2].equals("1"));
            if (splitLine.length > 3) {
                builder.addVcLengthOverride(consonant,
                        splitLine[3].equals("1") ? VcLength.OTO : VcLength.PREUTTERANCE);
            }
        }
        return -1;
    }

    private int parsePriority(String[] lines, int sectionStart, PresampConfig.Builder builder) {
        builder.clearNeverVcv();

        for (int i = sectionStart; i < lines.length; i++) {
            String line = lines[i].trim();
            if (HEADER_PATTERN.matcher(line).matches()) {
                return i;
            }
            for (String lyric: line.split(",")) {
                if (!lyric.isEmpty()) {
                    builder.addNeverVcv(lyric);
                }
            }
        }
        return -1;
    }

    private int parseReplace(String[] lines, int sectionStart, PresampConfig.Builder builder) {
        builder.clearLyricConversions();

        for (int i = sectionStart; i < lines.length; i++) {
            String line = lines[i].trim();
            if (HEADER_PATTERN.matcher(line).matches()) {
                return i;
            }
            String[] splitLine = line.split("=");
            if (splitLine.length != 2 || splitLine[0].isEmpty() || splitLine[1].isEmpty()) {
                printWarning("[REPLACE]", line);
                continue;
            }
            builder.addLyricConversionSet(ImmutableSet.copyOf(splitLine));
        }
        return -1;
    }

    private int parseAlias(String[] lines, int sectionStart, PresampConfig.Builder builder) {
        builder.clearAliasFormats();

        for (int i = sectionStart; i < lines.length; i++) {
            if (HEADER_PATTERN.matcher(lines[i].trim()).matches()) {
                return i;
            }
            String[] splitLine = lines[i].split("=");
            if (splitLine.length != 2 || splitLine[1].isEmpty()) {
                continue;
            }
            Optional<AliasType> aliasType = PresampConfigUtils.getAliasType(splitLine[0]);
            if (aliasType.isEmpty()) {
                printWarning("[ALIAS]", splitLine[0]);
            } else {
                builder.setAliasFormat(
                        aliasType.get(), ImmutableList.copyOf(splitLine[1].split(",")));
            }
        }
        return -1;
    }

    private int parseAlias(
            String[] lines, int sectionStart, PresampConfig.Builder builder, AliasType aliasType) {
        for (int i = sectionStart; i < lines.length; i++) {
            if (HEADER_PATTERN.matcher(lines[i].trim()).matches()) {
                return i;
            }
            if (!lines[i].isEmpty()) {
                builder.setAliasFormat(aliasType, ImmutableList.copyOf(lines[i].split(",")));
            }

        }
        return -1;
    }

    private int parsePre(String[] lines, int sectionStart, PresampConfig.Builder builder) {
        builder.clearPrefixes();

        for (int i = sectionStart; i < lines.length; i++) {
            if (HEADER_PATTERN.matcher(lines[i].trim()).matches()) {
                return i;
            } else if (!lines[i].isEmpty()) {
                builder.addPrefix(lines[i]);
            }
        }
        return -1;
    }

    private int parseSu(String[] lines, int sectionStart, PresampConfig.Builder builder) {
        for (int i = sectionStart; i < lines.length; i++) {
            String line = lines[i].trim();
            if (HEADER_PATTERN.matcher(line).matches()) {
                return i;
            } else if (!line.isEmpty()) {
                Matcher matcher = PresampConfigUtils.SUFFIX_TYPE_PATTERN.matcher(line);
                ImmutableList.Builder<SuffixType> suffixOrderBuilder = ImmutableList.builder();
                while(matcher.find()) {
                    Optional<SuffixType> suffixType =
                            PresampConfigUtils.getSuffixType(matcher.group(1));
                    suffixType.ifPresent(suffixOrderBuilder::add);
                }
                builder.setSuffixOrder(suffixOrderBuilder.build());
            }
        }
        return -1;
    }

    private int parseSuffixList(
            String[] lines,
            int sectionStart,
            PresampConfig.Builder builder,
            SuffixType suffixType) {
        builder.clearAllowUnderbarSuffixes();
        builder.clearExcludeRepeatsSuffixes();
        builder.clearSuffixes();

        for (int i = sectionStart; i < lines.length; i++) {
            if (HEADER_PATTERN.matcher(lines[i].trim()).matches()) {
                return i;
            }
            String line = lines[i];
            if (line.equals("@UNDERBAR@")) {
                builder.addAllowUnderbarSuffix(suffixType);
            } else if (line.equals("@NOREPEAT")) {
                builder.addExcludeRepeatsSuffix(suffixType);
            } else if (!line.isEmpty()) {
                builder.addSuffix(suffixType, line);
            }
        }
        return -1;
    }

    private int parseAliasPriority(
            String[] lines, int sectionStart, PresampConfig.Builder builder) {
        ImmutableList.Builder<AliasType> priorityBuilder = ImmutableList.builder();
        int nextSectionStart = -1;
        for (int i = sectionStart; i < lines.length; i++) {
            String line = lines[i].trim();
            if (HEADER_PATTERN.matcher(line).matches()) {
                nextSectionStart = i;
                break;
            }
            Optional<AliasType> aliasType = PresampConfigUtils.getAliasType(line);
            if (aliasType.isEmpty()) {
                printWarning("[ALIAS_PRIORITY]", line);
            } else {
                priorityBuilder.add(aliasType.get());
            }
        }
        ImmutableList<AliasType> priorityList = priorityBuilder.build();
        if (!priorityList.isEmpty()) {
            builder.setAliasPriority(priorityList);
        }
        return nextSectionStart;
    }

    private int parseAliasPriorityDifappend(
            String[] lines, int sectionStart, PresampConfig.Builder builder) {
        ImmutableList.Builder<AliasType> priorityBuilder = ImmutableList.builder();
        int nextSectionStart = -1;
        for (int i = sectionStart; i < lines.length; i++) {
            String line = lines[i].trim();
            if (HEADER_PATTERN.matcher(line).matches()) {
                nextSectionStart = i;
                break;
            }
            Optional<AliasType> aliasType = PresampConfigUtils.getAliasType(line);
            if (aliasType.isEmpty()) {
                printWarning("[ALIAS_PRIORITY_DIFAPPEND]", line);
            } else {
                priorityBuilder.add(aliasType.get());
            }
        }
        ImmutableList<AliasType> priorityList = priorityBuilder.build();
        if (!priorityList.isEmpty()) {
            builder.setAliasPriorityDifappend(priorityList);
        }
        return nextSectionStart;
    }

    private int parseAliasPriorityDifpitch(
            String[] lines, int sectionStart, PresampConfig.Builder builder) {
        ImmutableList.Builder<AliasType> priorityBuilder = ImmutableList.builder();
        int nextSectionStart = -1;
        for (int i = sectionStart; i < lines.length; i++) {
            String line = lines[i].trim();
            if (HEADER_PATTERN.matcher(line).matches()) {
                nextSectionStart = i;
                break;
            }
            Optional<AliasType> aliasType = PresampConfigUtils.getAliasType(line);
            if (aliasType.isEmpty()) {
                printWarning("[ALIAS_PRIORITY_DIFAPPEND]", line);
            } else {
                priorityBuilder.add(aliasType.get());
            }
        }
        ImmutableList<AliasType> priorityList = priorityBuilder.build();
        if (!priorityList.isEmpty()) {
            builder.setAliasPriorityDifpitch(priorityList);
        }
        return nextSectionStart;
    }

    private int parseSplit(String[] lines, int sectionStart, PresampConfig.Builder builder) {
        for (int i = sectionStart; i < lines.length; i++) {
            String line = lines[i].trim();
            if (HEADER_PATTERN.matcher(line).matches()) {
                return i;
            }
            builder.setEnableSplitting(line.equals("1"));
        }
        return -1;
    }

    private int parseMustVc(String[] lines, int sectionStart, PresampConfig.Builder builder) {
        for (int i = sectionStart; i < lines.length; i++) {
            String line = lines[i].trim();
            if (HEADER_PATTERN.matcher(line).matches()) {
                return i;
            }
            builder.setMustVC(line.equals("1"));
        }
        return -1;
    }

    private int parseCFlags(String[] lines, int sectionStart, PresampConfig.Builder builder) {
        for (int i = sectionStart; i < lines.length; i++) {
            String line = lines[i].trim();
            if (HEADER_PATTERN.matcher(line).matches()) {
                return i;
            } else if (!line.isEmpty()) {
                builder.setConsonantFlags(line);
            }
        }
        return -1;
    }

    private int parseVcLength(String[] lines, int sectionStart, PresampConfig.Builder builder) {
        for (int i = sectionStart; i < lines.length; i++) {
            String line = lines[i].trim();
            if (HEADER_PATTERN.matcher(line).matches()) {
                return i;
            }
            builder.setVcLength(line.equals("1") ? VcLength.OTO : VcLength.PREUTTERANCE);
        }
        return -1;
    }

    private int parseEndFlag(String[] lines, int sectionStart, PresampConfig.Builder builder) {
        for (int i = sectionStart; i < lines.length; i++) {
            String line = lines[i].trim();
            if (HEADER_PATTERN.matcher(line).matches()) {
                return i;
            }
            switch (line) {
                case "0":
                    builder.setEndFlag(EndFlag.NO_AUTOMATIC_ENDINGS);
                    break;
                case "1":
                    builder.setEndFlag(EndFlag.USE_ENDING_1);
                    break;
                case "2":
                    builder.setEndFlag(EndFlag.USE_ENDING_2);
                    break;
                case "3":
                    builder.setEndFlag(EndFlag.USE_BOTH_ENDINGS);
                    break;
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

    private static void printWarning(String section, String context) {
        System.out.println(
                "Warning: Unexpected value in " + section + " section of presamp.ini:" + context);
    }
}
