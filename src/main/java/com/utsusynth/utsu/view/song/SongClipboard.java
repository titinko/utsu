package com.utsusynth.utsu.view.song;

import java.util.List;
import com.google.common.collect.ImmutableList;
import com.utsusynth.utsu.common.data.NoteData;

/** Singleton clipboard used for all tabs containing songs. */
public class SongClipboard {
    private List<NoteData> cachedNotes;

    public SongClipboard() {
        cachedNotes = ImmutableList.of();
    }

    void setNotes(List<NoteData> cachedNotes) {
        // Don't replace a non-empty note list with an empty one.
        if (!cachedNotes.isEmpty()) {
            this.cachedNotes = cachedNotes;
        }
    }

    List<NoteData> getNotes() {
        return cachedNotes;
    }
}
