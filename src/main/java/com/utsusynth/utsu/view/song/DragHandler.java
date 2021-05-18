package com.utsusynth.utsu.view.song;

/** This should be passed to the SongEditor by any element that starts a drag behavior. */
public interface DragHandler {
    void onDragged(double absoluteX, double absoluteY);

    void onDragReleased(double absoluteX, double absoluteY);
}
