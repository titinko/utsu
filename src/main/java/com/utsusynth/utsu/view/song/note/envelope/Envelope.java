package com.utsusynth.utsu.view.song.note.envelope;

import com.utsusynth.utsu.common.data.EnvelopeData;
import com.utsusynth.utsu.common.quantize.Scaler;
import com.utsusynth.utsu.common.utils.RoundUtils;
import com.utsusynth.utsu.view.song.TrackItem;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.shape.Circle;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;

import java.util.HashSet;
import java.util.Set;

public class Envelope implements TrackItem {
    private final double startX;
    private final double startY;
    private final DoubleProperty[] xValues;
    private final DoubleProperty[] yValues;
    private final double endX;
    private final double endY;
    private final double maxHeight;
    private final Set<Integer> drawnColumns;
    private final EnvelopeCallback callback;
    private final Scaler scaler;

    // Temporary cache values.
    private boolean changed = false;
    private EnvelopeData startData;

    Envelope(
            double[] allXValues,
            double[] allYValues,
            EnvelopeCallback callback,
            double maxHeight,
            Scaler scaler) {
        this.maxHeight = maxHeight;
        this.scaler = scaler;
        this.callback = callback;
        this.startX = allXValues[0];
        this.startY = allYValues[0];
        this.xValues = new DoubleProperty[] {
                new SimpleDoubleProperty(allXValues[1]),
                new SimpleDoubleProperty(allXValues[2]),
                new SimpleDoubleProperty(allXValues[3]),
                new SimpleDoubleProperty(allXValues[4]),
                new SimpleDoubleProperty(allXValues[5])
        };
        this.yValues = new DoubleProperty[] {
                new SimpleDoubleProperty(allYValues[1]),
                new SimpleDoubleProperty(allYValues[2]),
                new SimpleDoubleProperty(allYValues[3]),
                new SimpleDoubleProperty(allYValues[4]),
                new SimpleDoubleProperty(allYValues[5])
        };
        drawnColumns = new HashSet<>();
        this.endX = allXValues[6];
        this.endY = allYValues[6];
    }

    @Override
    public double getStartX() {
        return startX;
    }

    @Override
    public double getWidth() {
        return endX - startX;
    }

    @Override
    public Group redraw() {
        return redraw(-1, 0); // Grab element without any positioning.
    }

    @Override
    public Group redraw(int colNum, double offsetX) {
        drawnColumns.add(colNum);

        // Refresh all x-values.
        for (int i = 0; i < xValues.length; i++) {
            double value = xValues[i].get();
            xValues[i] = new SimpleDoubleProperty(value);
        }
        MoveTo start = new MoveTo(startX - offsetX, startY);
        LineTo[] lines = new LineTo[] {
                new LineTo(xValues[0].get() - offsetX, yValues[0].get()),
                new LineTo(xValues[1].get() - offsetX, yValues[1].get()),
                new LineTo(xValues[2].get() - offsetX, yValues[2].get()),
                new LineTo(xValues[3].get() - offsetX, yValues[3].get()),
                new LineTo(xValues[4].get() - offsetX, yValues[4].get())};
        LineTo end = new LineTo(endX - offsetX, endY);
        Path path = new Path(
                start, lines[0], lines[1], lines[2], lines[3], lines[4], end);
        path.getStyleClass().add("envelope-line");

        Circle[] circles = new Circle[5]; // Control points.
        for (int i = 0; i < 5; i++) {
            Circle circle = new Circle(lines[i].getX(), lines[i].getY(), 3);
            circle.getStyleClass().add("envelope-circle");
            // Custom binding for circle x values.
            final int index = i;
            circle.centerXProperty().addListener((obs, oldX, newX) -> {
                if (!oldX.equals(newX)) {
                    double adjustedX = newX.doubleValue() + offsetX;
                    if (RoundUtils.round(adjustedX) != RoundUtils.round(xValues[index].get())) {
                        xValues[index].set(adjustedX);
                    }
                }
            });
            xValues[i].addListener((obs, oldX, newX) -> {
                if (!oldX.equals(newX)) {
                    double adjustedX = newX.doubleValue() - offsetX;
                    if (RoundUtils.round(adjustedX) != RoundUtils.round(circle.getCenterX())) {
                        circle.setCenterX(adjustedX);
                    }
                }
            });
            circle.centerYProperty().bindBidirectional(yValues[i]);
            lines[i].xProperty().bind(circle.centerXProperty());
            lines[i].yProperty().bind(circle.centerYProperty());
            circle.setOnMouseEntered(event -> {
                circle.getScene().setCursor(Cursor.HAND);
            });
            circle.setOnMouseExited(event -> {
                circle.getScene().setCursor(Cursor.DEFAULT);
            });
            circle.setOnMousePressed(event -> {
                changed = false;
                startData = getData();
            });
            circle.setOnMouseDragged(event -> {
                // Set reasonable limits for where envelope can be dragged.
                if (index > 0 && index < 4) {
                    double newX = event.getX();
                    if (newX > lines[index - 1].getX() && newX < lines[index + 1].getX()) {
                        changed = true;
                        circle.setCenterX(newX);
                    }
                }
                double newY = event.getY();
                if (newY >= 0 && newY <= maxHeight) {
                    changed = true;
                    circle.setCenterY(newY);
                }
            });
            circle.setOnMouseReleased(event -> {
                if (changed) {
                    callback.modifySongEnvelope(startData, getData());
                }
            });
            circles[i] = circle;
        }
        Group group = new Group(
                path, circles[0], circles[1], circles[2], circles[4], circles[3]);
        //drawnCache.put(colNum, group);
        return group;
    }

    @Override
    public Set<Integer> getColumns() {
        return drawnColumns;
    }

    @Override
    public void clearColumns() {
        drawnColumns.clear();
    }

    public int getStartMs() {
        return RoundUtils.round(scaler.unscalePos(startX));
    }

    public EnvelopeData getData() {
        double[] widths = new double[5];
        widths[0] = scaler.unscaleX(xValues[0].get() - startX);
        widths[1] = scaler.unscaleX(xValues[1].get() - xValues[0].get());
        widths[2] = scaler.unscaleX(xValues[4].get() - xValues[3].get());
        widths[3] = scaler.unscaleX(endX - xValues[4].get());
        widths[4] = scaler.unscaleX(xValues[2].get() - xValues[1].get());

        double multiplier = 200 / maxHeight; // Final value should have range 0-200.
        double[] heights = new double[5];
        heights[0] = (maxHeight - yValues[0].get()) * multiplier;
        heights[1] = (maxHeight - yValues[1].get()) * multiplier;
        heights[2] = (maxHeight - yValues[3].get()) * multiplier;
        heights[3] = (maxHeight - yValues[4].get()) * multiplier;
        heights[4] = (maxHeight - yValues[2].get()) * multiplier;
        return new EnvelopeData(widths, heights);
    }
}
