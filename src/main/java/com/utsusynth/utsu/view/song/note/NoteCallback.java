package com.utsusynth.utsu.view.song.note;

import com.utsusynth.utsu.common.utils.RegionBounds;
import com.utsusynth.utsu.view.song.DragHandler;
import javafx.scene.layout.AnchorPane;

/**
 * A way of communicating TrackNote information back to its parent Track.
 */
public interface NoteCallback {
    void highlightExclusive(Note note);

    void highlightInclusive(Note note);

    void realignHighlights();

    boolean isExclusivelyHighlighted(Note note);

    /** Only catches current note. */
    void updateNote(Note note);

    /** Catches all highlighted notes, if applicable. */
    void moveNote(Note note, int positionDelta, int rowDelta);

    /** Catches all highlighted notes, if applicable. */
    void recordNoteMovement(Note note, int positionDelta, int rowDelta);

    /** Catches all highlighted notes, if applicable. */
    void copyNote(Note note);

    /** Catches all highlighted notes, if applicable. */
    void deleteNote(Note note);

    /** Catches all highlighted notes, if applicable. */
    RegionBounds getBounds(Note note);

    /** Catches all highlighted notes, if applicable. */
    int getLowestRow(Note note);

    /** Catches all highlighted notes, if applicable. */
    int getHighestRow(Note note);

    void recordAction(Runnable redoAction, Runnable undoAction);

    boolean hasVibrato(int position);

    void setHasVibrato(int position, boolean hasVibrato);

    void openNoteProperties(Note note);

    void openLyricConfig(Note note);

    void startDrag(DragHandler dragHandler);

    void clearCache(Note note);

    AnchorPane getLyricPane();
}
