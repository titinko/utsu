package com.utsusynth.utsu.view.song.note;

import com.utsusynth.utsu.common.data.NoteData;
import com.utsusynth.utsu.common.data.NoteUpdateData;
import com.utsusynth.utsu.common.exception.NoteAlreadyExistsException;
import com.utsusynth.utsu.controller.SongController.Mode;

/**
 * A way of communicating TrackNote information back to its parent Track.
 */
public interface NoteCallback {
    void highlightExclusive(Note note);

    void highlightInclusive(Note note);

    boolean isExclusivelyHighlighted(Note note);

    void addSongNote(Note note, NoteData toAdd) throws NoteAlreadyExistsException;

    NoteUpdateData removeSongNote(int position);

    void removeTrackNote(Note trackNote);

    /** Catches all highlighted notes, if present. */
    void deleteSongNote(Note note);

    /** Catches all highlighted notes, if present. */
    void deleteTrackNote(Note trackNote);

    boolean hasVibrato(int position);

    void setHasVibrato(int position, boolean hasVibrato);

    void openNoteProperties(Note note);

    Mode getCurrentMode();
}
