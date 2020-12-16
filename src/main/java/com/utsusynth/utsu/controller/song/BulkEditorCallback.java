package com.utsusynth.utsu.controller.song;

import com.utsusynth.utsu.common.data.EnvelopeData;
import com.utsusynth.utsu.common.data.PitchbendData;
import com.utsusynth.utsu.controller.song.BulkEditorController.FilterType;

import java.util.List;

public interface BulkEditorCallback {
    void updatePortamento(PitchbendData newPortamento, List<FilterType> filters);

    void updateVibrato(PitchbendData newVibrato, List<FilterType> filters);

    void updateEnvelope(EnvelopeData newEnvelope, List<FilterType> filters);
}
