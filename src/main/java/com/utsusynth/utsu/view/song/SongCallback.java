package com.utsusynth.utsu.view.song;

import com.utsusynth.utsu.common.RegionBounds;
import com.utsusynth.utsu.common.data.MutateResponse;
import com.utsusynth.utsu.common.data.NoteData;
import com.utsusynth.utsu.common.exception.NoteAlreadyExistsException;
import com.utsusynth.utsu.controller.SongController.Mode;

/**
 * The view can use this interface to communicate with the model by way of the controller.
 */
public interface SongCallback {
    /** Add a note to the song. */
    MutateResponse addNote(NoteData toAdd) throws NoteAlreadyExistsException;

    /** Remove a note from the song. */
    MutateResponse removeNote(int position);

    /** Modify a note without changing its position or duration. */
    void modifyNote(NoteData toModify);

    /** Open the note properties editor on the given RegionBounds. */
    void openNoteProperties(RegionBounds regionBounds);

    /** Gets the current mode: ADD, EDIT, or DELETE. */
    Mode getCurrentMode();
}
