package com.utsusynth.utsu.view.song.track;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.utsusynth.utsu.common.utils.RegionBounds;
import com.utsusynth.utsu.common.quantize.Quantizer;
import com.utsusynth.utsu.common.quantize.Scaler;
import com.utsusynth.utsu.common.utils.PitchUtils;
import com.utsusynth.utsu.common.utils.RoundUtils;
import com.utsusynth.utsu.files.PreferencesManager;
import com.utsusynth.utsu.files.PreferencesManager.AutoscrollCancelMode;
import com.utsusynth.utsu.files.PreferencesManager.AutoscrollMode;
import com.utsusynth.utsu.view.song.DragHandler;
import javafx.beans.InvalidationListener;
import javafx.beans.property.DoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
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
    private final PreferencesManager preferencesManager;
    private final Scaler scaler;

    private int numMeasures = 0;
    private ListView<TrackItemSet> noteTrack;
    private ScrollBar noteHScrollBar;
    private ScrollBar noteVScrollBar;
    private ListView<TrackItemSet> dynamicsTrack;
    private TrackCallback callback;
    private DragHandler dragHandler;

    @Inject
    public Track(PreferencesManager preferencesManager, Scaler scaler) {
        this.preferencesManager = preferencesManager;
        this.scaler = scaler;
    }

    public void initialize(TrackCallback callback, DragHandler dragHandler) {
        this.callback = callback;
        this.dragHandler = dragHandler;
    }

    public void reset() {
        // Clear all items and set number of measures to 4.
        this.numMeasures = 4;
        setNumMeasures(noteTrack, numMeasures, false);
        setNumMeasures(dynamicsTrack, numMeasures, false);
    }

    public int getNumMeasures() {
        return numMeasures;
    }

    public void setNumMeasures(int numMeasures) {
        if (numMeasures < 0 || numMeasures == this.numMeasures) {
            return; // Don't refresh unless you have to.
        }
        this.numMeasures = numMeasures;
        setNumMeasures(noteTrack, numMeasures, true);
        setNumMeasures(dynamicsTrack, numMeasures, true);
    }

    private void setNumMeasures(
            ListView<TrackItemSet> updateMe, int numMeasures, boolean preserveItems) {
        if (updateMe != null) {
            int numCols = (numMeasures + 1) * 4;
            ObservableList<TrackItemSet> newItems = FXCollections.observableArrayList();
            for (int i = 0; i < numCols; i++) {
                if (preserveItems && updateMe.getItems().size() > i) {
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
        } else if (source == noteTrack) {
            if (orientation.equals(Orientation.VERTICAL) && noteVScrollBar != null) {
                return Optional.of(noteVScrollBar);
            }
            if (orientation.equals(Orientation.HORIZONTAL) && noteHScrollBar != null) {
                return Optional.of(noteHScrollBar);
            }
        }
        for (Node node : source.lookupAll(".scroll-bar")) {
            if (!(node instanceof ScrollBar)) {
                continue;
            }
            ScrollBar scrollBar = (ScrollBar) node;
            if (scrollBar.getOrientation() == orientation) {
                if (source == noteTrack && orientation.equals(Orientation.VERTICAL)) {
                    noteVScrollBar = scrollBar;
                }
                if (source == noteTrack && orientation.equals(Orientation.HORIZONTAL)) {
                    noteHScrollBar = scrollBar;
                }
                return Optional.of(scrollBar);
            }
        }
        return Optional.empty();
    }

    private ListView<TrackItemSet> createNoteTrack() {
        noteTrack = new ListView<>();
        noteVScrollBar = null;
        noteTrack.setSelectionModel(new NoSelectionModel<>());
        noteTrack.setOrientation(Orientation.HORIZONTAL);
        noteTrack.setCellFactory(source -> new ListCell<>() {
            {
                setStyle("-fx-padding: 0px;");
            }
            @Override
            protected void updateItem(TrackItemSet item, boolean empty) {
                super.updateItem(item, empty);
                if (getIndex() < 0) {
                    return; // Don't bother rendering if there is no item.
                }

                double colWidth = scaler.scaleX(Quantizer.COL_WIDTH);
                double rowHeight = scaler.scaleY(Quantizer.ROW_HEIGHT);
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
                        graphic.getChildren().add(trackItem.redraw(offset));
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
                return; // Horizontal scrolling should be left to default behavior.
            }
            Optional<ScrollBar> verticalScroll = getScrollBar(noteTrack, Orientation.VERTICAL);
            if (verticalScroll.isPresent()) {
                double newValue = verticalScroll.get().getValue() - event.getDeltaY();
                double boundedValue = Math.min(
                        verticalScroll.get().getMax(),
                        Math.max(verticalScroll.get().getMin(), newValue));
                verticalScroll.get().setValue(boundedValue);
            }
            event.consume();
        });
        setNumMeasures(noteTrack, numMeasures, false);
        return noteTrack;
    }

    public ListView<TrackItemSet> getNoteTrack() {
        if (noteTrack == null) {
            return createNoteTrack();
        }
        return noteTrack;
    }

    private ListView<TrackItemSet> createDynamicsTrack() {
        double rowHeight = 50;

        dynamicsTrack = new ListView<>();
        dynamicsTrack.setSelectionModel(new NoSelectionModel<>());
        dynamicsTrack.setOrientation(Orientation.HORIZONTAL);
        dynamicsTrack.setCellFactory(source -> new ListCell<>() {
            {
                setPrefHeight(rowHeight * 2);
                setStyle("-fx-padding: 0px;");
            }
            @Override
            protected void updateItem (TrackItemSet item, boolean empty) {
                super.updateItem(item, empty);
                if (getIndex() < 0) {
                    return; // Don't bother rendering if there is no item.
                }

                Pane graphic = new Pane();
                double colWidth = scaler.scaleX(Quantizer.COL_WIDTH);
                graphic.setPrefSize(colWidth, rowHeight * 2);

                // Background.
                if (callback != null) {
                    graphic.getChildren().addAll(callback.createDynamicsColumn(getIndex()));
                }
                // Foreground.
                if (item != null) {
                    for (TrackItem trackItem : item.asList()) {
                        double offset = getIndex() * colWidth;
                        graphic.getChildren().add(trackItem.redraw(offset));
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
        setNumMeasures(dynamicsTrack, numMeasures, false);
        return dynamicsTrack;
    }

    public ListView<TrackItemSet> getDynamicsTrack() {
        if (dynamicsTrack == null) {
            return createDynamicsTrack();
        }
        return dynamicsTrack;
    }

    public void scrollToPosition(int positionMs) {
        Optional<ScrollBar> hScroll = getScrollBar(noteTrack, Orientation.HORIZONTAL);
        if (hScroll.isPresent()) {
            double totalWidth = scaler.scaleX(Quantizer.COL_WIDTH) * (numMeasures + 1) * 4;
            double visibleWidth = noteTrack.getWidth() - Quantizer.SCROLL_BAR_WIDTH;
            if (visibleWidth != 0 && totalWidth > visibleWidth) {
                // Should be between 0 and 1.
                double hValue = scaler.scalePos(positionMs) / (totalWidth - visibleWidth);
                hScroll.get().setValue((hScroll.get().getMax() - hScroll.get().getMin()) * hValue);
            }
        }
    }

    public void startPlaybackAutoscroll(DoubleProperty playbackX) {
        Optional<ScrollBar> maybeHScroll = getScrollBar(noteTrack, Orientation.HORIZONTAL);
        AutoscrollMode autoscrollMode = preferencesManager.getAutoscroll();
        if (playbackX == null
                || maybeHScroll.isEmpty()
                || autoscrollMode.equals(AutoscrollMode.DISABLED)) {
            return;
        }
        // Implement autoscroll to follow playback bar.
        ScrollBar hScroll = maybeHScroll.get();
        InvalidationListener autoscrollListener = event -> {
            RegionBounds visibleRegion = visibleRegion();
            int newMs = RoundUtils.round(scaler.unscalePos(playbackX.get()));
            if (autoscrollMode.equals(AutoscrollMode.ENABLED_END)) {
                // Scroll when playback bar reaches end of screen.
                if (!visibleRegion.contains(newMs)) {
                    scrollToPosition(newMs);
                }
            } else if (autoscrollMode.equals(AutoscrollMode.ENABLED_MIDDLE)) {
                // Scroll when playback bar reaches middle of screen.
                int halfWidthMs = RoundUtils.round(
                        (visibleRegion().getMaxMs() - visibleRegion.getMinMs()) / 2.0);
                int scrollMidMs = visibleRegion.getMinMs() + halfWidthMs;
                if (!new RegionBounds(visibleRegion.getMinMs(), scrollMidMs + 10).contains(newMs)) {
                    scrollToPosition(Math.max(0, newMs - halfWidthMs));
                }
            }
        };
        playbackX.addListener(autoscrollListener);
        // Disable autoscroll if user jiggles the scroll bar.
        if (preferencesManager.getAutoscrollCancel().equals(AutoscrollCancelMode.ENABLED)) {
            ChangeListener<Number> disableAutoScroll = new ChangeListener<>() {
                @Override
                public void changed(ObservableValue<? extends Number> obs, Number oldValue, Number newValue) {
                    double visibleWidth = noteTrack.getWidth() - Quantizer.SCROLL_BAR_WIDTH;
                    double pixelsTravelled = visibleWidth
                            * Math.abs(newValue.doubleValue() - oldValue.doubleValue());
                    if (autoscrollMode.equals(AutoscrollMode.ENABLED_END) && pixelsTravelled < 5) {
                        playbackX.removeListener(autoscrollListener);
                        // These listeners remove themselves when user touches the srollbar, but
                        // there's a chance they could pile up before then.
                        hScroll.valueProperty().removeListener(this);
                    } else if (autoscrollMode.equals(AutoscrollMode.ENABLED_MIDDLE)
                            && pixelsTravelled < 5
                            && oldValue.doubleValue() > newValue.doubleValue()) {
                        playbackX.removeListener(autoscrollListener);
                        // These listeners remove themselves when user touches the scrollbar,
                        // but there's a chance they could pile up before then.
                        hScroll.valueProperty().removeListener(this);
                    } else {
                        hScroll.valueProperty().removeListener(this);
                    }
                }
            };
            hScroll.valueProperty().addListener(disableAutoScroll);
        }
    }

    public RegionBounds visibleRegion() {
        Optional<ScrollBar> maybeHScroll = getScrollBar(noteTrack, Orientation.HORIZONTAL);
        if (maybeHScroll.isEmpty()) {
            return noteTrack == null ? RegionBounds.INVALID : RegionBounds.WHOLE_SONG;
        }
        ScrollBar hScroll = maybeHScroll.get();
        double totalWidth = scaler.scaleX(Quantizer.COL_WIDTH) * (numMeasures + 1) * 4;
        double visibleWidth = noteTrack.getWidth() - Quantizer.SCROLL_BAR_WIDTH;
        if (visibleWidth <= 0 || totalWidth <= 0) {
            return RegionBounds.INVALID;
        } else if (visibleWidth >= totalWidth) {
            return RegionBounds.WHOLE_SONG;
        } else {
            double hValue = hScroll.getValue() * (hScroll.getMax() - hScroll.getMin());
            double leftX = hValue * (totalWidth - visibleWidth);
            double rightX = leftX + visibleWidth;
            int startPos = RoundUtils.round(scaler.unscalePos(leftX));
            int endPos = RoundUtils.round(scaler.unscalePos(rightX));
            return new RegionBounds(startPos, endPos); // May be negative positions.
        }
    }

    public void insertItem(ListView<TrackItemSet> track, TrackItem trackItem) {
        double startX = trackItem.getStartX();
        double endX = trackItem.getStartX() + trackItem.getWidth();
        double colWidth = scaler.scaleX(Quantizer.COL_WIDTH);
        int startColNum = (int) (startX / colWidth);
        int endColNum = (int) (endX / colWidth);
        ImmutableSet<Integer> columns = trackItem.getColumns();
        // Remove items that should no longer be rendered.
        for (Integer colNum : columns) {
            if (colNum >= startColNum && colNum <= endColNum) {
                continue;
            }
            if (colNum >= 0) {
                TrackItemSet itemSet = track.getItems().get(colNum);
                track.getItems().set(colNum, itemSet.withoutItem(trackItem));
            }
            trackItem.removeColumn(colNum);
        }
        // Add new items that should be rendered.
        for (int colNum = startColNum; colNum <= endColNum; colNum++) {
            if (columns.contains(colNum)) {
                continue; // Don't add something that's already there.
            }
            TrackItemSet itemSet = track.getItems().get(colNum);
            track.getItems().set(colNum, itemSet.withItem(trackItem));
            trackItem.addColumn(colNum);
        }
    }

    public void removeItem(ListView<TrackItemSet> track, TrackItem trackItem) {
        for (Integer colNum : trackItem.getColumns()) {
            if (colNum < 0 || track.getItems().size() <= colNum) {
                continue;
            }
            TrackItemSet itemSet = track.getItems().get(colNum);
            track.getItems().set(colNum, itemSet.withoutItem(trackItem));
        }
        trackItem.removeAllColumns();
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
