package com.utsusynth.utsu.controller.song;

import com.google.common.base.Function;
import com.utsusynth.utsu.common.utils.RegionBounds;
import com.utsusynth.utsu.common.data.NoteData;
import com.utsusynth.utsu.model.song.converters.ReclistConverter;

import java.util.List;

public interface LyricEditorCallback {
    void insertLyrics(String[] newLyrics, RegionBounds regionToUpdate);

    void transformLyric(Function<NoteData, NoteData> transform, RegionBounds regionToUpdate);

    void convertReclist(
            List<ReclistConverter> path, boolean usePresampIni, RegionBounds regionToUpdate);
}
