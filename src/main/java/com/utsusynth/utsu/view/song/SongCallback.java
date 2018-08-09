package com.utsusynth.utsu.view.song;

import com.utsusynth.utsu.common.RegionBounds;
import com.utsusynth.utsu.common.data.AddResponse;
import com.utsusynth.utsu.common.data.NoteData;
import com.utsusynth.utsu.common.data.RemoveResponse;
import com.utsusynth.utsu.common.exception.NoteAlreadyExistsException;
import com.utsusynth.utsu.controller.SongController.Mode;

/**
 * The view can use this interface to communicate with the model by way of the controller.
 */
public interface SongCallback {
    /** Add a note to the song. */
    AddResponse addNote(NoteData toAdd) throws NoteAlreadyExistsException;

    /** Remove a note from the song. */
    RemoveResponse removeNote(int position);

    /** Modify a note without changing its position or duration. */
    void modifyNote(NoteData toModify);

    /** Open the note properties editor on the given RegionBounds. */
    void openNoteProperties(RegionBounds regionBounds);

    /** Gets the current mode: ADD, EDIT, or DELETE. */
    Mode getCurrentMode();

    /**
     * When measures are added/removed and the track width changes, call this method to make sure
     * that the user is scrolled to the same spot as before.
     */
    void adjustScrollbar(double oldWidth, double newWidth);
}
