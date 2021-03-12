package com.utsusynth.utsu.view.song;

import javafx.scene.Node;

/** Represents a single item that can be drawn in the track. */
public interface TrackItem {
    /** Start position of this item. */
    double getStartX();

    /** Width, in pixels, of this item. */
    double getWidth();

    /** Element to render for this item. */
    Node getElement();
}
