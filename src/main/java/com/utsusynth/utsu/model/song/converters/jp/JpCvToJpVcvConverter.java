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
            String prevLyric = noteContext.getPrev().map(NoteData::getLyric).orElse("");
            String newLyric = presampConfig.parseAlias(
                    AliasType.VCV,
                    /* backup= */ note.getLyric(),
                    /* cValue= */Optional.empty(),
                    /* vValue= */guessPrefix(prevLyric, voicebankData),
                    /* cvValue= */ Optional.of(note.getLyric()));
            return note.withNewLyric(newLyric);
        }).collect(Collectors.toList());
    }

    // Finds the vowel sound of previous lyric by converting to ASCII and taking the last character.
    private static Optional<String> guessPrefix(String prevLyric, VoicebankData voicebankData) {
        if (prevLyric.isEmpty()) {
            return Optional.of("-"); // Return dash if there appears to be no previous note.
        }
        return LyricUtils.guessJpVowel(prevLyric, voicebankData);
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
