package com.utsusynth.utsu.view.song;

import com.google.inject.Inject;
import com.utsusynth.utsu.common.quantize.Quantizer;
import com.utsusynth.utsu.common.quantize.Scaler;
import com.utsusynth.utsu.common.utils.PitchUtils;
import javafx.beans.binding.DoubleExpression;
import javafx.collections.FXCollections;
import javafx.geometry.Orientation;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;

/** The background track of the song editor. */
public class Track {
    private final Quantizer quantizer;
    private final Scaler scaler;

    private DoubleExpression trackWidth;
    private ListView<String> noteTrack;
    private ListView<String> dynamicsTrack;

    @Inject
    public Track(Quantizer quantizer, Scaler scaler) {
        this.quantizer = quantizer;
        this.scaler = scaler;
    }

    public void initialize(DoubleExpression trackWidth) {
        this.trackWidth = trackWidth;
    }

    public ListView<String> createNoteTrack() {
        double colWidth = scaler.scaleX(Quantizer.COL_WIDTH).get();
        double rowHeight = scaler.scaleY(Quantizer.ROW_HEIGHT).get();

        noteTrack = new ListView<>();
        noteTrack.setEditable(false);
        noteTrack.prefWidthProperty().bind(trackWidth);
        noteTrack.setPrefHeight(rowHeight * PitchUtils.TOTAL_NUM_PITCHES);
        noteTrack.setOrientation(Orientation.HORIZONTAL);
        noteTrack.setCellFactory(source -> new ListCell<>() {
            {
                setStyle("-fx-padding: 0px;");
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);

                Pane quarterNote = new Pane();
                quarterNote.setPrefSize(colWidth, rowHeight * PitchUtils.TOTAL_NUM_PITCHES);
                System.out.println(getIndex());
                int rowNum = 0;
                for (int octave = 7; octave > 0; octave--) {
                    for (String pitch : PitchUtils.REVERSE_PITCHES) {
                        // Add row to track.
                        Pane newCell = new Pane();
                        newCell.setPrefSize(colWidth, rowHeight);
                        newCell.getStyleClass().add("track-cell");
                        if (getIndex() >= -2) {
                            newCell.getStyleClass()
                                    .add(pitch.endsWith("#") ? "black-key" : "white-key");
                        } else {
                            newCell.getStyleClass().add("gray-key");
                        }
                        if (getIndex() % 4 == 0) {
                            newCell.getStyleClass().add("measure-start");
                        } else if (getIndex() % 4 == 3) {
                            newCell.getStyleClass().add("measure-end");
                        }
                        newCell.setTranslateY(rowHeight * rowNum);
                        quarterNote.getChildren().add(newCell);
                        rowNum++;
                    }
                }
                setGraphic(quarterNote);
            }
        });
        noteTrack.setItems(FXCollections.observableArrayList("")); // Force generation.
        return noteTrack;
    }

    public ListView<String> getNoteTrack() {
        if (noteTrack == null) {
            return createNoteTrack();
        }
        return noteTrack;
    }

    public ListView<String> createDynamicsTrack() {
        double colWidth = scaler.scaleX(Quantizer.COL_WIDTH).get();
        double rowHeight = 50;

        dynamicsTrack = new ListView<>();
        dynamicsTrack.setEditable(false);
        dynamicsTrack.prefWidthProperty().bind(trackWidth);
        dynamicsTrack.setPrefHeight(rowHeight * 2);
        dynamicsTrack.setOrientation(Orientation.HORIZONTAL);
        dynamicsTrack.setCellFactory(source -> new ListCell<>() {
            {
                setStyle("-fx-padding: 0px;");
            }
            @Override
            protected void updateItem (String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);

                Pane newDynamics = new Pane();
                newDynamics.setPrefSize(colWidth, rowHeight * 2);
                AnchorPane topCell = new AnchorPane();
                topCell.setPrefSize(colWidth, rowHeight);
                topCell.getStyleClass().add("dynamics-top-cell");
                if (getIndex() % 4 == 0) {
                    topCell.getStyleClass().add("measure-start");
                }

                AnchorPane bottomCell = new AnchorPane();
                bottomCell.setPrefSize(colWidth, rowHeight);
                bottomCell.getStyleClass().add("dynamics-bottom-cell");
                if (getIndex() % 4 == 0) {
                    bottomCell.getStyleClass().add("measure-start");
                }
                bottomCell.setTranslateY(rowHeight);
                newDynamics.getChildren().addAll(topCell, bottomCell);
                setGraphic(newDynamics);
            }
        });
        dynamicsTrack.setItems(FXCollections.observableArrayList("")); // Force generation.
        return dynamicsTrack;
    }

    public ListView<String> getDynamicsTrack() {
        if (dynamicsTrack == null) {
            return createDynamicsTrack();
        }
        return dynamicsTrack;
    }
}
