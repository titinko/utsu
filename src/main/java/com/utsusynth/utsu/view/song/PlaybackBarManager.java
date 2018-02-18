package com.utsusynth.utsu.view.song;

import java.util.Collection;
import java.util.HashSet;
import com.google.inject.Inject;
import com.utsusynth.utsu.common.PitchUtils;
import com.utsusynth.utsu.common.RegionBounds;
import com.utsusynth.utsu.common.quantize.Quantizer;
import com.utsusynth.utsu.common.quantize.Scaler;
import com.utsusynth.utsu.view.song.note.Note;
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
    private final HashSet<Note> highlighted;
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
            double barX = bars.getChildren().contains(startBar) ? startBar.getTranslateX() : 0;
            Line playBar = new Line(barX, 0, barX, scaler.scaleY(totalHeight));
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

    void highlightTo(Note highlightToMe, Collection<Note> allNotes) {
        RegionBounds noteBounds = highlightToMe.getValidBounds();
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
        for (Note note : allNotes) {
            if (addRegion.intersects(note.getValidBounds())) {
                // These operations are idempotent.
                highlighted.add(note);
                note.setHighlighted(true);
            }
        }
    }

    void refreshHighlights(Note refreshMe) {
        int startBarX = (int) Math.round(scaler.unscaleX(startBar.getTranslateX()));
        int endBarX = (int) Math.round(scaler.unscaleX(endBar.getTranslateX()));
        RegionBounds highlightedRegion = new RegionBounds(startBarX, endBarX);

        if (highlightedRegion.intersects(refreshMe.getValidBounds())) {
            // This operation is idempotent.
            highlighted.add(refreshMe);
            refreshMe.setHighlighted(true);
        } else if (highlighted.contains(refreshMe)) {
            highlighted.remove(refreshMe);
            refreshMe.setHighlighted(false);
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
        for (Note note : highlighted) {
            note.setHighlighted(false);
        }
        highlighted.clear();
        bars.getChildren().removeAll(startBar, endBar);
    }

    boolean isExclusivelyHighlighted(Note note) {
        return highlighted.size() == 1 && highlighted.contains(note);
    }

    RegionBounds getRegionBounds() {
        int startBarX = (int) Math.round(scaler.unscaleX(startBar.getTranslateX()));
        int endBarX = (int) Math.round(scaler.unscaleX(endBar.getTranslateX()));
        if (bars.getChildren().contains(startBar) && bars.getChildren().contains(endBar)) {
            return new RegionBounds(startBarX, endBarX);
        } else if (bars.getChildren().contains(startBar)) {
            return new RegionBounds(startBarX, Integer.MAX_VALUE);
        } else if (bars.getChildren().contains(endBar)) {
            return new RegionBounds(0, endBarX);
        } else {
            return RegionBounds.WHOLE_SONG;
        }
    }
}
