package com.utsusynth.utsu.engine;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.io.Files;
import com.utsusynth.utsu.common.PitchUtils;
import com.utsusynth.utsu.common.exception.ErrorLogger;
import com.utsusynth.utsu.common.quantize.Quantizer;
import com.utsusynth.utsu.model.Song;
import com.utsusynth.utsu.model.SongIterator;
import com.utsusynth.utsu.model.SongNote;
import com.utsusynth.utsu.model.voicebank.LyricConfig;
import com.utsusynth.utsu.model.voicebank.Voicebank;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

public class Engine {
    private static final ErrorLogger errorLogger = ErrorLogger.getLogger();
    private static MediaPlayer mediaPlayer; // Used for audio playback.

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

    public void renderWav(Song song, File finalDestination) {
        Optional<File> finalSong = render(song);
        if (finalSong.isPresent()) {
            finalSong.get().renameTo(finalDestination);
        }
    }

    public void playSong(Song song, Function<Duration, Void> callback) {
        Optional<File> finalSong = render(song);
        if (finalSong.isPresent()) {
            Media media = new Media(finalSong.get().toURI().toString());
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.setOnReady(() -> {
                callback.apply(media.getDuration());
            });
            mediaPlayer.play();
        }
    }

    private Optional<File> render(Song song) {
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
            return Optional.absent();
        }
        int totalDelta = 0;
        Voicebank voicebank = song.getVoicebank();
        boolean isFirstNote = true;
        while (notes.hasNext()) {
            SongNote note = notes.next();
            totalDelta += note.getDelta();

            // Get lyric config.
            Optional<LyricConfig> config = Optional.absent();
            if (!note.getTrueLyric().isEmpty()) {
                config = voicebank.getLyricConfig(note.getTrueLyric());
            }
            if (!config.isPresent()) {
                // Make one last valiant effort to find the true lyric.
                String prevLyric = getNearbyPrevLyric(notes.peekPrev());
                String pitch = PitchUtils.noteNumToPitch(note.getNoteNum());
                config = voicebank.getLyricConfig(prevLyric, note.getLyric(), pitch);
                if (config.isPresent()) {
                    note.setTrueLyric(config.get().getTrueLyric());
                }
            }

            // Find preutterance of current and next notes.
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
                if (notes.peekNext().isPresent()) {
                    addSilence(
                            note.getLength() - notes.peekNext().get().getRealPreutter(),
                            song,
                            renderedNote,
                            finalSong);
                } else {
                    // Case where the last note in the song is silent.
                    addFinalSilence(note.getLength(), song, renderedNote, finalSong);
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
                    areNotesTouching(notes.peekPrev(), voicebank, Optional.of(preutter)),
                    // Whether this is the last note in the song.
                    !notes.peekNext().isPresent());

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
        return Optional.of(finalSong);
    }

    private void addSilence(double duration, Song song, File renderedNote, File finalSong) {
        if (duration <= 0.0) {
            return;
        }
        duration = duration * (125.0 / song.getTempo());
        resampler.resampleSilence(resamplerPath, renderedNote, duration);
        wavtool.addSilence(wavtoolPath, duration, renderedNote, finalSong, false);
    }

    private void addFinalSilence(double duration, Song song, File renderedNote, File finalSong) {
        // The final note must be passed to the wavtool.
        duration = Math.max(duration, 0) * (125.0 / song.getTempo());
        resampler.resampleSilence(resamplerPath, renderedNote, duration);
        wavtool.addSilence(wavtoolPath, duration, renderedNote, finalSong, true);
    }

    // Returns empty string if there is no nearby (within DEFAULT_NOTE_DURATION) previous note.
    private static String getNearbyPrevLyric(Optional<SongNote> prev) {
        if (prev.isPresent() && prev.get().getLength()
                - prev.get().getDuration() > Quantizer.DEFAULT_NOTE_DURATION) {
            return prev.get().getLyric();
        }
        return "";
    }

    private static int getFirstPitchStep(int totalDelta, double preutter) {
        return (int) Math.ceil((totalDelta - preutter) / 5.0);
    }

    private static int getLastPitchStep(int totalDelta, double preutter, double adjustedLength) {
        return (int) Math.floor((totalDelta - preutter + adjustedLength) / 5.0);
    }

    // Determines whether two notes are "touching" given the second note's preutterance.
    private static boolean areNotesTouching(
            Optional<SongNote> note,
            Voicebank voicebank,
            Optional<Double> nextPreutter) {
        if (!note.isPresent() || !nextPreutter.isPresent()) {
            return false;
        }

        // Return false if current note cannot be rendered.
        if (!voicebank.getLyricConfig(note.get().getTrueLyric()).isPresent()) {
            return false;
        }

        double preutterance = Math.min(nextPreutter.get(), note.get().getLength());
        if (preutterance + note.get().getDuration() < note.get().getLength()) {
            return false;
        }
        return true;
    }
}
