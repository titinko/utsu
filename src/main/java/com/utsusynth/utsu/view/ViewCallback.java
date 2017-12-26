package com.utsusynth.utsu.view;

import com.utsusynth.utsu.UtsuController.Mode;
import com.utsusynth.utsu.common.data.AddResponse;
import com.utsusynth.utsu.common.data.NoteData;
import com.utsusynth.utsu.common.data.RemoveResponse;
import com.utsusynth.utsu.common.exception.NoteAlreadyExistsException;

/**
 * The view can use this interface to communicate with the model by way of the constructor.
 */
public interface ViewCallback {
    AddResponse addNote(NoteData toAdd) throws NoteAlreadyExistsException;

    RemoveResponse removeNote(int position);

    void modifyNote(NoteData toModify);

    Mode getCurrentMode();
}
