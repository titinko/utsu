package com.utsusynth.utsu.model.song.converters.jp;

import com.google.common.collect.ImmutableList;
import com.utsusynth.utsu.common.data.NoteContextData;
import com.utsusynth.utsu.common.data.NoteData;
import com.utsusynth.utsu.common.data.VoicebankData;
import com.utsusynth.utsu.common.enums.ReclistType;
import com.utsusynth.utsu.model.song.converters.ReclistConverter;

import java.util.List;

public class JpCvToJpCvvcConverter implements ReclistConverter {
    @Override
    public List<NoteData> apply(List<NoteContextData> notes, VoicebankData voicebankData) {
        ImmutableList.Builder<NoteData> output = ImmutableList.builder();
        for (NoteContextData noteContextData : notes) {
            if (noteContextData.getNext().isEmpty()) {
                // Try adding an end note.
                // NoteData endNote = new NoteData(1, 1, note.getPitch(), "");
                // output.add(endNote);

            }
            NoteData note = noteContextData.getNote();
            if (noteContextData.getNext().isPresent()) {
                int nextDuration = noteContextData.getNext().get().getDuration();
                output.add(note.withDuration(note.getDuration() + nextDuration));
                continue; // Lengthen note if it appears to be followed by a VC.
            }
            output.add(noteContextData.getNote());
        }
        return output.build();
    }

    @Override
    public ReclistType getFrom() {
        return ReclistType.JP_CV;
    }

    @Override
    public ReclistType getTo() {
        return ReclistType.JP_CVVC;
    }
}
