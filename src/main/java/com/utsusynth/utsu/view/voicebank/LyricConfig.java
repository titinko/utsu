package com.utsusynth.utsu.view.voicebank;

import javafx.scene.layout.StackPane;

/** Graph view of a single lyric config in the voicebank editor. */
public class LyricConfig {
    private final StackPane[] cells;

    LyricConfig(LyricConfigCallback callback, StackPane... cells) {
        this.cells = cells;
        for (StackPane cell : cells) {
            cell.setOnMousePressed(event -> {
                callback.highlight(this);
            });
        }
    }

    /** Should not be called from within LyricConfig. */
    public void setHighlighted(boolean highlighted) {
        for (StackPane cell : cells) {
            cell.getStyleClass().set(2, highlighted ? "highlighted" : "not-highlighted");
        }
    }

    public StackPane[] getCells() {
        return cells;
    }
}
