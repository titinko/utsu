package com.utsusynth.utsu.model.song.converters.jp;

import com.utsusynth.utsu.common.data.NoteContextData;
import com.utsusynth.utsu.common.data.NoteData;
import com.utsusynth.utsu.common.data.VoicebankData;
import com.utsusynth.utsu.common.enums.ReclistType;
import com.utsusynth.utsu.common.utils.LyricUtils;
import com.utsusynth.utsu.model.song.converters.ReclistConverter;
import com.utsusynth.utsu.model.voicebank.PresampConfig;
import com.utsusynth.utsu.model.voicebank.PresampConfig.AliasType;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class JpCvToJpVcvConverter implements ReclistConverter {
    @Override
    public List<NoteData> apply(List<NoteContextData> notes, VoicebankData voicebankData) {
        PresampConfig.Reader presampConfig = voicebankData.getPresampConfig();
        return notes.stream().map(noteContext -> {
            NoteData note = noteContext.getNote();
            // Get prefix, suffix, and stripped lyric.
            String prefix = LyricUtils.guessJpPrefix(note.getLyric(), voicebankData);
            String suffix = LyricUtils.guessJpSuffix(note.getLyric(), voicebankData);
            String strippedLyric = LyricUtils.stripPrefixSuffix(note.getLyric(), prefix, suffix);

            String prevLyric = noteContext.getPrev().map(NoteData::getLyric).orElse("");
            String newStrippedLyric = presampConfig.parseAlias(
                    AliasType.VCV,
                    /* backup= */ strippedLyric,
                    /* cValue= */Optional.empty(),
                    /* vValue= */guessPrefix(prevLyric, voicebankData),
                    /* cvValue= */ Optional.of(strippedLyric));
            return note.withNewLyric(prefix + newStrippedLyric + suffix);
        }).collect(Collectors.toList());
    }

    // Finds the vowel sound of previous lyric by converting to ASCII and taking the last character.
    private static Optional<String> guessPrefix(String prevLyric, VoicebankData voicebankData) {
        if (prevLyric.isEmpty()) {
            return Optional.of("-"); // Return dash if there appears to be no previous note.
        }
        String prevSuffix = LyricUtils.guessJpSuffix(prevLyric, voicebankData);
        String strippedPrevLyric = prevLyric.substring(0, prevLyric.length() - prevSuffix.length());
        return LyricUtils.guessJpVowel(strippedPrevLyric, voicebankData);
    }

    @Override
    public ReclistType getFrom() {
        return ReclistType.JP_CV;
    }

    @Override
    public ReclistType getTo() {
        return ReclistType.JP_VCV;
    }
}
