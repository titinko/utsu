package com.utsusynth.utsu.controller.song;

import com.google.common.base.Function;
import com.utsusynth.utsu.common.utils.RegionBounds;
import com.utsusynth.utsu.common.data.NoteData;

public interface LyricEditorCallback {
    void insertLyrics(String[] newLyrics, RegionBounds regionToUpdate);

    void transformLyric(Function<NoteData, NoteData> transform, RegionBounds regionToUpdte);
}
