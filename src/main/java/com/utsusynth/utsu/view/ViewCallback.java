package com.utsusynth.utsu.view;

import com.utsusynth.utsu.UtsuController.Mode;
import com.utsusynth.utsu.common.exception.NoteAlreadyExistsException;
import com.utsusynth.utsu.common.quantize.QuantizedAddRequest;
import com.utsusynth.utsu.common.quantize.QuantizedAddResponse;
import com.utsusynth.utsu.common.quantize.QuantizedNote;

/**
 * The view can use this interface to communicate with the model by way of the constructor.
 */
public interface ViewCallback {
	QuantizedAddResponse addNote(QuantizedAddRequest request) throws NoteAlreadyExistsException;

	QuantizedAddResponse removeNote(QuantizedNote toRemove);

	Mode getCurrentMode();
}
