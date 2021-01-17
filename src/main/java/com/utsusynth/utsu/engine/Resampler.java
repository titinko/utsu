package com.utsusynth.utsu.engine;

import java.io.File;
import com.google.inject.Inject;
import com.utsusynth.utsu.common.data.LyricConfigData;
import com.utsusynth.utsu.common.utils.PitchUtils;
import com.utsusynth.utsu.files.AssetManager;
import com.utsusynth.utsu.files.FileNameFixer;
import com.utsusynth.utsu.model.song.Note;
import com.utsusynth.utsu.model.song.Song;
import com.utsusynth.utsu.model.voicebank.LyricConfig;

public class Resampler {
    private final ExternalProcessRunner runner;
    private final FileNameFixer fileNameFixer;
    private final AssetManager assetManager;

    @Inject
    Resampler(
            ExternalProcessRunner runner, FileNameFixer fileNameFixer, AssetManager assetManager) {
        this.runner = runner;
        this.fileNameFixer = fileNameFixer;
        this.assetManager = assetManager;
    }

    void resample(
            File resamplerPath,
            Note note,
            double noteLength,
            LyricConfig config,
            File outputFile,
            String pitchString,
            Song song) {
        double scaleFactor = 125 / song.getTempo();
        String inputFilePath = fileNameFixer.getFixedName(config.getPathToFile().getAbsolutePath());
        String outputFilePath = outputFile.getAbsolutePath();
        String pitch = PitchUtils.noteNumToPitch(note.getNoteNum());
        String consonantVelocity = Double.toString(note.getVelocity() * scaleFactor);
        String flags = note.getNoteFlags().isEmpty() ? song.getFlags() : note.getNoteFlags();
        String offset = Double.toString(config.getOffset());
        double startPoint = note.getStartPoint() + note.getAutoStartPoint();
        double scaledLength = (noteLength + startPoint + 1) * scaleFactor;
        double consonantLength = config.getConsonant(); // TODO: Cutoff?
        String cutoff = Double.toString(config.getCutoff());
        String intensity = Integer.toString(note.getIntensity());
        String modulation = Integer.toString(note.getModulation()); // TODO: Set this song-wide?
        String tempo = "T" + song.getTempo(); // TODO: Override with note tempo.

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


    /**
     * Play a note based on Note and LyricConfigData, using the resampler
     * @param resamplerPath Path to the resampler
     * @param note A Note object
     * @param noteLength Note length, in ms
     * @param config a LyricConfigData, as present on LyricConfigEditor
     * @param outputFile File to write the result
     * @param pitchString Pitch changes
     * @param tempo Tempo in BPM at which pitch changes are established
     */
    public void resampleNote(
            File resamplerPath,
            Note note,
            double noteLength,
            LyricConfigData config,
            File outputFile,
            String pitchString,
            int tempo) {
        String inputFilePath = fileNameFixer.getFixedName(config.getPathToFile().getAbsolutePath());
        String outputFilePath = outputFile.getAbsolutePath();
        String pitch = PitchUtils.noteNumToPitch(note.getNoteNum());
        String consonantVelocity = Double.toString(note.getVelocity());
        String flags = note.getNoteFlags().isEmpty() ? "?" : note.getNoteFlags();
        String offset = Double.toString(config.offsetProperty().getValue());
        double consonantLength = config.consonantProperty().getValue();
        String cutoff = Double.toString(config.cutoffProperty().getValue());
        String intensity = Integer.toString(note.getIntensity());
        String modulation = Integer.toString(note.getModulation());
        String tempoString = "T" + tempo;

        // Call resampler.
        runner.runProcess(
                resamplerPath.getAbsolutePath(),
                inputFilePath,
                outputFilePath,
                pitch,
                consonantVelocity,
                flags.isEmpty() ? "?" : flags,
                offset,
                Double.toString(noteLength),
                Double.toString(consonantLength),
                cutoff,
                intensity,
                modulation,
                tempoString,
                pitchString);
    }


    void resampleSilence(File resamplerPath, File outputFile, double duration) {
        String desiredLength = Double.toString(duration + 1);
        runner.runProcess(
                resamplerPath.getAbsolutePath(),
                assetManager.getSilenceFile().getAbsolutePath(),
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
