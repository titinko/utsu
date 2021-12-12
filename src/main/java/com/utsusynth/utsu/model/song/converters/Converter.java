package com.utsusynth.utsu.model.song.converters;

import com.utsusynth.utsu.common.data.NoteData;

import java.util.List;

public interface Converter {
    /**
     * Starts with a list of notes, returns a list of notes that should replace it. The start and
     * end bounds of the output notes will usually be the same as the input notes, but that is not
     * guaranteed and some implementations may change the bounds.
     */
    List<NoteData> apply(List<NoteData> notes);
}
