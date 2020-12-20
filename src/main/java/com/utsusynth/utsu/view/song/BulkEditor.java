package com.utsusynth.utsu.view.song;

import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;

public class BulkEditor {
    public VBox createEnvelopeEditor(ReadOnlyDoubleProperty width, ReadOnlyDoubleProperty height) {
        AnchorPane topCell = new AnchorPane();
        topCell.getStyleClass().add("dynamics-top-cell");
        topCell.prefWidthProperty().bind(width);
        topCell.prefHeightProperty().bind(height.divide(2));
        AnchorPane bottomCell = new AnchorPane();
        bottomCell.getStyleClass().add("dynamics-bottom-cell");
        bottomCell.prefWidthProperty().bind(width);
        bottomCell.prefHeightProperty().bind(height.divide(2));
        return new VBox(topCell, bottomCell);
    }
}
