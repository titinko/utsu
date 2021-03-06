package com.utsusynth.utsu.view.song;

import com.google.inject.Inject;
import com.utsusynth.utsu.common.quantize.Quantizer;
import com.utsusynth.utsu.common.quantize.Scaler;
import com.utsusynth.utsu.common.utils.PitchUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import java.util.Collections;
import java.util.Optional;

/** The background track of the song editor. */
public class Track {
    private final Scaler scaler;

    private int numMeasures = 0;
    private ListView<String> noteTrack;
    private ListView<String> dynamicsTrack;

    @Inject
    public Track(Scaler scaler) {
        this.scaler = scaler;
    }

    public int getNumMeasures() {
        return numMeasures;
    }

    public void setNumMeasures(int numMeasures) {
        if (numMeasures < 0 || numMeasures == this.numMeasures) {
            return; // Don't refresh unless you have to.
        }
        this.numMeasures = numMeasures;
        setNumMeasures(noteTrack, numMeasures);
        setNumMeasures(dynamicsTrack, numMeasures);
    }

    private void setNumMeasures(ListView<String> updateMe, int numMeasures) {
        if (updateMe != null) {
            int numCols = (numMeasures + 1) * 4;
            updateMe.setItems(
                    FXCollections.observableArrayList(Collections.nCopies(numCols, "")));
        }
    }

    private Optional<ScrollBar> getScrollBar(ListView<String> source, Orientation orientation) {
        if (source == null) {
            return Optional.empty();
        }
        for (Node node : noteTrack.lookupAll(".scroll-bar")) {
            if (!(node instanceof ScrollBar)) {
                continue;
            }
            ScrollBar scrollBar = (ScrollBar) node;
            if (scrollBar.getOrientation() == orientation) {
                return Optional.of(scrollBar);
            }
        }
        return Optional.empty();
    }

    private ListView<String> createNoteTrack() {
        double colWidth = scaler.scaleX(Quantizer.COL_WIDTH).get();
        double rowHeight = scaler.scaleY(Quantizer.ROW_HEIGHT).get();

        noteTrack = new ListView<>();
        noteTrack.setSelectionModel(new NoSelectionModel<>());
        noteTrack.setFixedCellSize(colWidth);
        noteTrack.setOrientation(Orientation.HORIZONTAL);
        noteTrack.setCellFactory(source -> new ListCell<>() {
            {
                setPrefHeight(rowHeight * PitchUtils.TOTAL_NUM_PITCHES);
                setStyle("-fx-padding: 0px;");
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                VBox column = new VBox();
                for (int octave = 7; octave > 0; octave--) {
                    for (String pitch : PitchUtils.REVERSE_PITCHES) {
                        // Add row to track.
                        Pane newCell = new Pane();
                        newCell.setPrefSize(colWidth, rowHeight);
                        newCell.getStyleClass().add("track-cell");
                        if (getIndex() >= 4) {
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
                        column.getChildren().add(newCell);
                    }
                }
                setGraphic(column);
            }
        });
        // Custom scroll behavior because default behavior is stupid.
        noteTrack.addEventFilter(ScrollEvent.ANY, event -> {
            Optional<ScrollBar> verticalScroll = getScrollBar(noteTrack, Orientation.VERTICAL);
            if (verticalScroll.isPresent()) {
                double newValue = verticalScroll.get().getValue() - event.getDeltaY();
                double boundedValue = Math.min(
                        verticalScroll.get().getMax(),
                        Math.max(verticalScroll.get().getMin(), newValue));
                verticalScroll.get().setValue(boundedValue);
            }
            Optional<ScrollBar> horizontalScroll = getScrollBar(noteTrack, Orientation.HORIZONTAL);
            if (horizontalScroll.isPresent()) {
                double deltaX = event.getDeltaX() / noteTrack.getWidth();
                double newValue = horizontalScroll.get().getValue() - deltaX;
                double boundedValue = Math.min(
                        horizontalScroll.get().getMax(),
                        Math.max(horizontalScroll.get().getMin(), newValue));
                horizontalScroll.get().setValue(boundedValue);
            }
            event.consume();
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
        dynamicsTrack.setSelectionModel(new NoSelectionModel<>());
        dynamicsTrack.setFixedCellSize(colWidth);
        dynamicsTrack.setOrientation(Orientation.HORIZONTAL);
        dynamicsTrack.setCellFactory(source -> new ListCell<>() {
            {
                setPrefHeight(rowHeight * 2);
                setStyle("-fx-padding: 0px;");
            }
            @Override
            protected void updateItem (String item, boolean empty) {
                super.updateItem(item, empty);

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
        // Custom scroll behavior because default behavior is stupid.
        dynamicsTrack.addEventFilter(ScrollEvent.ANY, event -> {
            Optional<ScrollBar> horizontalScroll =
                    getScrollBar(dynamicsTrack, Orientation.HORIZONTAL);
            if (horizontalScroll.isPresent()) {
                double deltaX = event.getDeltaX() / dynamicsTrack.getWidth();
                double newValue = horizontalScroll.get().getValue() - deltaX;
                double boundedValue = Math.min(
                        horizontalScroll.get().getMax(),
                        Math.max(horizontalScroll.get().getMin(), newValue));
                horizontalScroll.get().setValue(boundedValue);
            }
            event.consume();
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

    /**
     * No-op selection model to remove unwanted selection behavior.
     */
    private static class NoSelectionModel<T> extends MultipleSelectionModel<T> {
        @Override
        public void clearAndSelect(int i) {}
        @Override
        public void select(int i) {}
        @Override
        public void select(T t) {}
        @Override
        public void clearSelection(int i) {}
        @Override
        public void clearSelection() {}
        @Override
        public boolean isSelected(int i) {
            return false;
        }
        @Override
        public boolean isEmpty() {
            return true;
        }
        @Override
        public void selectPrevious() {}
        @Override
        public void selectNext() {}
        @Override
        public ObservableList<Integer> getSelectedIndices() {
            return FXCollections.observableArrayList();
        }
        @Override
        public ObservableList<T> getSelectedItems() {
            return FXCollections.observableArrayList();
        }
        @Override
        public void selectIndices(int i, int... ints) {}
        @Override
        public void selectAll() {}
        @Override
        public void selectFirst() {}
        @Override
        public void selectLast() {}
    }
}
