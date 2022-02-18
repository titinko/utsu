package com.utsusynth.utsu.model.song.converters.jp;

import com.utsusynth.utsu.common.data.NoteContextData;
import com.utsusynth.utsu.common.data.NoteData;
import com.utsusynth.utsu.common.data.VoicebankData;
import com.utsusynth.utsu.common.enums.ReclistType;
import com.utsusynth.utsu.common.utils.LyricUtils;
import com.utsusynth.utsu.model.song.converters.ReclistConverter;
import com.utsusynth.utsu.model.voicebank.DisjointLyricSet;
import com.utsusynth.utsu.model.voicebank.PresampConfig;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class JpCvToJpVcvConverter implements ReclistConverter {
    @Override
    public List<NoteData> apply(List<NoteContextData> notes, VoicebankData voicebankData) {
        DisjointLyricSet.Reader conversionSet = voicebankData.getLyricConversions();
        PresampConfig.Reader presampConfig = voicebankData.getPresampConfig();
        return notes.stream().map(noteContext -> {
            NoteData note = noteContext.getNote();
            String prevLyric = noteContext.getPrev().map(NoteData::getLyric).orElse("");
            String prefix = guessPrefix(prevLyric, voicebankData).map(res -> res + " ").orElse("");
            return note.withNewLyric(prefix + note.getLyric());
        }).collect(Collectors.toList());
    }

    // Finds the vowel sound of previous lyric by converting to ASCII and taking the last character.
    private static Optional<Character> guessPrefix(String prevLyric, VoicebankData voicebankData) {
        if (prevLyric.isEmpty()) {
            return Optional.of('-'); // Return dash if there appears to be no previous note.
        }
        String vowel = LyricUtils.guessJpVowel(prevLyric, voicebankData);
        return vowel.isEmpty() ? Optional.empty() : Optional.of(vowel.charAt(0));

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
