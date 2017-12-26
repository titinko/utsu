package com.utsusynth.utsu.view.note;

import com.google.common.base.Optional;
import com.utsusynth.utsu.UtsuController.Mode;
import com.utsusynth.utsu.common.data.EnvelopeData;
import com.utsusynth.utsu.common.data.NoteData;
import com.utsusynth.utsu.common.data.PitchbendData;
import com.utsusynth.utsu.common.exception.NoteAlreadyExistsException;

/**
 * A way of communicating TrackNote information back to its parent Track.
 */
public interface TrackNoteCallback {
    void setHighlighted(TrackNote note, boolean highlighted);

    boolean isHighlighted(TrackNote note);

    boolean isInBounds(int rowNum);

    String addSongNote(TrackNote note, NoteData toAdd) throws NoteAlreadyExistsException;

    void removeSongNote(int position);

    void modifySongVibrato(int position);

    void removeTrackNote(TrackNote trackNote);

    Optional<EnvelopeData> getEnvelope(int position);

    Optional<PitchbendData> getPortamento(int position);

    Mode getCurrentMode();
}
