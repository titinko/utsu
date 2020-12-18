package com.utsusynth.utsu.controller.song;

import com.utsusynth.utsu.common.RegionBounds;
import com.utsusynth.utsu.common.data.EnvelopeData;
import com.utsusynth.utsu.common.data.PitchbendData;
import com.utsusynth.utsu.common.enums.FilterType;

import java.util.List;

public interface BulkEditorCallback {
    void updatePortamento(
            PitchbendData newPortamento, RegionBounds regionToUpdate, List<FilterType> filters);

    void updateVibrato(
            PitchbendData newVibrato, RegionBounds regionToUpdate, List<FilterType> filters);

    void updateEnvelope(
            EnvelopeData newEnvelope, RegionBounds regionToUpdate, List<FilterType> filters);
}
