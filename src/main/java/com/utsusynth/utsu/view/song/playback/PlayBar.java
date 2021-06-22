package com.utsusynth.utsu.view.song.playback;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.utsusynth.utsu.common.quantize.Quantizer;
import com.utsusynth.utsu.common.quantize.Scaler;
import com.utsusynth.utsu.common.utils.PitchUtils;
import com.utsusynth.utsu.view.song.track.TrackItem;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.shape.Line;

import java.util.HashSet;

public class PlayBar implements TrackItem {
    private static final int TOTAL_HEIGHT = PitchUtils.TOTAL_NUM_PITCHES * Quantizer.ROW_HEIGHT;
    private static final int STROKE_WIDTH = 2;

    private final Scaler scaler;
    private final HashSet<Integer> drawnColumns;

    private DoubleProperty xValue;

    @Inject
    PlayBar(Scaler scaler) {
        this.scaler = scaler;
        drawnColumns = new HashSet<>();

        xValue = new SimpleDoubleProperty(0);
    }

    void setX(double newX) {
        xValue.set(newX);
    }

    void clearListeners() {
        double currentValue = xValue.get();
        xValue = new SimpleDoubleProperty(currentValue);
    }

    DoubleProperty xProperty() {
        return xValue;
    }

    @Override
    public TrackItemType getType() {
        return TrackItemType.PLAYBACK;
    }

    @Override
    public double getStartX() {
        return xValue.get();
    }

    @Override
    public double getWidth() {
        return STROKE_WIDTH;
    }

    @Override
    public Node redraw() {
        return redraw(0);
    }

    @Override
    public Node redraw(double offsetX) {
        Line bar = new Line(0, 0, 0, scaler.scaleY(TOTAL_HEIGHT).get());
        bar.translateXProperty().bind(xValue.subtract(offsetX));
        bar.getStyleClass().add("playback-bar");
        bar.setStrokeWidth(STROKE_WIDTH);
        bar.setMouseTransparent(true);

        // Add a backing bar to handle a Windows-specific optimization issue.
        Node playBarNode;
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            Line backingBar = new Line(0, 0, 0, scaler.scaleY(TOTAL_HEIGHT).get());
            backingBar.translateXProperty().bind(xValue.subtract(offsetX));
            backingBar.getStyleClass().add("playback-backing-bar");
            playBarNode = new Group(bar, backingBar);
        } else {
            playBarNode = bar;
        }
        return playBarNode;
    }

    @Override
    public ImmutableSet<Integer> getColumns() {
        return ImmutableSet.copyOf(drawnColumns);
    }

    @Override
    public void addColumn(int colNum) {
        drawnColumns.add(colNum);
    }

    @Override
    public void removeColumn(int colNum) {
        drawnColumns.remove(colNum);
    }

    @Override
    public void removeAllColumns() {
        drawnColumns.clear();
    }
}
