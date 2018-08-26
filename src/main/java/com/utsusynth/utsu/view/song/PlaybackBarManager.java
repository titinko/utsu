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
import javafx.beans.property.SimpleObjectProperty;
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
    private final SimpleObjectProperty<RegionBounds> selectedRegion;

    private Line startBar;
    private Line endBar;
    private Group bars;

    @Inject
    public PlaybackBarManager(Scaler scaler) {
        this.scaler = scaler;
        highlighted = new HashSet<>();
        playback = new TranslateTransition();
        selectedRegion = new SimpleObjectProperty<>(RegionBounds.INVALID);
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
        if (highlighted.isEmpty()) {
            // Add region is defined only by the note.
            selectedRegion.set(noteBounds);
            // Add start and stop bars to the track.
            if (!bars.getChildren().contains(startBar)) {
                bars.getChildren().add(startBar);
            }
            if (!bars.getChildren().contains(endBar)) {
                bars.getChildren().add(endBar);
            }
        } else {
            selectedRegion.set(selectedRegion.get().mergeWith(noteBounds));
        }
        // Move startBar and endBar to the right locations.
        startBar.setTranslateX(scaler.scaleX(selectedRegion.get().getMinMs()));
        endBar.setTranslateX(scaler.scaleX(selectedRegion.get().getMaxMs()));

        // Highlight all notes within the add region.
        for (Note note : allNotes) {
            if (selectedRegion.get().intersects(note.getValidBounds())) {
                // These operations are idempotent.
                highlighted.add(note);
                note.setHighlighted(true);
            }
        }
    }

    /** Highlight an exact region and any notes within that region. */
    void highlightRegion(RegionBounds region, Collection<Note> allNotes) {
        clearHighlights();
        selectedRegion.set(region);

        // Add start and stop bars to the track at the correct location.
        bars.getChildren().addAll(startBar, endBar);
        startBar.setTranslateX(scaler.scaleX(selectedRegion.get().getMinMs()));
        endBar.setTranslateX(scaler.scaleX(selectedRegion.get().getMaxMs()));

        // Highlight all notes within the add region.
        for (Note note : allNotes) {
            if (selectedRegion.get().intersects(note.getValidBounds())) {
                // These operations are idempotent.
                highlighted.add(note);
                note.setHighlighted(true);
            }
        }
    }

    void refreshHighlights(Note refreshMe) {
        if (selectedRegion.get().intersects(refreshMe.getValidBounds())) {
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
        selectedRegion.set(RegionBounds.INVALID);
        bars.getChildren().removeAll(startBar, endBar);
    }

    boolean isExclusivelyHighlighted(Note note) {
        return highlighted.size() == 1 && highlighted.contains(note);
    }

    boolean isHighlighted(Note note) {
        return highlighted.contains(note);
    }

    RegionBounds getRegionBounds() {
        return selectedRegion.get();
    }
}
