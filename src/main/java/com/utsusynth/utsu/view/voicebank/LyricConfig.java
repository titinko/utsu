package com.utsusynth.utsu.view.voicebank;

import javafx.scene.layout.StackPane;

/** Represents a single lyric config in the voicebank editor. */
public class LyricConfig {
    private final StackPane[] cells;

    LyricConfig(StackPane... cells) {
        if (cells.length != 7) {
            // TODO: Throw error.
        }
        this.cells = cells;
    }

    public StackPane[] getCells() {
        return cells;
    }
}
