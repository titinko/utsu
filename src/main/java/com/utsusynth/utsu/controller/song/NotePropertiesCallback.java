package com.utsusynth.utsu.controller.song;

import java.util.List;
import com.utsusynth.utsu.common.data.NoteConfigData;

public interface NotePropertiesCallback {
    void updateNotes(List<NoteConfigData> oldData, List<NoteConfigData> newData);
}
