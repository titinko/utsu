package com.utsusynth.utsu.view.song.track;

import javafx.scene.layout.Pane;

public interface TrackCallback {
    Pane createNoteColumn(int colNum);

    Pane createDynamicsColumn(int colNum);
}
