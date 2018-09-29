package com.utsusynth.utsu.controller;

import java.util.List;
import com.utsusynth.utsu.common.data.NoteConfigData;

public interface NotePropertiesCallback {
    void updateNotes(List<NoteConfigData> oldData, List<NoteConfigData> newData);
}
