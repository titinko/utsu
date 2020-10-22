package com.utsusynth.utsu.engine;

import java.io.File;
import com.google.inject.Inject;
import com.utsusynth.utsu.model.song.Song;
import com.utsusynth.utsu.model.song.Note;
import com.utsusynth.utsu.model.voicebank.LyricConfig;

public class Wavtool {
    private final ExternalProcessRunner runner;
    private double totalDelta = 0; // Total duration in ms, used to debug timing issues.

    @Inject
    Wavtool(ExternalProcessRunner runner) {
        this.runner = runner;
    }

    void startRender(int startDelta) {
        totalDelta = startDelta;
    }

    void addNewNote(
            File wavtoolPath,
            Song song,
            Note note,
            double noteLength,
            File inputFile,
            File outputFile,
            boolean includeOverlap,
            boolean triggerSynthesis) {
        String outputFilePath = outputFile.getAbsolutePath();
        String inputFilePath = inputFile.getAbsolutePath();
        double startPoint = note.getStartPoint() + note.getAutoStartPoint();
        String[] envelope = note.getFullEnvelope();

        double boundedOverlap = Math.max(0, Math.min(note.getFadeIn(), noteLength));
        // Ignore overlap if current note doesn't touch previous one.
        if (!includeOverlap) {
            boundedOverlap = 0;
        }

        double scaleFactor = 125 / song.getTempo();
        double scaledNoteLength = noteLength * scaleFactor;
        double scaledOverlap = boundedOverlap * scaleFactor;
        double scaledStartPoint = startPoint * scaleFactor;

        // Call wavtool to add new note onto the end of the output file.
        runner.runProcess(
                wavtoolPath.getAbsolutePath(),
                outputFilePath,
                inputFilePath,
                Double.toString(scaledStartPoint),
                Double.toString(scaledNoteLength),
                envelope[0], // p1
                envelope[1], // p2
                envelope[2], // p3
                envelope[3], // v1
                envelope[4], // v2
                envelope[5], // v3
                envelope[6], // v4
                Double.toString(scaledOverlap), // overlap
                envelope[8], // p4
                envelope[9], // p5
                envelope[10], // v5
                triggerSynthesis ? "LAST_NOTE" : ""); // Triggers final song processing.
        totalDelta += scaledNoteLength - scaledOverlap;
    }

    void addSilence(
            File wavtoolPath,
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
                wavtoolPath.getAbsolutePath(),
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
