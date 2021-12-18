package com.utsusynth.utsu.model.song.converters.jp;

import com.google.common.base.CharMatcher;
import com.utsusynth.utsu.common.data.NoteContextData;
import com.utsusynth.utsu.common.data.NoteData;
import com.utsusynth.utsu.common.data.VoicebankData;
import com.utsusynth.utsu.common.enums.ReclistType;
import com.utsusynth.utsu.model.song.converters.ReclistConverter;
import com.utsusynth.utsu.model.voicebank.DisjointLyricSet;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class JpCvToJpVcvConverter implements ReclistConverter {
    @Override
    public List<NoteData> apply(List<NoteContextData> notes, VoicebankData voicebankData) {
        DisjointLyricSet.Reader conversionSet = voicebankData.getLyricConversions();
        return notes.stream().map(noteContext -> {
            NoteData note = noteContext.getNote();
            String prevLyric = noteContext.getPrev().map(NoteData::getLyric).orElse("");
            String prefix = guessPrefix(prevLyric, conversionSet).map(res -> res + " ").orElse("");
            return note.withNewLyric(prefix + note.getLyric());
        }).collect(Collectors.toList());
    }

    // Finds the vowel sound of previous lyric by converting to ASCII and taking the last character.
    private Optional<Character> guessPrefix(
            String prevLyric, DisjointLyricSet.Reader conversionSet) {
        if (prevLyric.isEmpty()) {
            return Optional.of('-'); // Return dash if there appears to be no previous note.
        }
        for (String converted : conversionSet.getGroup(prevLyric)) {
            if (CharMatcher.ascii().matchesAllOf(converted) && !converted.isEmpty()) {
                return Optional.of(converted.toLowerCase().charAt(converted.length() - 1));
            }
        }
        // No vowel found.
        return Optional.empty();
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
