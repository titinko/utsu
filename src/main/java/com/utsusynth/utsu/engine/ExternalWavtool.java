package com.utsusynth.utsu.engine;

import java.io.File;
import com.google.inject.Inject;
import com.utsusynth.utsu.engine.wavtool.Wavtool;
import com.utsusynth.utsu.files.PreferencesManager;
import com.utsusynth.utsu.model.song.Song;
import com.utsusynth.utsu.model.song.Note;

public class ExternalWavtool implements Wavtool {
    private final ExternalProcessRunner runner;
    private final PreferencesManager preferencesManager;
    private File wavtoolPath;
    private double totalDelta = 0; // Total duration in ms, used to debug timing issues.

    @Inject
    ExternalWavtool(ExternalProcessRunner runner, PreferencesManager preferencesManager) {
        this.runner = runner;
        this.preferencesManager = preferencesManager;
        wavtoolPath = preferencesManager.getWavtool();
    }

    File getWavtoolPath() {
        if (wavtoolPath != null) {
            return wavtoolPath;
        }
        return preferencesManager.getWavtool();
    }

    void setWavtoolPath(File wavtoolPath) {
        this.wavtoolPath = wavtoolPath;
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
        String outputFilePath = outputFile.getAbsolutePath();
        String inputFilePath = inputFile.getAbsolutePath();
        String[] envelope = note.getFullEnvelope();

        double boundedOverlap = Math.max(0, Math.min(note.getFadeIn(), noteLength));
        // Ignore overlap if current note doesn't touch previous one.
        if (!includeOverlap) {
            boundedOverlap = 0;
        }

        double scaleFactor = 125 / song.getTempo();
        double scaledExpectedDelta = expectedDelta * scaleFactor;

        // Check that current length matches expected length and correct any discrepancies.
        if (scaledExpectedDelta > totalDelta && Math.abs(scaledExpectedDelta - totalDelta) > 0.01) {
            double timingCorrection = scaledExpectedDelta - totalDelta;
            if (boundedOverlap > timingCorrection) {
                // Disable for now.
                // boundedOverlap -= timingCorrection;
                System.out.println("Corrected note timing by " + timingCorrection + " ms.");
            }
        }

        // Call wavtool to add new note onto the end of the output file.
        runner.runProcess(
                getWavtoolPath().getAbsolutePath(),
                outputFilePath,
                inputFilePath,
                Double.toString(note.getRealStartPoint()),
                Double.toString(noteLength),
                envelope[0], // p1
                envelope[1], // p2
                envelope[2], // p3
                envelope[3], // v1
                envelope[4], // v2
                envelope[5], // v3
                envelope[6], // v4
                Double.toString(boundedOverlap), // overlap
                envelope[8], // p4
                envelope[9], // p5
                envelope[10], // v5
                triggerSynthesis ? "LAST_NOTE" : ""); // Triggers final song processing.
        totalDelta += noteLength - boundedOverlap;
    }

    @Override
    public void addSilence(
            double duration,
            double expectedDelta,
            File inputFile,
            File outputFile,
            boolean triggerSynthesis) {
        // Check that current length matches expected length and correct any discrepancies.
        if (expectedDelta > totalDelta && Math.abs(expectedDelta - totalDelta) > 0.01) {
                double timingCorrection = expectedDelta - totalDelta;
                duration += timingCorrection;
                System.out.println("Corrected timing by " + timingCorrection + " ms.");
        }
        String startPoint = "0.0";
        String noteLength = Double.toString(duration); // Tempo already applied.
        String[] envelope = new String[] {"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0"};

        // Call wavtool to add new note onto the end of the output file.
        runner.runProcess(
                getWavtoolPath().getAbsolutePath(),
                outputFile.getAbsolutePath(),
                inputFile.getAbsolutePath(),
                startPoint,
                noteLength,
                envelope[0], // p1
                envelope[1], // p2
                envelope[2], // p3
                envelope[3], // v1
                envelope[4], // v2
                envelope[5], // v3
                envelope[6], // v4
                envelope[7], // overlap
                envelope[8], // p4
                envelope[9], // p5
                envelope[10], // v5
                triggerSynthesis ? "LAST_NOTE" : ""); // Triggers final song processing.
        totalDelta += duration;
    }
}
