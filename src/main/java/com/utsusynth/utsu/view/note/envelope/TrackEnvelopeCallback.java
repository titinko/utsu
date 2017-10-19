package com.utsusynth.utsu.view.note.envelope;

import com.utsusynth.utsu.common.quantize.QuantizedEnvelope;

public interface TrackEnvelopeCallback {
	void modifySongEnvelope(QuantizedEnvelope envelope);
}
