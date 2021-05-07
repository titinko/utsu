package com.utsusynth.utsu.view.song.note;

import com.utsusynth.utsu.view.song.TrackItem;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.shape.Rectangle;

import java.util.HashSet;
import java.util.Set;

public class AddNoteBox implements TrackItem {
    private final DoubleProperty startX;
    private final DoubleProperty startY;
    private final DoubleProperty width;
    private final DoubleProperty height;
    private final Set<Integer> drawnColumns;

    AddNoteBox() {
        startX = new SimpleDoubleProperty(0);
        startY = new SimpleDoubleProperty(0);
        width = new SimpleDoubleProperty(0);
        height = new SimpleDoubleProperty(0);
        drawnColumns = new HashSet<>();
    }

    public void setStartX(double newX) {
        this.startX.set(newX);
    }

    public void setStartY(double newY) {
        this.startY.set(newY);
    }

    public void setWidth(double newWidth) {
        this.width.set(newWidth);
    }

    public void setHeight(double newHeight) {
        this.height.set(newHeight);
    }

    @Override
    public double getStartX() {
        return startX.get();
    }

    @Override
    public double getWidth() {
        return width.get();
    }

    @Override
    public Rectangle redraw() {
        return redraw(-1, 0);
    }

    @Override
    public Rectangle redraw(int colNum, double offsetX) {
        drawnColumns.add(colNum);

        Rectangle box = new Rectangle();
        box.xProperty().bind(startX);
        box.yProperty().bind(startY);
        box.widthProperty().bind(width);
        box.heightProperty().bind(height);
        box.getStyleClass().setAll("add-note-box");
        box.setMouseTransparent(true);
        return box;
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