package com.utsusynth.utsu.view.song;

import com.google.inject.Inject;
import com.utsusynth.utsu.common.quantize.Quantizer;
import com.utsusynth.utsu.common.quantize.Scaler;
import com.utsusynth.utsu.common.utils.PitchUtils;
import com.utsusynth.utsu.common.utils.RoundUtils;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.DoubleExpression;
import javafx.beans.binding.IntegerExpression;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Orientation;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Collections;

/** The background track of the song editor. */
public class Track {
    private final IntegerProperty minVisibleColumn;
    private final IntegerProperty maxVisibleColumn;
    private final Scaler scaler;

    private int numMeasures = 0;
    private ListView<String> noteTrack;
    private ListView<String> dynamicsTrack;

    @Inject
    public Track(Scaler scaler) {
        minVisibleColumn = new SimpleIntegerProperty(Integer.MIN_VALUE);
        maxVisibleColumn = new SimpleIntegerProperty(Integer.MAX_VALUE);
        this.scaler = scaler;
    }

    public void setNumMeasures(int numMeasures) {
        this.numMeasures = numMeasures;
        setNumMeasures(noteTrack, numMeasures);
        setNumMeasures(dynamicsTrack, numMeasures);
    }

    private void setNumMeasures(ListView<String> updateMe, int numMeasures) {
        if (updateMe != null) {
            int numCols = (numMeasures + 1) * 4;
            double trackWidth = numCols * scaler.scaleX(Quantizer.COL_WIDTH).get();
            updateMe.setPrefWidth(trackWidth);
            updateMe.setItems(
                    FXCollections.observableArrayList(Collections.nCopies(numCols, "")));
        }
    }

    public void showMeasures(int minMeasure, int maxMeasure) {
        int minColumn = minMeasure * 4;
        int maxColumn = maxMeasure * 4 + 3;
        if (minVisibleColumn.get() == minColumn && maxVisibleColumn.get() == maxColumn) {
            return; // Do nothing if the rendered columns haven't changed.
        }
        minVisibleColumn.set(minMeasure * 4);
        maxVisibleColumn.set(maxMeasure * 4 + 3);
        if (noteTrack != null) {
            noteTrack.refresh();
        }
        if (dynamicsTrack != null) {
            dynamicsTrack.refresh();
        }
    }

    private ListView<String> createNoteTrack() {
        double colWidth = scaler.scaleX(Quantizer.COL_WIDTH).get();
        double rowHeight = scaler.scaleY(Quantizer.ROW_HEIGHT).get();

        noteTrack = new ListView<>();
        noteTrack.setEditable(false);
        dynamicsTrack.setFixedCellSize(colWidth);
        noteTrack.setOrientation(Orientation.HORIZONTAL);
        noteTrack.setCellFactory(source -> new ListCell<>() {
            {
                setStyle("-fx-padding: 0px;");
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (getIndex() < minVisibleColumn.get() || getIndex() > maxVisibleColumn.get()) {
                    return; // Only render visible measures.
                }

                VBox column = new VBox();
                //column.setPrefSize(colWidth, rowHeight * PitchUtils.TOTAL_NUM_PITCHES);
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
                        column.getChildren().add(newCell);
                        rowNum++;
                    }
                }
                setGraphic(column);
            }
        });
        setNumMeasures(noteTrack, numMeasures);
        return noteTrack;
    }

    public ListView<String> getNoteTrack() {
        if (noteTrack == null) {
            return createNoteTrack();
        }
        return noteTrack;
    }

    private ListView<String> createDynamicsTrack() {
        double colWidth = scaler.scaleX(Quantizer.COL_WIDTH).get();
        double rowHeight = 50;

        dynamicsTrack = new ListView<>();
        dynamicsTrack.setEditable(false);
        dynamicsTrack.setFixedCellSize(colWidth);
        dynamicsTrack.setOrientation(Orientation.HORIZONTAL);
        dynamicsTrack.setCellFactory(source -> new ListCell<>() {
            {
                setStyle("-fx-padding: 0px;");
            }
            @Override
            protected void updateItem (String item, boolean empty) {
                super.updateItem(item, empty);
                if (getIndex() < minVisibleColumn.get() || getIndex() > maxVisibleColumn.get()) {
                    return; // Only render visible measures.
                }

                VBox newDynamics = new VBox();
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
                newDynamics.getChildren().addAll(topCell, bottomCell);
                setGraphic(newDynamics);
            }
        });
        setNumMeasures(dynamicsTrack, numMeasures);
        return dynamicsTrack;
    }

    public ListView<String> getDynamicsTrack() {
        if (dynamicsTrack == null) {
            return createDynamicsTrack();
        }
        return dynamicsTrack;
    }
}
