package com.utsusynth.utsu.view;

import java.util.Collection;
import java.util.HashSet;
import com.google.inject.Inject;
import com.utsusynth.utsu.common.PitchUtils;
import com.utsusynth.utsu.common.RegionBounds;
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
    private static final int totalHeight = PitchUtils.TOTAL_NUM_PITCHES * Quantizer.ROW_HEIGHT;

    private final Scaler scaler;
    private final HashSet<TrackNote> highlighted;
    private final TranslateTransition playback;

    private Line startBar;
    private Line endBar;
    private Group bars;

    @Inject
    public PlaybackBarManager(Scaler scaler) {
        this.scaler = scaler;
        highlighted = new HashSet<>();
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

    void highlightTo(TrackNote highlightToMe, Collection<TrackNote> allNotes) {
        // All values maintain their current scale.
        RegionBounds noteBounds = highlightToMe.getBounds();
        RegionBounds addRegion;
        if (highlighted.isEmpty()) {
            // Add region is defined only by the note.
            addRegion = noteBounds;
            // Add start and stop bars to the track.
            if (!bars.getChildren().contains(startBar)) {
                bars.getChildren().add(startBar);
            }
            if (!bars.getChildren().contains(endBar)) {
                bars.getChildren().add(endBar);
            }
        } else {
            int startBarX = (int) Math.round(scaler.unscaleX(startBar.getTranslateX()));
            int endBarX = (int) Math.round(scaler.unscaleX(endBar.getTranslateX()));
            addRegion = new RegionBounds(startBarX, endBarX).mergeWith(noteBounds);
        }
        // Move startBar and endBar to the right locations.
        startBar.setTranslateX(scaler.scaleX(addRegion.getMinMs()));
        endBar.setTranslateX(scaler.scaleX(addRegion.getMaxMs()));

        // Highlight all notes within the add region.
        for (TrackNote note : allNotes) {
            if (addRegion.intersects(note.getBounds())) {
                // These operations are idempotent.
                highlighted.add(note);
                note.setHighlighted(true);
            }
        }
    }

    void clear() {
        playback.stop(); // Stop any ongoing playback.
        bars = new Group();
        clearHighlights();

        // Recreate start and end bars, as scale might have changed.
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

    boolean isExclusivelyHighlighted(TrackNote note) {
        return highlighted.size() == 1 && highlighted.contains(note);
    }
}
