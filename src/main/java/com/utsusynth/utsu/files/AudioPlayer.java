package com.utsusynth.utsu.files;

import com.google.inject.Inject;
import com.utsusynth.utsu.common.quantize.Quantizer;
import com.utsusynth.utsu.common.utils.PitchUtils;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

/** Common library for playback of all audio files. Currently not a singleton. */
public class AudioPlayer {
    private final AssetManager assetManager;

    private MediaPlayer pianoPlayer;
    private int lastPianoNote = 0;

    @Inject
    public AudioPlayer(AssetManager assetManager) {
        this.assetManager = assetManager;
    }

    /**
     * Plays a piano note at a certain pitch.
     * @param volume A value between 0 and 1.
     * @param alwaysPlay If false, a note that's already in progress won't be replayed.
     */
    public void playPianoNote(String pitch, double volume, boolean alwaysPlay) {
        int noteNum = PitchUtils.pitchToNoteNum(pitch);
        if (noteNum < 24 || noteNum > 107) {
            return; // Can only play C1->B7.
        }
        if (noteNum == lastPianoNote && !alwaysPlay) {
            return;
        }
        Media media = new Media(assetManager.getPianoFile().toURI().toString());
        lastPianoNote = noteNum;
        pianoPlayer = new MediaPlayer(media);
        pianoPlayer.setStartTime(Duration.millis(500 * (noteNum - 24)));
        pianoPlayer.setStopTime(Duration.millis(500 * (noteNum - 23) - 50));
        pianoPlayer.setVolume(volume);
        pianoPlayer.setOnEndOfMedia(pianoPlayer::stop);
        pianoPlayer.setOnStopped(pianoPlayer::dispose);
        pianoPlayer.play();
    }
}
