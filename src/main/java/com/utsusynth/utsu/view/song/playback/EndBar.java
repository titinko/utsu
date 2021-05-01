package com.utsusynth.utsu.view.song.playback;

import com.utsusynth.utsu.view.song.TrackItem;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.shape.Line;

import java.util.HashSet;
import java.util.Set;

public class EndBar implements TrackItem {
    private static final int STROKE_WIDTH = 2;

    private final DoubleProperty xValue;
    private final double heightY;
    private final Set<Integer> drawnColumns;

    EndBar(double xValue, double heightY) {
        this.xValue = new SimpleDoubleProperty(xValue);
        this.heightY = heightY;
        drawnColumns = new HashSet<>();
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

        Line bar = new Line(0, 0, 0, heightY);
        bar.translateXProperty().bind(xValue.subtract(offsetX));
        bar.getStyleClass().add("end-bar");
        bar.setStrokeWidth(STROKE_WIDTH);
        bar.setMouseTransparent(true);
        return null;
    }

    @Override
    public Set<Integer> getColumns() {
        return drawnColumns;
    }

    @Override
    public void clearColumns() {
        drawnColumns.clear();
    }
}
