package com.utsusynth.utsu.view.song.note.envelope;

import com.utsusynth.utsu.common.data.EnvelopeData;

public interface EnvelopeCallback {
    void modifySongEnvelope(EnvelopeData oldData, EnvelopeData newData);
}
