package com.utsusynth.utsu.model.song.converters.jp;

import com.utsusynth.utsu.common.data.NoteContextData;
import com.utsusynth.utsu.common.data.NoteData;
import com.utsusynth.utsu.common.data.VoicebankData;
import com.utsusynth.utsu.common.enums.ReclistType;
import com.utsusynth.utsu.model.song.converters.ReclistConverter;

import java.util.List;
import java.util.stream.Collectors;

public class JpVcvToJpCvConverter implements ReclistConverter {
    @Override
    public List<NoteData> apply(List<NoteContextData> notes, VoicebankData voicebankData) {
        return notes.stream().map(noteContext -> {
            NoteData note = noteContext.getNote();
            int index = note.getLyric().indexOf(" ");
            if (index == -1) {
                return note;
            }
            String newLyric = note.getLyric().substring(index + 1);
            if (newLyric.isEmpty()) {
                return note;
            }
            return note.withNewLyric(newLyric);
        }).collect(Collectors.toList());
    }

    @Override
    public ReclistType getFrom() {
        return ReclistType.JP_VCV;
    }

    @Override
    public ReclistType getTo() {
        return ReclistType.JP_CV;
    }
}
