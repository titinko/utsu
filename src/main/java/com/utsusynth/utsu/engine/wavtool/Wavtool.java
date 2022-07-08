package com.utsusynth.utsu.engine.wavtool;

import com.utsusynth.utsu.model.song.Note;
import com.utsusynth.utsu.model.song.Song;

import java.io.File;

public interface Wavtool {
    /* Called when starting a new render. */
    void startRender(double startDelta);

    /* Add a note to the final file. */
    void addNewNote(
            Song song,
            Note note,
            double noteLength,
            double expectedDelta,
            File inputFile,
            File outputFile,
            boolean includeOverlap,
            boolean triggerSynthesis);

    /* Add a silence to the final file. */
    void addSilence(
            double duration,
            double expectedDelta,
            File inputFile,
            File outputFile,
            boolean triggerSynthesis);
}
