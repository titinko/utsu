package com.utsusynth.utsu.view.song.playback;

import java.util.Collection;
import java.util.TreeSet;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.utsusynth.utsu.common.utils.RegionBounds;
import com.utsusynth.utsu.common.quantize.Scaler;
import com.utsusynth.utsu.common.utils.PitchUtils;
import com.utsusynth.utsu.common.utils.RoundUtils;
import com.utsusynth.utsu.view.song.note.Note;
import javafx.animation.*;
import javafx.animation.Animation.Status;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Group;
import javafx.util.Duration;

/**
 * Keeps track of what notes are currently highlighted.
 */
public class PlaybackManager {
    private final Scaler scaler;
    private final TreeSet<Note> highlighted; // All highlighted notes.
    private final BooleanProperty isAnythingHighlighted;
    private final Timeline playback;

    private final StartBar startBar;
    private final EndBar endBar;
    private final PlayBar playBar;
    private PlaybackCallback callback;

    private Group bars;

    @Inject
    public PlaybackManager(StartBar startBar, EndBar endBar, PlayBar playBar, Scaler scaler) {
        this.startBar = startBar;
        this.endBar = endBar;
        this.playBar = playBar;

        this.scaler = scaler;
        highlighted = new TreeSet<>();
        isAnythingHighlighted = new SimpleBooleanProperty(false);
        playback = new Timeline();
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
        if (callback != null && duration != Duration.UNKNOWN && duration != Duration.INDEFINITE) {
            playBar.clearListeners();
            playBar.setX(scaler.scalePos(playRegion.getMinMs()));
            callback.setBar(playBar);

            playback.stop();
            playback.getKeyFrames().clear();
            double finalX = scaler.scalePos(playRegion.getMaxMs());
            playback.getKeyFrames().add(
                    new KeyFrame(duration, new KeyValue(playBar.xProperty(), finalX)));
            playback.play();
            playback.setOnFinished(event -> callback.removeBar(playBar));

            playBar.xProperty().addListener((obs, oldValue, newValue) -> {
                if (callback != null && playback.getStatus() == Status.RUNNING) {
                    callback.readjust(playBar);
                }
            });
            return playBar.xProperty();
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
            startBar.setX(scaler.scalePos(region.getMinMs()));
            callback.setBar(startBar);
            endBar.setX(scaler.scalePos(region.getMaxMs()));
            callback.setBar(endBar);
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
            startBar.setX(
                    scaler.scalePos(highlighted.first().getValidBounds().getMinMs()));
            callback.setBar(startBar);
            endBar.setX(scaler.scalePos(highlighted.last().getValidBounds().getMaxMs()));
            callback.setBar(endBar);
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
                startBar.setX(
                        scaler.scalePos(highlighted.first().getValidBounds().getMinMs()));
                callback.setBar(startBar);
                endBar.setX(
                        scaler.scalePos(highlighted.last().getValidBounds().getMaxMs()));
                callback.setBar(endBar);
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
            callback.removeBar(startBar);
            callback.removeBar(endBar);
        }
    }

    public void setCursor(int positionMs) {
        clearHighlights();
        if (callback != null) {
            startBar.setX(scaler.scalePos(positionMs));
            callback.setBar(startBar);
        }
    }

    public int getCursorPosition() {
        if (!startBar.getColumns().isEmpty()) {
            return Math.max(0, RoundUtils.round(scaler.unscalePos(startBar.getStartX())));
        }
        return 0;
    }

    public RegionBounds getPlayableRegion() {
        int endPosition = Integer.MAX_VALUE;
        if (!endBar.getColumns().isEmpty()) {
            endPosition = RoundUtils.round(scaler.unscalePos(endBar.getStartX()));
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
