package com.utsusynth.utsu.engine;

import java.io.File;

import com.google.inject.Inject;
import com.utsusynth.utsu.common.PitchUtils;
import com.utsusynth.utsu.model.LyricConfig;
import com.utsusynth.utsu.model.Song;
import com.utsusynth.utsu.model.SongNote;

public class Resampler {
	private static final String SILENCE_PATH = "/Users/emmabreen/Desktop/silence.wav";
	
	private final ExternalProcessRunner runner;
	
	@Inject
	Resampler(ExternalProcessRunner runner) {
		this.runner = runner;
	}
	
	void resample(
			String resamplerPath,
			SongNote note,
			double noteLength,
			LyricConfig config,
			File outputFile,
			String pitchString,
			Song song) {
		String inputFilePath = config.getPathToFile();
		String outputFilePath = outputFile.getAbsolutePath();
		String pitch = PitchUtils.noteNumToPitch(note.getNoteNum());
		String consonantVelocity = Double.toString(note.getVelocity());
		String flags = note.getNoteFlags().isEmpty() ? song.getFlags() : note.getNoteFlags();
		String offset = Double.toString(config.getOffset());
		double scaledLength = noteLength * (125 / song.getTempo()) + 1;
		String consonantLength = Double.toString(config.getConsonant()); // TODO: Cutoff?
		String cutoff = Double.toString(config.getCutoff());		
		String intensity = Integer.toString(note.getIntensity());
		String modulation = Integer.toString(note.getModulation());  // TODO: Set this song-wide?
		String tempo = "T" + Double.toString(song.getTempo()); // TODO: Override with note tempo.
		
		// Call resampler.
		runner.runProcess(
				resamplerPath,
				inputFilePath,
				outputFilePath,
				pitch,
				consonantVelocity,
				flags,
				offset,
				Double.toString(scaledLength),
				consonantLength,
				cutoff,
				intensity,
				modulation,
				tempo,
				pitchString);
	}
	
	void resampleSilence(String resamplerPath, File outputFile, double duration) {
		String outputFilePath = outputFile.getAbsolutePath();
		String desiredLength = Double.toString(duration + 1);
		runner.runProcess(
				resamplerPath,
				SILENCE_PATH,
				outputFilePath,
				"C4",
				"100",
				"",
				"0",
				desiredLength,
				"0",
				"0",
				"100",
				"0");
	}
}
