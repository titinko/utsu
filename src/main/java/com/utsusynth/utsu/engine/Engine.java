package com.utsusynth.utsu.engine;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.apache.commons.io.FileUtils;

import com.google.common.base.Optional;
import com.google.common.io.Files;
import com.utsusynth.utsu.model.LyricConfig;
import com.utsusynth.utsu.model.Song;
import com.utsusynth.utsu.model.SongIterator;
import com.utsusynth.utsu.model.SongNote;
import com.utsusynth.utsu.model.Voicebank;

public class Engine {
	private final Resampler resampler;
	private final Wavtool wavtool;
	private String resamplerPath;
	private String wavtoolPath;

	public Engine(Resampler resampler, Wavtool wavtool, String resamplerPath, String wavtoolPath) {
		this.resampler = resampler;
		this.wavtool = wavtool;
		this.resamplerPath = resamplerPath;
		this.wavtoolPath = wavtoolPath;
	}

	public String getResamplerPath() {
		return resamplerPath;
	}

	public void setResamplerPath(String resamplerPath) {
		this.resamplerPath = resamplerPath;
	}

	public String getWavtoolPath() {
		return wavtoolPath;
	}

	public void setWavtoolPath(String wavtoolPath) {
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
				e.printStackTrace();
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
			Optional<Double> maybePreutter = getPreutter(Optional.of(note), voicebank);
			Optional<Double> nextPreutter = getPreutter(notes.peekNext(), voicebank);

			// Possible silence before first note.
			if (isFirstNote) {
				double preutter = maybePreutter.isPresent() ? maybePreutter.get() : 0.0;
				if (note.getDelta() - preutter > 0) {
					addSilence(note.getDelta() - preutter, song, renderedNote, finalSong);
				}
				isFirstNote = false;
			}

			// Add silence in place of note if lyric not found.
			if (!config.isPresent()) {
				System.out.println("Could not find config for lyric: " + note.getLyric());
				if (nextPreutter.isPresent()) {
					addSilence(note.getLength() - nextPreutter.get(), song, renderedNote,
							finalSong);
				} else {
					addSilence(note.getLength(), song, renderedNote, finalSong);
				}
				continue;
			}
			System.out.println(config.get());

			// Adjust note length based on preutterance/overlap.
			// TODO: Scale by tempo before calculating this?
			double adjustedLength = getAdjustedLength(voicebank, note, config.get(),
					notes.peekNext());
			System.out.println("Length is " + adjustedLength);

			// Calculate pitchbends.
			int firstStep = getFirstPitchStep(totalDelta, maybePreutter);
			int lastStep = getLastPitchStep(totalDelta, maybePreutter, adjustedLength);
			String pitchString = song.getPitchString(firstStep, lastStep, note.getNoteNum());

			// Re-samples lyric and puts result into renderedNote file.
			resampler.resample(resamplerPath, note, adjustedLength, config.get(), renderedNote,
					pitchString, song);

			// Append rendered note to the output file using wavtool.
			wavtool.addNewNote(wavtoolPath, song, note, adjustedLength, config.get(), renderedNote,
					finalSong,
					// Whether to include overlap in the wavtool.
					areNotesTouching(notes.peekPrev(), voicebank, maybePreutter));

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
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (LineUnavailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private int getFirstPitchStep(int totalDelta, Optional<Double> maybePreutter) {
		double preutter = maybePreutter.isPresent() ? maybePreutter.get() : 0.0;
		return (int) Math.ceil((totalDelta - preutter) / 5.0);
	}

	private int getLastPitchStep(
			int totalDelta,
			Optional<Double> maybePreutter,
			double adjustedLength) {
		// TODO: This is no longer usable if we start changing adjustedLength based on tempo.
		double preutter = maybePreutter.isPresent() ? maybePreutter.get() : 0.0;
		return (int) Math.floor((totalDelta - preutter + adjustedLength) / 5.0);
	}

	// Find length of a note taking into account preutterance and overlap, but not tempo.
	private double getAdjustedLength(
			Voicebank voicebank,
			SongNote cur,
			LyricConfig config,
			Optional<SongNote> next) {
		double noteLength = cur.getDuration();
		// Increase length by this note's preutterance.
		// Cap the preutterance at start of prev note or start of track.
		double preutterance = Math.min(config.getPreutterance(), cur.getDelta());
		noteLength += preutterance;

		// Decrease length by next note's preutterance.
		if (!next.isPresent()) {
			return noteLength;
		}

		Optional<LyricConfig> nextConfig = voicebank.getLyricConfig(next.get().getLyric());
		if (!nextConfig.isPresent()) {
			// Ignore next note if it has an invalid lyric.
			return noteLength;
		}

		double nextPreutter = Math.min(nextConfig.get().getPreutterance(), cur.getLength());
		if (nextPreutter + cur.getDuration() < cur.getLength()) {
			// Ignore next note if it doesn't touch current note.
			return noteLength;
		}

		double encroachingPreutter = nextPreutter + cur.getDuration() - cur.getLength();
		noteLength -= encroachingPreutter;

		// Increase length by next note's overlap.
		double nextOverlap = Math.min(nextConfig.get().getOverlap(), next.get().getFadeIn());
		double nextBoundedOverlap = Math.max(0, Math.min(nextOverlap, next.get().getDuration()));
		noteLength += nextBoundedOverlap;

		return noteLength;
	}

	private Optional<Double> getPreutter(Optional<SongNote> note, Voicebank voicebank) {
		if (!note.isPresent()) {
			return Optional.absent();
		}

		Optional<LyricConfig> config = voicebank.getLyricConfig(note.get().getLyric());
		if (!config.isPresent()) {
			return Optional.absent();
		}
		return Optional.of(Math.min(config.get().getPreutterance(), note.get().getDelta()));
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
