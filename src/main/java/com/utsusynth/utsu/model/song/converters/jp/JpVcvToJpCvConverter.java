package com.utsusynth.utsu.model.song.converters.jp;

import com.google.common.collect.ImmutableList;
import com.utsusynth.utsu.common.data.NoteContextData;
import com.utsusynth.utsu.common.data.NoteData;
import com.utsusynth.utsu.common.data.VoicebankData;
import com.utsusynth.utsu.common.enums.ReclistType;
import com.utsusynth.utsu.model.song.converters.ReclistConverter;
import com.utsusynth.utsu.model.voicebank.PresampConfig.AliasType;

import java.util.List;
import java.util.stream.Collectors;

public class JpVcvToJpCvConverter implements ReclistConverter {
    @Override
    public List<NoteData> apply(List<NoteContextData> notes, VoicebankData voicebankData) {
        return notes.stream().map(noteContext -> {
            NoteData note = noteContext.getNote();
            String vcvPad = voicebankData.getPresampConfig().parseAlias(AliasType.VCVPAD, " ");
            int index = note.getLyric().indexOf(vcvPad);
            if (index == -1) {
                return note;
            }
            String newLyric = note.getLyric().substring(index + vcvPad.length());
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
