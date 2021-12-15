package com.utsusynth.utsu.model.song.converters.jp;

import com.utsusynth.utsu.common.data.NoteContextData;
import com.utsusynth.utsu.common.data.NoteData;
import com.utsusynth.utsu.common.enums.ReclistType;
import com.utsusynth.utsu.model.song.converters.ReclistConverter;

import java.util.List;

public class JpCvToJpVcvConverter implements ReclistConverter {
    @Override
    public List<NoteData> apply(List<NoteContextData> notes) {
        return null;
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
