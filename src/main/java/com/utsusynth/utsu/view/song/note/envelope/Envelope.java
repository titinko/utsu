package com.utsusynth.utsu.view.song.note.envelope;

import com.utsusynth.utsu.common.data.EnvelopeData;
import com.utsusynth.utsu.common.quantize.Scaler;
import com.utsusynth.utsu.common.utils.RoundUtils;
import com.utsusynth.utsu.view.song.TrackItem;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.shape.Circle;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Envelope implements TrackItem {
    private final MoveTo start;
    private final LineTo[] lines;
    private final LineTo end;
    private final double maxHeight;
    private final EnvelopeCallback callback;
    private final Scaler scaler;

    private final Map<Integer, Group> drawnCache;
    private final DoubleProperty[] xValues;
    private final DoubleProperty[] yValues;

    // Temporary cache values.
    private boolean changed = false;
    private EnvelopeData startData;

    Envelope(
            MoveTo start,
            LineTo l1,
            LineTo l2,
            LineTo l3,
            LineTo l4,
            LineTo l5,
            LineTo end,
            EnvelopeCallback callback,
            double maxHeight,
            Scaler scaler) {
        this.maxHeight = maxHeight;
        this.scaler = scaler;
        this.callback = callback;
        this.start = start;
        lines = new LineTo[] {l1, l2, l3, l4, l5};
        xValues = new DoubleProperty[] {
                new SimpleDoubleProperty(l1.getX()),
                new SimpleDoubleProperty(l2.getX()),
                new SimpleDoubleProperty(l3.getX()),
                new SimpleDoubleProperty(l4.getX()),
                new SimpleDoubleProperty(l5.getX())
        };
        yValues = new DoubleProperty[] {
                new SimpleDoubleProperty(l1.getY()),
                new SimpleDoubleProperty(l2.getY()),
                new SimpleDoubleProperty(l3.getY()),
                new SimpleDoubleProperty(l4.getY()),
                new SimpleDoubleProperty(l5.getY())
        };
        drawnCache = new HashMap<>();
        this.end = end;
    }

    @Override
    public double getStartX() {
        return start.getX();
    }

    @Override
    public double getWidth() {
        return end.getX() - start.getX();
    }

    @Override
    public Group getElement() {
        return redraw(-1, 0); // Grap element without any positioning.
    }

    @Override
    public Group redraw(int colNum, double offsetX) {
        if (drawnCache.containsKey(colNum)) {
            return drawnCache.get(colNum);
        }
        MoveTo startClone = new MoveTo(start.getX() - offsetX, start.getY());
        LineTo l1Clone = new LineTo(lines[0].getX() - offsetX, lines[0].getY());
        LineTo l2Clone = new LineTo(lines[1].getX() - offsetX, lines[1].getY());
        LineTo l3Clone = new LineTo(lines[2].getX() - offsetX, lines[2].getY());
        LineTo l4Clone = new LineTo(lines[3].getX() - offsetX, lines[3].getY());
        LineTo l5Clone = new LineTo(lines[4].getX() - offsetX, lines[4].getY());
        LineTo[] linesClone = new LineTo[] {l1Clone, l2Clone, l3Clone, l4Clone, l5Clone};
        LineTo endClone = new LineTo(end.getX() - offsetX, end.getY());
        Path pathClone = new Path(
                startClone, l1Clone, l2Clone, l3Clone, l4Clone, l5Clone, endClone);
        pathClone.getStyleClass().add("envelope-line");

        Circle[] circles = new Circle[5]; // Control points.
        for (int i = 0; i < 5; i++) {
            Circle circle = new Circle(linesClone[i].getX(), linesClone[i].getY(), 3);
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
            linesClone[i].xProperty().bind(circle.centerXProperty());
            linesClone[i].yProperty().bind(circle.centerYProperty());
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
                    if (newX > linesClone[index - 1].getX() && newX < linesClone[index + 1].getX()) {
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
        Group groupClone = new Group(
                pathClone, circles[0], circles[1], circles[2], circles[4], circles[3]);
        drawnCache.put(colNum, groupClone);
        return groupClone;
    }

    @Override
    public Set<Integer> getColumns() {
        return drawnCache.keySet();
    }

    public int getStartMs() {
        return RoundUtils.round(scaler.unscalePos(start.getX()));
    }

    public EnvelopeData getData() {
        double[] widths = new double[5];
        widths[0] = scaler.unscaleX(xValues[0].get() - start.getX());
        widths[1] = scaler.unscaleX(xValues[1].get() - xValues[0].get());
        widths[2] = scaler.unscaleX(xValues[4].get() - xValues[3].get());
        widths[3] = scaler.unscaleX(end.getX() - xValues[4].get());
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
