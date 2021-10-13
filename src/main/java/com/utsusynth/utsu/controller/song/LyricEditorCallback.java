package com.utsusynth.utsu.controller.song;

import com.utsusynth.utsu.common.RegionBounds;

public interface LyricEditorCallback {
    void insertLyrics(String[] newLyrics, RegionBounds regionToUpdate);

    void addPrefix(String prefixToAdd, RegionBounds regionToUpdate);

    void removePrefix(String prefixToRemove, RegionBounds regionToUpdate);

    void addSuffix(String suffixToAdd, RegionBounds regionToUpdate);

    void removeSuffix(String suffixToRemove, RegionBounds regionToUpdate);
}
