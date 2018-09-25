package com.utsusynth.utsu.view.song;

import java.util.Collection;
import java.util.TreeSet;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.utsusynth.utsu.common.RegionBounds;
import com.utsusynth.utsu.common.quantize.Quantizer;
import com.utsusynth.utsu.common.quantize.Scaler;
import com.utsusynth.utsu.common.utils.PitchUtils;
import com.utsusynth.utsu.common.utils.RoundUtils;
import com.utsusynth.utsu.view.song.note.Note;
import javafx.animation.Animation.Status;
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
    private final TreeSet<Note> highlighted;
    private final TranslateTransition playback;

    private Line startBar;
    private Line endBar;
    private Group bars;

    @Inject
    public PlaybackBarManager(Scaler scaler) {
        this.scaler = scaler;
        highlighted = new TreeSet<>();
        playback = new TranslateTransition();
        clear();
    }

    Group getElement() {
        return bars;
    }

    // Sends the playback bar across the part of the song that plays.
    void startPlayback(Duration duration, RegionBounds playRegion) {
        if (duration != Duration.UNKNOWN && duration != Duration.INDEFINITE) {
            // Create a playback bar.
            double barX = scaler.scalePos(playRegion.getMinMs());
            Line playBar = new Line(barX, 0, barX, scaler.scaleY(totalHeight));
            playBar.getStyleClass().addAll("playback-bar");
            bars.getChildren().add(playBar);

            // Move the playback bar as the song plays.
            playback.stop();
            playback.setNode(playBar);
            playback.setDuration(duration);
            playback.setToX(scaler.scaleX(playRegion.getMaxMs() - playRegion.getMinMs()));
            playback.setInterpolator(Interpolator.LINEAR);
            playback.statusProperty().addListener((obs, oldStatus, newStatus) -> {
                if (newStatus == Status.STOPPED) {
                    bars.getChildren().remove(playBar);
                }
            });
            playback.play();
        }
    }

    void pausePlayback() {
        playback.pause(); // Does nothing if animation not playing.
    }

    void resumePlayback() {
        if (playback.getStatus() == Status.PAUSED) {
            playback.play(); // Does nothing if animation not paused.
        }
    }

    // Removes the playback bar.
    void stopPlayback() {
        playback.stop();
    }

    /** Adds a specific note to highlighted set and adjust playback bars. */
    void highlightNote(Note highlightMe) {
        highlighted.add(highlightMe);
        highlightMe.setHighlighted(true);
    }

    /** Highlight an exact region and any notes within that region. */
    void highlightRegion(RegionBounds region, Collection<Note> allNotes) {
        clearHighlights();
        if (region.equals(RegionBounds.INVALID)) {
            return;
        }

        // Add start and stop bars to the track at the correct location.
        bars.getChildren().addAll(startBar, endBar);
        startBar.setTranslateX(scaler.scalePos(region.getMinMs()));
        endBar.setTranslateX(scaler.scalePos(region.getMaxMs()));

        // Highlight all notes within the add region.
        for (Note note : allNotes) {
            if (region.intersects(note.getValidBounds())) {
                // These operations are idempotent.
                highlighted.add(note);
                note.setHighlighted(true);
            }
        }
    }

    /** Highlights all notes and places playback bars at their edges. */
    void highlightAll(Collection<Note> allNotes) {
        clearHighlights();
        if (allNotes.isEmpty()) {
            return;
        }

        for (Note note : allNotes) {
            highlighted.add(note);
            note.setHighlighted(true);
        }

        // Add start and stop bars to the track at the correct location.
        bars.getChildren().addAll(startBar, endBar);
        startBar.setTranslateX(scaler.scalePos(highlighted.first().getValidBounds().getMinMs()));
        endBar.setTranslateX(scaler.scalePos(highlighted.last().getValidBounds().getMaxMs()));
    }

    /** Aligns playback bar to reflect actual highlighted notes. */
    void realign() {
        if (highlighted.isEmpty()) {
            clearHighlights();
        } else {
            // Add start and stop bars to the track if necessary.
            if (!bars.getChildren().contains(startBar)) {
                bars.getChildren().add(startBar);
            }
            if (!bars.getChildren().contains(endBar)) {
                bars.getChildren().add(endBar);
            }
            startBar.setTranslateX(
                    scaler.scalePos(highlighted.first().getValidBounds().getMinMs()));
            endBar.setTranslateX(scaler.scalePos(highlighted.last().getValidBounds().getMaxMs()));
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

    void setCursor(int positionMs) {
        clearHighlights();
        bars.getChildren().add(startBar);
        startBar.setTranslateX(scaler.scalePos(positionMs));
    }

    int getCursorPosition() {
        if (bars.getChildren().contains(startBar)) {
            return Math.max(0, RoundUtils.round(scaler.unscalePos(startBar.getTranslateX())));
        }
        return 0;
    }

    RegionBounds getPlayableRegion() {
        int endPosition = Integer.MAX_VALUE;
        if (bars.getChildren().contains(endBar)) {
            endPosition = RoundUtils.round(scaler.unscalePos(endBar.getTranslateX()));
        }
        return new RegionBounds(getCursorPosition(), endPosition);
    }

    RegionBounds getSelectedRegion() {
        if (highlighted.isEmpty()) {
            return RegionBounds.INVALID;
        }
        return highlighted.first().getValidBounds().mergeWith(highlighted.last().getValidBounds());
    }

    boolean isExclusivelyHighlighted(Note note) {
        return highlighted.size() == 1 && highlighted.contains(note);
    }

    boolean isHighlighted(Note note) {
        return highlighted.contains(note);
    }

    ImmutableList<Note> getHighlightedNotes() {
        return ImmutableList.copyOf(highlighted);
    }
}
