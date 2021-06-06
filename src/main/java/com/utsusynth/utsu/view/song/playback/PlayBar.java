package com.utsusynth.utsu.view.song.playback;

import com.google.inject.Inject;
import com.utsusynth.utsu.common.quantize.Quantizer;
import com.utsusynth.utsu.common.quantize.Scaler;
import com.utsusynth.utsu.common.utils.PitchUtils;
import com.utsusynth.utsu.view.song.TrackItem;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.shape.Line;

import java.util.HashSet;
import java.util.Set;

public class PlayBar implements TrackItem {
    private static final int TOTAL_HEIGHT = PitchUtils.TOTAL_NUM_PITCHES * Quantizer.ROW_HEIGHT;
    private static final int STROKE_WIDTH = 2;

    private final Scaler scaler;
    private final DoubleProperty xValue;
    private final Set<Integer> drawnColumns;

    @Inject
    PlayBar(Scaler scaler) {
        this.scaler = scaler;
        this.xValue = new SimpleDoubleProperty(0);
        drawnColumns = new HashSet<>();
    }

    void setX(double newX) {
        xValue.set(newX);
    }

    DoubleProperty xProperty() {
        return xValue;
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
        return redraw(-1, 0);
    }

    @Override
    public Node redraw(int colNum, double offsetX) {
        drawnColumns.add(colNum);

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
    public Set<Integer> getColumns() {
        return drawnColumns;
    }

    @Override
    public void clearColumns() {
        drawnColumns.clear();
    }
}
