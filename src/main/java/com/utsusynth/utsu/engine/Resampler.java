package com.utsusynth.utsu.engine;

import java.io.File;

import com.google.inject.Inject;
import com.utsusynth.utsu.common.utils.PitchUtils;
import com.utsusynth.utsu.files.FileNameMapper;
import com.utsusynth.utsu.model.song.Note;
import com.utsusynth.utsu.model.song.Song;
import com.utsusynth.utsu.model.voicebank.LyricConfig;

public class Resampler {
    private static final File SILENCE_PATH = new File("assets/silence.wav");

    private final ExternalProcessRunner runner;

    @Inject
    Resampler(ExternalProcessRunner runner) {
        this.runner = runner;
    }

    void resample(
            File resamplerPath,
            Note note,
            double noteLength,
            LyricConfig config,
            File outputFile,
            String pitchString,
            Song song) {

        FileNameMapper fileUtils = FileNameMapper.getInstance();
        String inputFilePath = fileUtils.getOSName(config.getPathToFile().getAbsolutePath());
        String outputFilePath = outputFile.getAbsolutePath();
        String pitch = PitchUtils.noteNumToPitch(note.getNoteNum());
        String consonantVelocity = Double.toString(note.getVelocity() * (song.getTempo() / 125));
        String flags = note.getNoteFlags().isEmpty() ? song.getFlags() : note.getNoteFlags();
        String offset = Double.toString(config.getOffset());
        double startPoint = note.getStartPoint() + note.getAutoStartPoint();
        double scaledLength = noteLength * (125 / song.getTempo()) + startPoint + 1;
        double consonantLength = config.getConsonant(); // TODO: Cutoff?
        String cutoff = Double.toString(config.getCutoff());
        String intensity = Integer.toString(note.getIntensity());
        String modulation = Integer.toString(note.getModulation()); // TODO: Set this song-wide?
        String tempo = "T" + Double.toString(song.getTempo()); // TODO: Override with note tempo.

        // Call resampler.
        runner.runProcess(
                resamplerPath.getAbsolutePath(),
                inputFilePath,
                outputFilePath,
                pitch,
                consonantVelocity,
                flags.isEmpty() ? "?" : flags, // Uses placeholder value if there are no flags.
                offset,
                Double.toString(scaledLength),
                Double.toString(consonantLength),
                cutoff,
                intensity,
                modulation,
                tempo,
                pitchString);
    }

    void resampleSilence(File resamplerPath, File outputFile, double duration) {
        String desiredLength = Double.toString(duration + 1);
        runner.runProcess(
                resamplerPath.getAbsolutePath(),
                SILENCE_PATH.getAbsolutePath(),
                outputFile.getAbsolutePath(),
                "C4",
                "100",
                "?",
                "0",
                desiredLength,
                "0",
                "0",
                "100",
                "0");
    }
}
