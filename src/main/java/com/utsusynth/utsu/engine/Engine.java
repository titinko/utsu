package com.utsusynth.utsu.engine;

import com.google.common.base.Function;
import com.utsusynth.utsu.common.utils.RegionBounds;
import com.utsusynth.utsu.common.StatusBar;
import com.utsusynth.utsu.common.data.LyricConfigData;
import com.utsusynth.utsu.common.exception.ErrorLogger;
import com.utsusynth.utsu.common.quantize.Quantizer;
import com.utsusynth.utsu.common.utils.PitchUtils;
import com.utsusynth.utsu.engine.resampler.Resampler;
import com.utsusynth.utsu.engine.wavtool.Wavtool;
import com.utsusynth.utsu.engine.wavtool.WavtoolConverter;
import com.utsusynth.utsu.files.CacheManager;
import com.utsusynth.utsu.files.PreferencesManager;
import com.utsusynth.utsu.files.PreferencesManager.CacheMode;
import com.utsusynth.utsu.model.song.Note;
import com.utsusynth.utsu.model.song.NoteIterator;
import com.utsusynth.utsu.model.song.Song;
import com.utsusynth.utsu.model.voicebank.LyricConfig;
import com.utsusynth.utsu.model.voicebank.Voicebank;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaPlayer.Status;
import javafx.util.Duration;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.*;

public class Engine {
    private static final ErrorLogger errorLogger = ErrorLogger.getLogger();

    public enum PlaybackStatus {
        PLAYING, PAUSED, STOPPED,
    }

    private final StatusBar statusBar;
    private final int threadPoolSize;
    private final CacheManager cacheManager;
    private final PreferencesManager preferencesManager;
    private Resampler resampler;
    private Wavtool wavtool;
    private File resamplerPath;

    private MediaPlayer instrumentalPlayer; // Used for background music.
    private MediaPlayer mediaPlayer; // Used for audio playback.

    public Engine(
            Resampler resampler,
            WavtoolConverter wavtoolConverter,
            StatusBar statusBar,
            int threadPoolSize,
            CacheManager cacheManager,
            PreferencesManager preferencesManager) {
        this.statusBar = statusBar;
        this.threadPoolSize = threadPoolSize;
        this.cacheManager = cacheManager;
        this.preferencesManager = preferencesManager;
        this.resampler = resampler;
        wavtool = wavtoolConverter.fromString(preferencesManager.getWavtool());
        resamplerPath = preferencesManager.getResampler();
    }

    public File getResamplerPath() {
        if (resamplerPath != null) {
            return resamplerPath;
        }
        return preferencesManager.getResampler();
    }

    public void setResamplerPath(File resamplerPath) {
        this.resamplerPath = resamplerPath;
    }

    public Resampler getResampler() {
        return resampler;
    }

    public void setResampler(Resampler resampler) {
        this.resampler = resampler;
    }

    public Wavtool getWavtool() {
        return wavtool;
    }

    public void setWavtool(Wavtool wavtool) {
        this.wavtool = wavtool;
    }

    /**
     * Exports of region of a song to a WAV file.
     *
     * @return Whether or not there is any sound to export.
     */
    public boolean renderWav(Song song, File finalDestination) {
        Optional<File> finalSong = render(song, RegionBounds.WHOLE_SONG);
        if (finalSong.isPresent()) {
            try {
                FileUtils.copyFile(finalSong.get(), finalDestination);
            } catch (IOException e) {
                errorLogger.logError(e);
            }
        }
        return finalSong.isPresent();
    }

    /**
     * Starts playback for a region of a song.
     *
     * @return Whether or not there is any sound to play.
     */
    public boolean startPlayback(
            Song song,
            RegionBounds bounds,
            Function<Duration, Void> startCallback,
            Runnable endCallback) {
        stopPlayback(); // Clear existing playback, if present.
        long start = System.currentTimeMillis();
        Optional<File> finalSong = render(song, bounds);
        long time = System.currentTimeMillis() - start;
        System.out.println("Rendering took " + (time / 1000.0) + " seconds.");
        if (finalSong.isPresent() && finalSong.get().canRead()) {
            // Play instrumental, if present.
            if (song.getInstrumental().isPresent()) {
                Media instrumental = new Media(song.getInstrumental().get().toURI().toString());
                System.out.println(instrumental.getSource());
                instrumentalPlayer = new MediaPlayer(instrumental);
                instrumentalPlayer.play();
            }
            Media media = new Media(finalSong.get().toURI().toString());
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.setOnReady(() -> startCallback.apply(media.getDuration()));
            mediaPlayer.setOnEndOfMedia(() -> mediaPlayer.stop());
            mediaPlayer.setOnStopped(() -> {
                // Explicitly release media and garbage collect player.
                mediaPlayer.dispose();
                mediaPlayer = null;
                endCallback.run();
                if (instrumentalPlayer != null) {
                    instrumentalPlayer.stop();
                    instrumentalPlayer.dispose();
                }
            });
            mediaPlayer.play();
        }
        return finalSong.isPresent();
    }

    public void pausePlayback() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
        }
        if (instrumentalPlayer != null && instrumentalPlayer.getStatus().equals(Status.PLAYING)) {
            instrumentalPlayer.pause();
        }
    }

    public void resumePlayback() {
        if (mediaPlayer != null) {
            mediaPlayer.play();
        }
        if (instrumentalPlayer != null && instrumentalPlayer.getStatus().equals(Status.PAUSED)) {
            instrumentalPlayer.play();
        }
    }

    public void stopPlayback() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }
    }

    public PlaybackStatus getStatus() {
        if (mediaPlayer != null) {
            switch (mediaPlayer.getStatus()) {
                case PLAYING:
                    return PlaybackStatus.PLAYING;
                case PAUSED:
                    return PlaybackStatus.PAUSED;
                default:
                    return PlaybackStatus.STOPPED;
            }
        }
        return PlaybackStatus.STOPPED;
    }

    /**
     * Play a note from a voicebank, using the current resampler.
     */
    public void playLyricWithResampler(LyricConfigData lyricData, int modulation) {
        File renderedNote;
        Note note = new Note();
        note.setLyric(lyricData.getLyric());
        note.setNoteNum(60);
        note.setDuration(2000); // Unnecessary.
        note.setModulation(modulation);
        renderedNote = cacheManager.createNoteCache();
        resampler.resampleNote(
                getResamplerPath(),
                note,
                2000.0,
                lyricData,
                renderedNote,
                "",
                120);

        try {
            Media media = new Media(renderedNote.toURI().toString());
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.setOnReady(() -> {
                mediaPlayer.play();
            });
        } catch (IllegalArgumentException e) {
            System.out.println("Not correct parameter for file");
        }
    }

    private Optional<File> render(Song song, RegionBounds bounds) {
        // Use cached render if it exists and cache is enabled.
        if (preferencesManager.getCache().equals(CacheMode.DISABLED)) {
            song.clearCache();
        } else if (bounds.equals(song.getCacheRegion())
                && song.getCacheFile().isPresent()
                && song.getCacheFile().get().exists()) {
            return song.getCacheFile();
        }

        // Set up a thread pool for asynchronous rendering.
        ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);
        ArrayList<Future<Runnable>> futures = new ArrayList<>();

        NoteIterator notes = song.getNoteIterator(bounds);
        if (!notes.hasNext()) {
            return Optional.empty();
        }

        int startPosition = bounds.getMinMs();
        int totalDelta = notes.getCurDelta(); // Absolute position of current note.
        double scaleFactor = 125.0 / song.getTempo(); // TODO: Override with note tempo.
        Voicebank voicebank = song.getVoicebank();
        boolean isFirstNote = true;
        final File finalSong = cacheManager.createRenderedCache();

        while (notes.hasNext()) {
            Note note = notes.next();
            totalDelta += note.getDelta();

            // Get lyric config.
            Optional<LyricConfig> config = Optional.empty();
            if (!note.getTrueLyric().isEmpty()) {
                config = voicebank.getLyricConfig(note.getTrueLyric());
            }
            if (config.isEmpty()) {
                // Make one last valiant effort to find the true lyric.
                String prevLyric = getNearbyPrevLyric(notes.peekPrev());
                String pitch = PitchUtils.noteNumToPitch(note.getNoteNum());
                config = voicebank.getLyricConfig(prevLyric, note.getLyric(), pitch);
                if (config.isPresent()) {
                    System.out.println("Would set true lyric.");
                    // note.setTrueLyric(config.get().getTrueLyric());
                }
            }

            // Find preutterance of current and next notes.
            double preutter = note.getRealPreutter();
            Optional<Double> nextPreutter = Optional.empty();
            if (notes.peekNext().isPresent()) {
                nextPreutter = Optional.of(notes.peekNext().get().getRealPreutter());
            }

            // Possible silence before first note.
            if (isFirstNote) {
                if (notes.getCurDelta() - preutter > bounds.getMinMs()) {
                    double firstNoteDelta =
                            (notes.getCurDelta() - bounds.getMinMs()) * scaleFactor - preutter;
                    addSilence(firstNoteDelta, 0, finalSong, executor, futures);
                }
                isFirstNote = false;
            }

            // Add silence in place of note if lyric not found.
            if (config.isEmpty()) {
                System.out.println("Could not find config for lyric: " + note.getLyric());
                if (notes.peekNext().isPresent()) {
                    addSilence(
                            note.getLength() * scaleFactor
                                    - notes.peekNext().get().getRealPreutter(),
                            totalDelta * scaleFactor,
                            finalSong,
                            executor,
                            futures);
                } else {
                    // Case where the last note in the song is silent.
                    addFinalSilence(
                            note.getLength() * scaleFactor,
                            totalDelta * scaleFactor,
                            finalSong,
                            executor,
                            futures);
                }
                continue;
            }
            // System.out.println(config.get());

            // Adjust note length based on preutterance/overlap.
            double nextNoteEncroachment = 0;
            if (notes.peekNext().isPresent() && areNotesTouching(
                    Optional.of(note), voicebank, nextPreutter, song.getTempo())) {
                Note nextNote = notes.peekNext().get();
                double scaledGap = (note.getLength() - note.getDuration()) * scaleFactor;
                nextNoteEncroachment = nextNote.getTrueLyric().isEmpty()
                        ? 0 : nextNote.getRealPreutter() - scaledGap - nextNote.getFadeIn();
            }
            double adjustedLength =
                    (note.getDuration() * scaleFactor) + preutter - nextNoteEncroachment;
            // System.out.println("Real duration is " + adjustedLength);

            // Calculate pitchbends.
            int firstStep = getFirstPitchStep(totalDelta, preutter);
            int lastStep = getLastPitchStep(totalDelta, preutter, adjustedLength);
            String pitchString = song.getPitchString(firstStep, lastStep, note.getNoteNum());

            // Apply resampler in separate thread and schedule wavtool.
            final LyricConfig curConfig = config.get();
            final boolean includeOverlap = areNotesTouching(
                    notes.peekPrev(), voicebank, Optional.of(preutter), song.getTempo());
            final boolean isLastNote = notes.peekNext().isEmpty();
            final int currentTotalDelta = totalDelta;
            final double expectedDelta = totalDelta - preutter;
            futures.add(executor.submit(() -> {
                // Re-samples lyric and puts result into renderedNote file.
                File renderedNote;
                if (preferencesManager.getCache().equals(CacheMode.DISABLED)) {
                    song.clearNoteCache(currentTotalDelta, currentTotalDelta);
                }
                if (note.getCacheFile() == null || !note.getCacheFile().exists()) {
                    renderedNote = cacheManager.createNoteCache();
                    resampler.resample(
                            getResamplerPath(),
                            note,
                            adjustedLength,
                            curConfig,
                            renderedNote,
                            pitchString,
                            song);
                    // Wait up to one second for file to become readable.
                    waitUntilFileReadable(renderedNote);
                    if (preferencesManager.getCache().equals(CacheMode.ENABLED)) {
                        note.setCacheFile(renderedNote);
                    }
                } else {
                    renderedNote = note.getCacheFile();
                }
                return () -> wavtool.addNewNote(
                        song,
                        note,
                        adjustedLength,
                        expectedDelta,
                        renderedNote,
                        finalSong,
                        includeOverlap,
                        isLastNote);
            }));

            // Possible silence after each note.
            if (notes.peekNext().isPresent() && !areNotesTouching(
                    Optional.of(note), voicebank, nextPreutter, song.getTempo())) {
                // Add silence
                double silenceLength;
                if (nextPreutter.isPresent()) {
                    silenceLength = (note.getLength() - note.getDuration()) * scaleFactor
                            - nextPreutter.get();
                } else {
                    silenceLength = (note.getLength() - note.getDuration()) * scaleFactor;
                }
                addSilence(
                        silenceLength,
                        (totalDelta + note.getDuration()) * scaleFactor,
                        finalSong,
                        executor,
                        futures);
            }
        }

        // When resampler finishes, run wavtool on notes in sequential order.
        // The wavtool is given its own thread so it can be terminated separately from the engine.
        Thread wavtoolThread = new Thread(() -> {
            wavtool.startRender(startPosition * scaleFactor);
            for (int i = 0; i < futures.size(); i++) {
                try {
                    double curProgress = i * 1.0 / futures.size();
                    statusBar.setProgressAsync(curProgress);
                    futures.get(i).get().run();
                    if (Thread.interrupted()) {
                        throw new InterruptedException("Wavtool thread manually interrupted.");
                    }
                } catch (InterruptedException | CancellationException | ExecutionException e) {
                    System.out.println("Wavtool run failed or was canceled.");
                    errorLogger.logError(e);
                    executor.shutdownNow(); // Shutdown all resamplers.
                    return; // Return early from wavtool thread.
                }
            }
        });
        statusBar.startProgress(wavtoolThread::interrupt);
        wavtoolThread.start();
        try {
            wavtoolThread.join();
            waitUntilFileReadable(finalSong);
        } catch (InterruptedException e) {
            // This triggers if engine thread is interrupted while waiting for the wavtool thread.
            errorLogger.logError(e);
            return Optional.empty();
        }
        statusBar.endProgress(); // Mark task as complete.
        executor.shutdown(); // Shut down thread pool

        if (!finalSong.canRead()) {
            System.out.println("Render did not produce a valid file.");
            return Optional.empty();
        }
        if (preferencesManager.getCache().equals(CacheMode.ENABLED)) {
            song.setCache(bounds, finalSong); // Cache region that was played.
        } else {
            cacheManager.clearNotes(); // Clear note cache if we aren't keeping caches.
        }
        cacheManager.clearSilences(); // Clear all silence temp files.
        return Optional.of(finalSong);
    }

    private void addSilence(
            double duration,
            double totalDelta,
            File finalSong,
            ExecutorService executor,
            ArrayList<Future<Runnable>> futures) {
        if (duration <= 0.0) {
            return;
        }
        File renderedSilence = cacheManager.createSilenceCache();
        futures.add(executor.submit(() -> {
            resampler.resampleSilence(getResamplerPath(), renderedSilence, duration);
            return () -> {
                wavtool.addSilence(
                        duration,
                        totalDelta,
                        renderedSilence,
                        finalSong,
                        false);
            };
        }));
    }

    private void addFinalSilence(
            double duration,
            double totalDelta,
            File finalSong,
            ExecutorService executor,
            ArrayList<Future<Runnable>> futures) {
        // The final note must be passed to the wavtool.
        double trueDuration = Math.max(duration, 0);
        File renderedSilence = cacheManager.createSilenceCache();
        futures.add(executor.submit(() -> {
            resampler.resampleSilence(getResamplerPath(), renderedSilence, trueDuration);
            return () -> wavtool.addSilence(
                    trueDuration,
                    totalDelta,
                    renderedSilence,
                    finalSong,
                    true);
        }));
    }

    // Returns empty string if there is no nearby (within DEFAULT_NOTE_DURATION) previous note.
    private static String getNearbyPrevLyric(Optional<Note> prev) {
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
            Optional<Note> note,
            Voicebank voicebank,
            Optional<Double> nextPreutter,
            double tempo) {
        if (note.isEmpty() || nextPreutter.isEmpty()) {
            return false;
        }

        // Return false if current note cannot be rendered.
        if (voicebank.getLyricConfig(note.get().getTrueLyric()).isEmpty()) {
            return false;
        }

        double scaleFactor = 125.0 / tempo;
        double scaledGap = (note.get().getLength() - note.get().getDuration()) * scaleFactor;
        return scaledGap <= nextPreutter.get();
    }

    // Wait up to 1 second for a file to be readable.
    private static void waitUntilFileReadable(File file) throws InterruptedException {
        for (int i = 0; i < 10; i++) {
            if (file.canRead()) {
                if (i > 0) {
                    System.out.println("Note appeared at " + (i * 100) + "ms.");
                }
                break;
            }
            Thread.sleep(100); // Wait for 100ms.
        }
    }
}
