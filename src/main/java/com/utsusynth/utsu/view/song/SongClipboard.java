package com.utsusynth.utsu.view.song;

import java.util.List;
import com.google.common.collect.ImmutableList;
import com.utsusynth.utsu.common.data.NoteData;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

/** Singleton clipboard used for all tabs containing songs. */
public class SongClipboard {
    private final BooleanProperty isAnythingCached;

    private List<NoteData> cachedNotes;

    public SongClipboard() {
        isAnythingCached = new SimpleBooleanProperty(false);
        cachedNotes = ImmutableList.of();
    }

    void setNotes(List<NoteData> cachedNotes) {
        // Don't replace a non-empty note list with an empty one.
        if (!cachedNotes.isEmpty()) {
            this.cachedNotes = cachedNotes;
            isAnythingCached.set(true);
        }
    }

    List<NoteData> getNotes() {
        return cachedNotes;
    }

    BooleanProperty clipboardFilledProperty() {
        return isAnythingCached;
    }
}
