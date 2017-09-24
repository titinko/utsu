package com.utsusynth.utsu.view;

import com.utsusynth.utsu.UtsuController.Mode;
import com.utsusynth.utsu.common.QuantizedAddRequest;
import com.utsusynth.utsu.common.QuantizedAddResponse;
import com.utsusynth.utsu.common.QuantizedNote;
import com.utsusynth.utsu.common.exception.NoteAlreadyExistsException;

/**
 * The view can use this interface to communicate with the model by way of the constructor.
 */
public interface ViewCallback {
	QuantizedAddResponse addNote(QuantizedAddRequest request) throws NoteAlreadyExistsException;

	QuantizedAddResponse removeNote(QuantizedNote toRemove);

	Mode getCurrentMode();
}
