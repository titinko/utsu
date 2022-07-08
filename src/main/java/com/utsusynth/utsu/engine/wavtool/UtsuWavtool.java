package com.utsusynth.utsu.engine.wavtool;

import com.google.inject.Inject;
import com.utsusynth.utsu.common.data.WavData;
import com.utsusynth.utsu.files.voicebank.SoundFileReader;
import com.utsusynth.utsu.model.song.Note;
import com.utsusynth.utsu.model.song.Song;

import java.io.File;
import java.util.Optional;

public class UtsuWavtool implements Wavtool {
    private final SoundFileReader soundFileReader;
    private double totalDelta = 0; // Total duration in ms, used to debug timing issues.

    @Inject
    public UtsuWavtool(SoundFileReader soundFileReader) {
        this.soundFileReader = soundFileReader;
    }

    @Override
    public void startRender(double startDelta) {
        totalDelta = startDelta;
    }

    @Override
    public void addNewNote(
            Song song,
            Note note,
            double noteLength,
            double expectedDelta,
            File inputFile,
            File outputFile,
            boolean includeOverlap,
            boolean triggerSynthesis) {
        Optional<WavData> wavData = soundFileReader.loadWavData(inputFile);
    }

    @Override
    public void addSilence(
            double duration,
            double expectedDelta,
            File inputFile,
            File outputFile,
            boolean triggerSynthesis) {

    }
}
