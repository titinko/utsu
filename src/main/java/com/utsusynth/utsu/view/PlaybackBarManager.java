package com.utsusynth.utsu.view;

import java.util.LinkedList;
import com.google.inject.Inject;
import com.utsusynth.utsu.common.PitchUtils;
import com.utsusynth.utsu.common.quantize.Quantizer;
import com.utsusynth.utsu.common.quantize.Scaler;
import com.utsusynth.utsu.view.note.TrackNote;
import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.scene.Group;
import javafx.scene.shape.Line;
import javafx.util.Duration;

/**
 * Keeps track of what notes are currently highlighted.
 */
public class PlaybackBarManager {
    private final Scaler scaler;
    private final LinkedList<TrackNote> highlighted;
    private final TranslateTransition playback;

    private Line startBar;
    private Line endBar;
    private Group bars;

    @Inject
    public PlaybackBarManager(Scaler scaler) {
        this.scaler = scaler;
        highlighted = new LinkedList<>();
        playback = new TranslateTransition();
        clear();
    }

    Group getElement() {
        return bars;
    }

    // Sends the playback bar across the part of the song that plays.
    void startPlayback(Duration duration, double tempo) {
        if (duration != Duration.UNKNOWN && duration != Duration.INDEFINITE) {
            // Create a playback bar.
            int totalHeight = PitchUtils.TOTAL_NUM_PITCHES * Quantizer.ROW_HEIGHT;
            Line playBar = new Line(0, 0, 0, scaler.scaleY(totalHeight));
            playBar.getStyleClass().addAll("playback-bar");
            bars.getChildren().add(playBar);

            // Move the playback bar as the song plays.
            playback.stop();
            playback.setDuration(duration);
            playback.setNode(playBar);
            double numBeats = tempo * duration.toMinutes();
            playback.setByX(numBeats * scaler.scaleX(Quantizer.COL_WIDTH));
            playback.setInterpolator(Interpolator.LINEAR);
            playback.setOnFinished(action -> {
                bars.getChildren().remove(playBar);
            });
            playback.play();
        }
    }

    void highlightTo(TrackNote note) {
        if (highlighted.isEmpty()) {
            // Only the current note needs to be highlighted.
            highlighted.add(note);
            note.setHighlighted(true);
            // Set start and stop bars.
            startBar.setTranslateX(scaler.scaleX(note.getAbsPosition()));
            if (!bars.getChildren().contains(startBar)) {
                bars.getChildren().add(startBar);
            }
            endBar.setTranslateX(scaler.scaleX(note.getAbsPosition() + note.getDuration()));
            if (!bars.getChildren().contains(endBar)) {
                bars.getChildren().add(endBar);
            }
        } else {
            // TODO: Add this note and everything in between to highlighted sequence.
        }
    }

    void clear() {
        playback.stop(); // Stop any ongoing playback.
        bars = new Group();
        clearHighlights();

        // Recreate start and end bars, as scale might have changed.
        int totalHeight = PitchUtils.TOTAL_NUM_PITCHES * Quantizer.ROW_HEIGHT;
        startBar = new Line(0, 0, 0, scaler.scaleY(totalHeight));
        startBar.getStyleClass().add("start-bar");
        endBar = new Line(0, 0, 0, scaler.scaleY(totalHeight));
        endBar.getStyleClass().add("end-bar");
    }

    void clearHighlights() {
        for (TrackNote note : highlighted) {
            note.setHighlighted(false);
        }
        highlighted.clear();
        bars.getChildren().removeAll(startBar, endBar);
    }

    boolean isHighlighted(TrackNote note) {
        return highlighted.contains(note);
    }
}
