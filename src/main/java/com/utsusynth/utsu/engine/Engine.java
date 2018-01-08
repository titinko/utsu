package com.utsusynth.utsu.engine;

import com.google.common.base.Optional;
import com.google.common.io.Files;
import com.utsusynth.utsu.common.exception.ErrorLogger;
import com.utsusynth.utsu.model.Song;
import com.utsusynth.utsu.model.SongIterator;
import com.utsusynth.utsu.model.SongNote;
import com.utsusynth.utsu.model.voicebank.LyricConfig;
import com.utsusynth.utsu.model.voicebank.Voicebank;
import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import org.apache.commons.io.FileUtils;

public class Engine {
	private static final ErrorLogger errorLogger = ErrorLogger.getLogger();

	private final Resampler resampler;
	private final Wavtool wavtool;
	private File resamplerPath;
	private File wavtoolPath;

	public Engine(Resampler resampler, Wavtool wavtool, File resamplerPath, File wavtoolPath) {
		this.resampler = resampler;
		this.wavtool = wavtool;
		this.resamplerPath = resamplerPath;
		this.wavtoolPath = wavtoolPath;
	}

	public File getResamplerPath() {
		return resamplerPath;
	}

	public void setResamplerPath(File resamplerPath) {
		this.resamplerPath = resamplerPath;
	}

	public File getWavtoolPath() {
		return wavtoolPath;
	}

	public void setWavtoolPath(File wavtoolPath) {
		this.wavtoolPath = wavtoolPath;
	}

	public void render(Song song, Optional<File> finalDestination) {
		// Create temporary directory for rendering.
		File tempDir = Files.createTempDir();
		File renderedNote = new File(tempDir, "rendered_note.wav");
		File finalSong = new File(tempDir, "final_song.wav");
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				FileUtils.deleteDirectory(tempDir);
			} catch (IOException e) {
				errorLogger.logError(e);
			}
		}));

		SongIterator notes = song.getNoteIterator();
		if (!notes.hasNext()) {
			return;
		}
		int totalDelta = 0;
		Voicebank voicebank = song.getVoicebank();
		boolean isFirstNote = true;
		while (notes.hasNext()) {
			SongNote note = notes.next();
			totalDelta += note.getDelta();
			Optional<LyricConfig> config = voicebank.getLyricConfig(note.getLyric());
			double preutter = note.getRealPreutter();
			Optional<Double> nextPreutter = Optional.absent();
			if (notes.peekNext().isPresent()) {
				nextPreutter = Optional.of(notes.peekNext().get().getRealPreutter());
			}

			// Possible silence before first note.
			if (isFirstNote) {
				if (note.getDelta() - preutter > 0) {
					addSilence(note.getDelta() - preutter, song, renderedNote, finalSong);
				}
				isFirstNote = false;
			}

			// Add silence in place of note if lyric not found.
			if (!config.isPresent()) {
				System.out.println("Could not find config for lyric: " + note.getLyric());
				if (nextPreutter.isPresent()) {
					addSilence(
							note.getLength() - nextPreutter.get(),
							song,
							renderedNote,
							finalSong);
				} else {
					addSilence(note.getLength(), song, renderedNote, finalSong);
				}
				continue;
			}
			System.out.println(config.get());

			// Adjust note length based on preutterance/overlap.
			double adjustedLength =
					note.getRealDuration() > -1 ? note.getRealDuration() : note.getDuration();
			System.out.println("Length is " + adjustedLength);

			// Calculate pitchbends.
			int firstStep = getFirstPitchStep(totalDelta, preutter);
			int lastStep = getLastPitchStep(totalDelta, preutter, adjustedLength);
			String pitchString = song.getPitchString(firstStep, lastStep, note.getNoteNum());

			// Re-samples lyric and puts result into renderedNote file.
			resampler.resample(
					resamplerPath,
					note,
					adjustedLength,
					config.get(),
					renderedNote,
					pitchString,
					song);

			// Append rendered note to the output file using wavtool.
			wavtool.addNewNote(
					wavtoolPath,
					song,
					note,
					adjustedLength,
					config.get(),
					renderedNote,
					finalSong,
					// Whether to include overlap in the wavtool.
					areNotesTouching(notes.peekPrev(), voicebank, Optional.of(preutter)));

			// Possible silence after each note.
			if (notes.peekNext().isPresent()
					&& !areNotesTouching(Optional.of(note), voicebank, nextPreutter)) {
				// Add silence
				double silenceLength;
				if (nextPreutter.isPresent()) {
					silenceLength = note.getLength() - note.getDuration() - nextPreutter.get();
				} else {
					silenceLength = note.getLength() - note.getDuration();
				}
				addSilence(silenceLength, song, renderedNote, finalSong);
			}
		}
		if (finalDestination.isPresent()) {
			finalSong.renameTo(finalDestination.get());
		} else {
			playSong(finalSong);
		}
	}

	private void addSilence(double duration, Song song, File renderedNote, File finalSong) {
		if (duration <= 0.0) {
			return;
		}
		duration = duration * (125.0 / song.getTempo());
		resampler.resampleSilence(resamplerPath, renderedNote, duration);
		wavtool.addSilence(wavtoolPath, duration, renderedNote, finalSong);
	}

	private void playSong(File wavFile) {
		// Use the least glitchy audio player for each operating system.
		String os = System.getProperty("os.name").toLowerCase();
		if (os.contains("mac")) {
			try {
				Clip clip = AudioSystem.getClip();
				clip.addLineListener((event) -> {
					if (event.getType() == LineEvent.Type.STOP) {
						clip.close();
					}
				});
				clip.open(AudioSystem.getAudioInputStream(wavFile));
				clip.start();
			} catch (UnsupportedAudioFileException e) {
				// TODO Auto-generated catch block
				errorLogger.logError(e);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				errorLogger.logError(e);
			} catch (LineUnavailableException e) {
				// TODO Auto-generated catch block
				errorLogger.logError(e);
			}
		} else {
			Media media = new Media(wavFile.toURI().toString());
			MediaPlayer player = new MediaPlayer(media);
			player.play();
		}
	}

	private int getFirstPitchStep(int totalDelta, double preutter) {
		return (int) Math.ceil((totalDelta - preutter) / 5.0);
	}

	private int getLastPitchStep(int totalDelta, double preutter, double adjustedLength) {
		return (int) Math.floor((totalDelta - preutter + adjustedLength) / 5.0);
	}

	// Determines whether two notes are "touching" given the second note's preutterance.
	private boolean areNotesTouching(
			Optional<SongNote> note,
			Voicebank voicebank,
			Optional<Double> nextPreutter) {
		if (!note.isPresent() || !nextPreutter.isPresent()) {
			return false;
		}

		if (!voicebank.getLyricConfig(note.get().getLyric()).isPresent()) {
			return false;
		}

		double preutterance = Math.min(nextPreutter.get(), note.get().getLength());
		if (preutterance + note.get().getDuration() < note.get().getLength()) {
			return false;
		}
		return true;
	}
}
