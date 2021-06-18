package com.utsusynth.utsu.view.song.playback;

import com.google.inject.Inject;
import com.utsusynth.utsu.common.quantize.Quantizer;
import com.utsusynth.utsu.common.quantize.Scaler;
import com.utsusynth.utsu.common.utils.PitchUtils;
import com.utsusynth.utsu.view.song.track.TrackItem;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.shape.Line;

import java.util.HashSet;

public class StartBar implements TrackItem {
    private static final int TOTAL_HEIGHT = PitchUtils.TOTAL_NUM_PITCHES * Quantizer.ROW_HEIGHT;
    private static final int STROKE_WIDTH = 2;

    private final Scaler scaler;
    private final DoubleProperty xValue;
    private final HashSet<Integer> drawnColumns;

    @Inject
    StartBar(Scaler scaler) {
        this.scaler = scaler;
        this.xValue = new SimpleDoubleProperty(0);
        drawnColumns = new HashSet<>();
    }

    void setX(double newX) {
        xValue.set(newX);
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
    public Line redraw() {
        return redraw(-1, 0);
    }

    @Override
    public Line redraw(int colNum, double offsetX) {
        drawnColumns.add(colNum);

        Line bar = new Line(0, 0, 0, scaler.scaleY(TOTAL_HEIGHT).get());
        bar.translateXProperty().bind(xValue.subtract(offsetX));
        bar.getStyleClass().add("start-bar");
        bar.setStrokeWidth(STROKE_WIDTH);
        bar.setMouseTransparent(true);
        return bar;
    }

    @Override
    public HashSet<Integer> getColumns() {
        return drawnColumns;
    }

    @Override
    public void clearColumns() {
        drawnColumns.clear();
    }
}
