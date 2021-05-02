package com.utsusynth.utsu.view.song.playback;

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
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.shape.Line;
import javafx.util.Duration;

/**
 * Keeps track of what notes are currently highlighted.
 */
public class PlaybackManager {
    private static final int TOTAL_HEIGHT = PitchUtils.TOTAL_NUM_PITCHES * Quantizer.ROW_HEIGHT;

    private final Scaler scaler;
    private final TreeSet<Note> highlighted; // All highlighted notes.
    private final BooleanProperty isAnythingHighlighted;
    private final TranslateTransition playback;

    private final StartBar startBarItem;
    private final EndBar endBarItem;
    private PlaybackCallback callback;

    private Group bars;

    @Inject
    public PlaybackManager(StartBar startBarItem, EndBar endBarItem, Scaler scaler) {
        this.startBarItem = startBarItem;
        this.endBarItem = endBarItem;

        this.scaler = scaler;
        highlighted = new TreeSet<>();
        isAnythingHighlighted = new SimpleBooleanProperty(false);
        playback = new TranslateTransition();
        clear();
    }

    public void initialize(PlaybackCallback callback) {
        this.callback = callback;
    }

    public Group getElement() {
        return bars;
    }

    /**
     * Sends the playback bar across the part of the song that plays.
     *
     * @return A double binding of the playback bar's current x-value.
     */
    public DoubleProperty startPlayback(Duration duration, RegionBounds playRegion) {
        if (duration != Duration.UNKNOWN && duration != Duration.INDEFINITE) {
            // Create a playback bar.
            double barX = scaler.scalePos(playRegion.getMinMs()).get();
            Line playBar = new Line(barX, 0, barX, scaler.scaleY(TOTAL_HEIGHT).get());
            playBar.getStyleClass().add("playback-bar");

            // Add a backing bar to handle a Windows-specific optimization issue.
            Node playBarNode;
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                Line backingBar = new Line(barX, 0, barX, scaler.scaleY(TOTAL_HEIGHT).get());
                backingBar.getStyleClass().add("playback-backing-bar");
                playBarNode = new Group(playBar, backingBar);
            } else {
                playBarNode = playBar;
            }
            bars.getChildren().add(playBarNode);

            // Move the playback bar as the song plays.
            playback.stop();
            playback.setNode(playBarNode);
            playback.setDuration(duration);
            playback.setToX(
                    scaler.scaleX(playRegion.getMaxMs() - playRegion.getMinMs()).get());
            playback.setInterpolator(Interpolator.LINEAR);
            playback.statusProperty().addListener((obs, oldStatus, newStatus) -> {
                if (newStatus == Status.STOPPED) {
                    bars.getChildren().remove(playBarNode);
                }
            });
            playback.play();
            return playBarNode.translateXProperty();
        }
        // Return null if no playback bar created.
        return null;
    }

    public void pausePlayback() {
        playback.pause(); // Does nothing if animation not playing.
    }

    public void resumePlayback() {
        if (playback.getStatus() == Status.PAUSED) {
            playback.play(); // Does nothing if animation not paused.
        }
    }

    // Removes the playback bar.
    public void stopPlayback() {
        playback.stop();
    }

    /**
     * Adds a specific note to highlighted set and adjust playback bars.
     */
    public void highlightNote(Note highlightMe) {
        highlighted.add(highlightMe);
        isAnythingHighlighted.set(true);
        highlightMe.setHighlighted(true);
    }

    /**
     * Highlight an exact region and any notes within that region.
     */
    public void highlightRegion(RegionBounds region, Collection<Note> allNotes) {
        clearHighlights();
        if (region.equals(RegionBounds.INVALID)) {
            return;
        }

        // Add start and stop bars to the track at the correct location.
        if (callback != null) {
            startBarItem.setX(scaler.scalePos(region.getMinMs()).get());
            callback.setBar(startBarItem);
            endBarItem.setX(scaler.scalePos(region.getMaxMs()).get());
            callback.setBar(endBarItem);
        }

        // Highlight all notes within the add region.
        for (Note note : allNotes) {
            if (region.intersects(note.getValidBounds())) {
                // These operations are idempotent.
                highlighted.add(note);
                note.setHighlighted(true);
            }
        }
        isAnythingHighlighted.set(!highlighted.isEmpty());
    }

    /**
     * Highlights all notes and places playback bars at their edges.
     */
    public void highlightAll(Collection<Note> allNotes) {
        clearHighlights();
        if (allNotes.isEmpty()) {
            return;
        }

        for (Note note : allNotes) {
            highlighted.add(note);
            note.setHighlighted(true);
        }
        isAnythingHighlighted.set(!highlighted.isEmpty());

        // Add start and stop bars to the track at the correct location.
        if (callback != null) {
            startBarItem.setX(
                    scaler.scalePos(highlighted.first().getValidBounds().getMinMs()).get());
            callback.setBar(startBarItem);
            endBarItem.setX(scaler.scalePos(highlighted.last().getValidBounds().getMaxMs()).get());
            callback.setBar(endBarItem);
        }
    }

    /**
     * Aligns playback bar to reflect actual highlighted notes.
     */
    public void realign() {
        if (highlighted.isEmpty()) {
            clearHighlights();
        } else {
            // Add start and stop bars to the track if necessary.
            if (callback != null) {
                startBarItem.setX(
                        scaler.scalePos(highlighted.first().getValidBounds().getMinMs()).get());
                callback.setBar(startBarItem);
                endBarItem.setX(
                        scaler.scalePos(highlighted.last().getValidBounds().getMaxMs()).get());
                callback.setBar(endBarItem);
            }
        }
    }

    public void clear() {
        playback.stop(); // Stop any ongoing playback.
        bars = new Group();
        clearHighlights();
    }

    public void clearHighlights() {
        for (Note note : highlighted) {
            note.setHighlighted(false);
        }
        highlighted.clear();
        isAnythingHighlighted.set(false);
        if (callback != null) {
            callback.removeBar(startBarItem);
            callback.removeBar(endBarItem);
        }
    }

    public void setCursor(int positionMs) {
        clearHighlights();
        if (callback != null) {
            startBarItem.setX(scaler.scalePos(positionMs).get());
            callback.setBar(startBarItem);
        }
    }

    public int getCursorPosition() {
        if (!startBarItem.getColumns().isEmpty()) {
            return Math.max(0, RoundUtils.round(scaler.unscalePos(startBarItem.getStartX())));
        }
        return 0;
    }

    public RegionBounds getPlayableRegion() {
        int endPosition = Integer.MAX_VALUE;
        if (!endBarItem.getColumns().isEmpty()) {
            endPosition = RoundUtils.round(scaler.unscalePos(endBarItem.getStartX()));
        }
        return new RegionBounds(getCursorPosition(), endPosition);
    }

    public RegionBounds getSelectedRegion() {
        if (highlighted.isEmpty()) {
            return RegionBounds.INVALID;
        }
        return highlighted.first().getValidBounds().mergeWith(highlighted.last().getValidBounds());
    }

    public int getLowestRow() {
        int lowest = 7 * PitchUtils.PITCHES.size() - 1;
        for (Note note : highlighted) {
            if (note.getRow() < lowest) {
                lowest = note.getRow();
            }
        }
        return lowest;
    }

    public int getHighestRow() {
        int highest = 0;
        for (Note note : highlighted) {
            if (note.getRow() > highest) {
                highest = note.getRow();
            }
        }
        return highest;
    }

    public boolean isExclusivelyHighlighted(Note note) {
        return highlighted.size() == 1 && highlighted.contains(note);
    }

    public boolean isHighlighted(Note note) {
        return highlighted.contains(note);
    }

    public BooleanProperty isAnythingHighlightedProperty() {
        return isAnythingHighlighted;
    }

    public ImmutableList<Note> getHighlightedNotes() {
        return ImmutableList.copyOf(highlighted);
    }
}
