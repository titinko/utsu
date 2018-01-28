package com.utsusynth.utsu.view.song.note;

import com.google.common.base.Optional;
import com.utsusynth.utsu.common.data.EnvelopeData;
import com.utsusynth.utsu.common.data.NoteData;
import com.utsusynth.utsu.common.data.PitchbendData;
import com.utsusynth.utsu.common.exception.NoteAlreadyExistsException;
import com.utsusynth.utsu.controller.SongController.Mode;

/**
 * A way of communicating TrackNote information back to its parent Track.
 */
public interface NoteCallback {
    void highlightExclusive(Note note);

    void highlightInclusive(Note note);

    boolean isExclusivelyHighlighted(Note note);

    boolean isInBounds(int rowNum);

    void addSongNote(Note note, NoteData toAdd) throws NoteAlreadyExistsException;

    void removeSongNote(int position);

    void modifySongVibrato(int position);

    void removeTrackNote(Note trackNote);

    Optional<EnvelopeData> getEnvelope(int position);

    Optional<PitchbendData> getPortamento(int position);

    Mode getCurrentMode();
}
