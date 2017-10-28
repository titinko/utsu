package com.utsusynth.utsu.engine;

import java.io.File;

import com.google.inject.Inject;
import com.utsusynth.utsu.model.Song;
import com.utsusynth.utsu.model.SongNote;
import com.utsusynth.utsu.model.voicebank.LyricConfig;

public class Wavtool {
	private final ExternalProcessRunner runner;

	@Inject
	Wavtool(ExternalProcessRunner runner) {
		this.runner = runner;
	}

	void addNewNote(
			String wavtoolPath,
			Song song,
			SongNote note,
			double noteLength,
			LyricConfig config,
			File inputFile,
			File outputFile,
			boolean includeOverlap) {
		String outputFilePath = outputFile.getAbsolutePath();
		String inputFilePath = inputFile.getAbsolutePath();
		double startPoint = note.getStartPoint(); // TODO: Add auto start point.
		String[] envelope = note.getFullEnvelope();

		double overlap = Math.min(config.getOverlap(), note.getFadeIn());
		double boundedOverlap = Math.max(0, Math.min(overlap, noteLength));
		// Ignore overlap if current note doesn't touch previous one.
		if (!includeOverlap) {
			boundedOverlap = 0;
		}

		double scaleFactor = 125 / song.getTempo();

		// Call wavtool to add new note onto the end of the output file.
		runner.runProcess(
				wavtoolPath,
				outputFilePath,
				inputFilePath,
				Double.toString(startPoint),
				Double.toString(noteLength * scaleFactor),
				envelope[0], // p1
				envelope[1], // p2
				envelope[2], // p3
				envelope[3], // v1
				envelope[4], // v2
				envelope[5], // v3
				envelope[6], // v4
				Double.toString(boundedOverlap * scaleFactor), // overlap
				envelope[8], // p4
				envelope[9], // p5
				envelope[10]); // v5
	}

	void addSilence(String wavtoolPath, double duration, File inputFile, File outputFile) {
		String outputFilePath = outputFile.getAbsolutePath();
		String inputFilePath = inputFile.getAbsolutePath();
		String startPoint = "0.0";
		String noteLength = Double.toString(duration); // Tempo already applied.
		String[] envelope = new String[] { "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0" };

		// Call wavtool to add new note onto the end of the output file.
		runner.runProcess(
				wavtoolPath,
				outputFilePath,
				inputFilePath,
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
				envelope[10]); // v5
	}
}
