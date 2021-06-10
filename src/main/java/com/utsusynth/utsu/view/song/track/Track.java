package com.utsusynth.utsu.view.song.track;

import com.google.inject.Inject;
import com.utsusynth.utsu.common.quantize.Quantizer;
import com.utsusynth.utsu.common.quantize.Scaler;
import com.utsusynth.utsu.common.utils.PitchUtils;
import com.utsusynth.utsu.view.song.DragHandler;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;

import java.util.*;

/** The background track of the song editor. */
public class Track {
    private final Scaler scaler;

    private int numMeasures = 0;
    private ListView<TrackItemSet> noteTrack;
    private ScrollBar noteVScrollBar;
    private ListView<TrackItemSet> dynamicsTrack;
    private TrackCallback callback;
    private DragHandler dragHandler;

    @Inject
    public Track(Scaler scaler) {
        this.scaler = scaler;
    }

    public void initialize(TrackCallback callback, DragHandler dragHandler) {
        this.callback = callback;
        this.dragHandler = dragHandler;
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

    private void setNumMeasures(ListView<TrackItemSet> updateMe, int numMeasures) {
        if (updateMe != null) {
            int numCols = (numMeasures + 1) * 4;
            ObservableList<TrackItemSet> newItems = FXCollections.observableArrayList();
            for (int i = 0; i < numCols; i++) {
                if (updateMe.getItems().size() > i) {
                    newItems.add(updateMe.getItems().get(i));
                } else {
                    newItems.add(new TrackItemSet());
                }
            }
            updateMe.setItems(newItems);
        }
    }

    private Optional<ScrollBar> getScrollBar(
            ListView<TrackItemSet> source, Orientation orientation) {
        if (source == null) {
            return Optional.empty();
        }
        for (Node node : source.lookupAll(".scroll-bar")) {
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

    private ListView<TrackItemSet> createNoteTrack() {
        double colWidth = scaler.scaleX(Quantizer.COL_WIDTH).get();
        double rowHeight = scaler.scaleY(Quantizer.ROW_HEIGHT).get();

        noteTrack = new ListView<>();
        noteVScrollBar = null;
        noteTrack.setSelectionModel(new NoSelectionModel<>());
        noteTrack.setFixedCellSize(colWidth);
        noteTrack.setOrientation(Orientation.HORIZONTAL);
        noteTrack.setCellFactory(source -> new ListCell<>() {
            {
                setPrefHeight(rowHeight * PitchUtils.TOTAL_NUM_PITCHES);
                setStyle("-fx-padding: 0px;");
            }
            @Override
            protected void updateItem(TrackItemSet item, boolean empty) {
                super.updateItem(item, empty);
                if (getIndex() < 0) {
                    return; // Don't bother rendering if there is no item.
                }

                Pane graphic = new Pane();
                graphic.setPrefSize(colWidth, rowHeight * PitchUtils.TOTAL_NUM_PITCHES);

                // Background.
                if (callback != null) {
                    graphic.getChildren().add(callback.createNoteColumn(getIndex()));
                }
                // Foreground.
                if (item != null) {
                    for (TrackItem trackItem : item.asList()) {
                        double offset = getIndex() * colWidth;
                        graphic.getChildren().add(trackItem.redraw(getIndex(), offset));
                    }
                }
                // Drag behavior.
                graphic.setOnMouseDragOver(event -> {
                    if (dragHandler != null) {
                        dragHandler.onDragged(
                                event.getX() + (getIndex() * colWidth), event.getY());
                    }
                });
                graphic.setOnMouseDragReleased(event -> {
                    if (dragHandler != null) {
                        dragHandler.onDragReleased(
                                event.getX() + (getIndex() * colWidth), event.getY());
                    }
                });
                setGraphic(graphic);
            }
        });
        // Custom scroll behavior because default behavior is stupid.
        noteTrack.addEventFilter(ScrollEvent.ANY, event -> {
            if (event.getDeltaX() != 0) {
                return; // Vertical scrolling should be left to default behavior.
            }
            Optional<ScrollBar> verticalScroll = noteVScrollBar != null
                    ? Optional.of(noteVScrollBar) : getScrollBar(noteTrack, Orientation.VERTICAL);
            if (verticalScroll.isPresent()) {
                noteVScrollBar = verticalScroll.get();
                double newValue = verticalScroll.get().getValue() - event.getDeltaY();
                double boundedValue = Math.min(
                        verticalScroll.get().getMax(),
                        Math.max(verticalScroll.get().getMin(), newValue));
                verticalScroll.get().setValue(boundedValue);
            }
            event.consume();
        });
        setNumMeasures(noteTrack, numMeasures);
        return noteTrack;
    }

    public ListView<TrackItemSet> getNoteTrack() {
        if (noteTrack == null) {
            return createNoteTrack();
        }
        return noteTrack;
    }

    private ListView<TrackItemSet> createDynamicsTrack() {
        double colWidth = scaler.scaleX(Quantizer.COL_WIDTH).get();
        double rowHeight = 50;

        dynamicsTrack = new ListView<>();
        dynamicsTrack.setSelectionModel(new NoSelectionModel<>());
        dynamicsTrack.setFixedCellSize(colWidth);
        dynamicsTrack.setOrientation(Orientation.HORIZONTAL);
        dynamicsTrack.setCellFactory(source -> new ListCell<>() {
            {
                setPrefHeight(rowHeight * 2);
                setPrefWidth(colWidth);
                setStyle("-fx-padding: 0px;");
            }
            @Override
            protected void updateItem (TrackItemSet item, boolean empty) {
                super.updateItem(item, empty);
                if (getIndex() < 0) {
                    return; // Don't bother rendering if there is no item.
                }

                Pane graphic = new Pane();
                graphic.setPrefSize(colWidth, rowHeight * 2);

                // Background.
                if (callback != null) {
                    graphic.getChildren().addAll(callback.createDynamicsColumn(getIndex()));
                }
                // Foreground.
                if (item != null) {
                    for (TrackItem trackItem : item.asList()) {
                        double offset = getIndex() * colWidth;
                        graphic.getChildren().add(trackItem.redraw(getIndex(), offset));
                    }
                }
                setGraphic(graphic);
            }
        });
        // Custom scroll behavior because default behavior is stupid.
        dynamicsTrack.addEventFilter(ScrollEvent.ANY, event -> {
            if (event.getDeltaX() == 0) {
                event.consume();
            }
        });
        setNumMeasures(dynamicsTrack, numMeasures);
        return dynamicsTrack;
    }

    public ListView<TrackItemSet> getDynamicsTrack() {
        if (dynamicsTrack == null) {
            return createDynamicsTrack();
        }
        return dynamicsTrack;
    }

    public void scrollToPosition(int position) {
        // blah
    }

    public void insertItem(ListView<TrackItemSet> track, TrackItem trackItem) {
        double startX = trackItem.getStartX();
        double endX = trackItem.getStartX() + trackItem.getWidth();
        double colWidth = scaler.scaleX(Quantizer.COL_WIDTH).get();
        int startColNum = (int) (startX / colWidth);
        int endColNum = (int) (endX / colWidth);
        for (int colNum = startColNum; colNum <= endColNum; colNum++) {
            TrackItemSet itemSet = track.getItems().get(colNum);
            if (itemSet.hasItem(trackItem)) {
                continue; // Don't add something that's already there.
            }
            track.getItems().set(colNum, itemSet.withItem(trackItem));
        }
    }

    public void removeItem(ListView<TrackItemSet> track, TrackItem trackItem) {
        for (Integer colNum : trackItem.getColumns()) {
            if (colNum < 0) {
                continue;
            }
            TrackItemSet itemSet = track.getItems().get(colNum);
            track.getItems().set(colNum, itemSet.withoutItem(trackItem));
        }
        trackItem.clearColumns();
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
