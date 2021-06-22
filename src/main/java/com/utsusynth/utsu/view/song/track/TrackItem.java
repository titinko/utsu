package com.utsusynth.utsu.view.song.track;

import com.google.common.collect.ImmutableSet;
import javafx.scene.Node;

import java.util.HashSet;

/** Represents a single item that can be drawn in the track. */
public interface TrackItem {
    enum TrackItemType {
        NOTE,
        PITCHBEND,
        ENVELOPE,
        LYRIC, // Note lyrics and lyric entry boxes.
        PLAYBACK, // Start, end, and playback bars.
        DRAWING, // Drawn by mouse action, i.e. AddNoteBox and SelectionBox.
    }

    /** Used to decide what order to place items on the track. */
    TrackItemType getType();

    /** Start position of this item. */
    double getStartX();

    /** Width, in pixels, of this item. */
    double getWidth();

    /** Draw the item outside the track, with default settings. */
    Node redraw();

    /** Redraw this item with a certain offset. */
    Node redraw(double offsetX);

    /** Return every column where this item is currently drawn. */
    ImmutableSet<Integer> getColumns();

    /** Officially adds this item to a column but does not actually redraw the item. */
    void addColumn(int colNum);

    /** Officially removes this item from a column and cleans up fields related to them. */
    void removeColumn(int colNum);

    /** Clear list of columns and clears any fields related to them. */
    void removeAllColumns();
}
