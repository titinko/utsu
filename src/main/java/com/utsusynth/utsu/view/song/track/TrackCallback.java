package com.utsusynth.utsu.view.song.track;

import javafx.scene.layout.VBox;

public interface TrackCallback {
    VBox createNoteColumn(int colNum);

    VBox createDynamicsColumn(int colNum);
}
