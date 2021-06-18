package com.utsusynth.utsu.view.song.note.pitch.portamento;

import java.util.*;

import com.google.common.collect.ImmutableList;
import com.utsusynth.utsu.common.data.PitchbendData;
import com.utsusynth.utsu.common.i18n.Localizer;
import com.utsusynth.utsu.common.quantize.Quantizer;
import com.utsusynth.utsu.common.quantize.Scaler;
import com.utsusynth.utsu.view.song.DragHandler;
import com.utsusynth.utsu.view.song.track.TrackItem;
import com.utsusynth.utsu.view.song.note.pitch.PitchbendCallback;
import javafx.scene.Group;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ToggleGroup;
import javafx.scene.shape.Rectangle;

public class Portamento implements TrackItem {
    private final int noteStartMs;
    private final double maxX; // Maximum x-position.
    private final double maxY; // Maximum y-position.
    private final ArrayList<Curve> curves; // Curves, ordered.
    private final ArrayList<ControlPoint> controlPoints; // Control points, ordered.
    private final HashMap<Double, Group> drawnCurves;
    private final HashMap<Double, Group> drawnSquares;
    private final HashSet<Integer> drawnColumns;
    private final PitchbendCallback callback;
    private final CurveFactory curveFactory;
    private final Localizer localizer;
    private final Scaler scaler;

    // UI-independent state.
    private boolean changed = false;
    private PitchbendData startData;

    public Portamento(
            int noteStartMs,
            double maxX,
            double maxY,
            ArrayList<Curve> curves,
            PitchbendCallback callback,
            CurveFactory factory,
            Localizer localizer,
            Scaler scaler) {
        this.noteStartMs = noteStartMs;
        this.maxX = maxX;
        this.maxY = maxY;
        this.curves = curves;
        this.callback = callback;
        this.curveFactory = factory;
        this.localizer = localizer;
        this.scaler = scaler;
        drawnCurves = new HashMap<>();
        drawnSquares = new HashMap<>();
        drawnColumns = new HashSet<>();

        controlPoints = new ArrayList<>();
        // Add control points
        for (int i = 0; i < curves.size(); i++) {
            Curve curve = curves.get(i);
            ControlPoint point = new ControlPoint(curve.getStartX(), curve.getStartY());
            curve.bindStart(point.centerXProperty(), point.centerYProperty());
            if (i > 0) {
                curves.get(i - 1).bindEnd(point.centerXProperty(), point.centerYProperty());
            }
            controlPoints.add(point);

            // Add last control point.
            if (i == curves.size() - 1) {
                ControlPoint end = new ControlPoint(curve.getEndX(), curve.getEndY());
                curve.bindEnd(end.centerXProperty(), end.centerYProperty());
                controlPoints.add(end);
            }
        }
    }

    @Override
    public TrackItemType getType() {
        return TrackItemType.PITCHBEND;
    }

    @Override
    public double getStartX() {
        if (curves.isEmpty()) {
            return -1;
        }
        return curves.get(0).getStartX();
    }

    @Override
    public double getWidth() {
        if (curves.isEmpty()) {
            return 0;
        }
        return curves.get(curves.size() - 1).getEndX() - curves.get(0).getStartX();
    }

    @Override
    public Group redraw() {
        return redraw(-1, 0);
    }

    @Override
    public Group redraw(int colNum, double offsetX) {
        drawnColumns.add(colNum);

        Group curveGroup = new Group();
        for (Curve curve : this.curves) {
            curveGroup.getChildren().add(curve.redraw(offsetX));
        }
        drawnCurves.put(offsetX, curveGroup);
        Group squareGroup = new Group();
        drawnSquares.put(offsetX, squareGroup);
        for (ControlPoint point : controlPoints) {
            squareGroup.getChildren().add(initializeControlPoint(point, offsetX));
        }
        drawnSquares.put(offsetX, squareGroup);
        return new Group(curveGroup, squareGroup);
    }

    @Override
    public HashSet<Integer> getColumns() {
        return drawnColumns;
    }

    @Override
    public void clearColumns() {
        drawnColumns.clear();
        drawnCurves.clear();
        drawnSquares.clear();
    }

    private Rectangle initializeControlPoint(ControlPoint point, double offsetX) {
        Rectangle square = point.redraw(offsetX);
        square.setOnContextMenuRequested(event -> {
            createContextMenu(point).show(square, event.getScreenX(), event.getScreenY());
        });
        square.setOnMousePressed(event -> {
            changed = false;
            startData = getData();
        });
        square.setOnDragDetected(event -> {
            if (callback == null) {
                return;
            }
            square.startFullDrag();
            callback.startDrag(new DragHandler() {
                @Override
                public void onDragged(double absoluteX, double absoluteY) {
                    dragPoint(point, absoluteX, absoluteY);
                }

                @Override
                public void onDragReleased(double absoluteX, double absoluteY) {
                    if (changed) {
                        callback.modifySongPitchbend(startData, getData());
                    }
                }
            });
        });
        square.setOnMouseDragged(event -> {
            // Only use old-fashioned drag when there is no callback.
            if (callback != null) {
                return;
            }
            double xDiff = event.getX() - (square.getX() + square.getWidth() / 2.0);
            double newX = point.getCenterX() + xDiff;
            dragPoint(point, newX, event.getY());
        });
        return square;
    }

    private void dragPoint(ControlPoint point, double newX, double newY) {
        int index = controlPoints.indexOf(point);
        // First point.
        if (index == 0) {
            if (newX > 0 && newX < Math.min(maxX, controlPoints.get(index + 1).getCenterX())) {
                changed = true;
                point.setCenterX(newX);
                if (callback != null) {
                    callback.readjust();
                }
            }
            // Last point.
        } else if (index == controlPoints.size() - 1) {
            if (newX > controlPoints.get(index - 1).getCenterX() && newX < maxX) {
                changed = true;
                point.setCenterX(newX);
                if (callback != null) {
                    callback.readjust();
                }
            }
            // Middle point.
        } else if (newX > controlPoints.get(index - 1).getCenterX()
                && newX < Math.min(maxX, controlPoints.get(index + 1).getCenterX())) {
            changed = true;
            point.setCenterX(newX);
            if (callback != null) {
                callback.readjust();
            }
        }

        // y-values.
        if (index > 0 && index < controlPoints.size() - 1) {
            if (newY > 0 && newY < maxY) {
                changed = true;
                point.setCenterY(newY);
            }
        }
    }

    private void updateCurvesInPlace() {
        for (Map.Entry<Double, Group> curveEntry : drawnCurves.entrySet()) {
            Group curveGroup = curveEntry.getValue();
            curveGroup.getChildren().clear();
            for (Curve curve : curves) {
                curveGroup.getChildren().add(curve.redraw(curveEntry.getKey()));
            }
        }
    }

    private void updateSquaresInPlace() {
        for (Map.Entry<Double, Group> squareEntry : drawnSquares.entrySet()) {
            Group squareGroup = squareEntry.getValue();
            squareGroup.getChildren().clear();
            for (ControlPoint newPoint : controlPoints) {
                squareGroup.getChildren().add(
                        initializeControlPoint(newPoint, squareEntry.getKey()));
            }
        }
    }

    private ContextMenu createContextMenu(ControlPoint point) {
        int pointIndex = controlPoints.indexOf(point);
        int curveIndex = Math.min(pointIndex, curves.size() - 1);
        ContextMenu contextMenu = new ContextMenu();

        MenuItem addControlPoint = new MenuItem("Add control point");
        addControlPoint.setOnAction(event -> {
            // Split curve into two.
            PitchbendData oldData = getData();
            Curve curveToSplit = curves.remove(curveIndex);
            double halfX = (curveToSplit.getStartX() + curveToSplit.getEndX()) / 2;
            double halfY = (curveToSplit.getStartY() + curveToSplit.getEndY()) / 2;
            Curve firstCurve = curveFactory.createCurve(
                    curveToSplit.getStartX(),
                    curveToSplit.getStartY(),
                    halfX,
                    halfY,
                    curveToSplit.getType());
            ControlPoint startPoint = controlPoints.get(curveIndex);
            firstCurve.bindStart(startPoint.centerXProperty(), startPoint.centerYProperty());
            curves.add(curveIndex, firstCurve);
            Curve secondCurve = curveFactory.createCurve(
                    halfX,
                    halfY,
                    curveToSplit.getEndX(),
                    curveToSplit.getEndY(),
                    curveToSplit.getType());
            ControlPoint endPoint = controlPoints.get(curveIndex + 1);
            secondCurve.bindEnd(endPoint.centerXProperty(), endPoint.centerYProperty());
            curves.add(curveIndex + 1, secondCurve);
            updateCurvesInPlace(); // Update all currently-displayed views.

            // Insert a new control point between the two new curves.
            ControlPoint newPoint = new ControlPoint(halfX, halfY);
            firstCurve.bindEnd(newPoint.centerXProperty(), newPoint.centerYProperty());
            secondCurve.bindStart(newPoint.centerXProperty(), newPoint.centerYProperty());
            int insertIndex = pointIndex == controlPoints.size() - 1 ? pointIndex : pointIndex + 1;
            controlPoints.add(insertIndex, newPoint);
            for (Map.Entry<Double, Group> squareEntry : drawnSquares.entrySet()) {
                squareEntry.getValue().getChildren().add(
                        initializeControlPoint(newPoint, squareEntry.getKey()));
            }
            if (callback != null) {
                callback.modifySongPitchbend(oldData, getData());
            }
        });
        addControlPoint.setDisable(controlPoints.size() >= 50); // Arbitrary control point limit.
        MenuItem removeControlPoint = new MenuItem("Remove control point");
        removeControlPoint.setOnAction(event -> {
            // Combine two curves into one.
            assert (curveIndex > 0); // Should disable removeControlPoint if this is true.
            PitchbendData oldData = getData();
            Curve firstCurve = curves.remove(curveIndex - 1);
            Curve secondCurve = curves.remove(curveIndex - 1);
            Curve combined = curveFactory.createCurve(
                    firstCurve.getStartX(),
                    firstCurve.getStartY(),
                    secondCurve.getEndX(),
                    secondCurve.getEndY(),
                    firstCurve.getType());
            ControlPoint startPoint = controlPoints.get(pointIndex - 1);
            combined.bindStart(startPoint.centerXProperty(), startPoint.centerYProperty());
            ControlPoint endPoint = controlPoints.get(pointIndex + 1);
            combined.bindEnd(endPoint.centerXProperty(), endPoint.centerYProperty());
            curves.add(curveIndex - 1, combined);
            updateCurvesInPlace(); // Update all currently-displayed views.

            // Remove the control point between the two curves.
            controlPoints.remove(pointIndex);
            updateSquaresInPlace(); // Update all currently-displayed views.
            if (callback != null) {
                callback.modifySongPitchbend(oldData, getData());
            }
        });
        removeControlPoint.setDisable(pointIndex == 0 || pointIndex == controlPoints.size() - 1);

        Menu curveType = new Menu("Curve");
        RadioMenuItem sCurve = createCurveChoice("S curve", "", curveIndex);
        RadioMenuItem jCurve = createCurveChoice("J curve", "j", curveIndex);
        RadioMenuItem rCurve = createCurveChoice("R curve", "r", curveIndex);
        RadioMenuItem straightCurve = createCurveChoice("Straight", "s", curveIndex);

        ToggleGroup curveGroup = new ToggleGroup();
        sCurve.setToggleGroup(curveGroup);
        jCurve.setToggleGroup(curveGroup);
        rCurve.setToggleGroup(curveGroup);
        straightCurve.setToggleGroup(curveGroup);
        curveType.getItems().addAll(sCurve, jCurve, rCurve, straightCurve);

        contextMenu.getItems().addAll(addControlPoint, removeControlPoint, curveType);
        contextMenu.setOnShowing(event -> {
            addControlPoint.setText(localizer.getMessage("song.note.addControlPoint"));
            removeControlPoint.setText(localizer.getMessage("song.note.removeControlPoint"));
            curveType.setText(localizer.getMessage("song.note.curveType"));
            sCurve.setText(localizer.getMessage("song.note.sCurve"));
            jCurve.setText(localizer.getMessage("song.note.jCurve"));
            rCurve.setText(localizer.getMessage("song.note.rCurve"));
            straightCurve.setText(localizer.getMessage("song.note.straightCurve"));
        });
        return contextMenu;
    }

    private RadioMenuItem createCurveChoice(String label, String curveType, int curveIndex) {
        RadioMenuItem radioItem = new RadioMenuItem(label);
        radioItem.setOnAction(event -> {
            PitchbendData oldData = getData();
            Curve oldCurve = curves.get(curveIndex);
            Curve newCurve = curveFactory.createCurve(
                    oldCurve.getStartX(),
                    oldCurve.getStartY(),
                    oldCurve.getEndX(),
                    oldCurve.getEndY(),
                    curveType);
            ControlPoint startPoint = controlPoints.get(curveIndex);
            newCurve.bindStart(startPoint.centerXProperty(), startPoint.centerYProperty());
            ControlPoint endPoint = controlPoints.get(curveIndex + 1);
            newCurve.bindEnd(endPoint.centerXProperty(), endPoint.centerYProperty());
            curves.set(curveIndex, newCurve);
            updateCurvesInPlace(); // Update all currently-displayed views.
            if (callback != null) {
                callback.modifySongPitchbend(oldData, getData());
            }
        });
        if (curves.get(curveIndex).getType().equals(curveType)) {
            radioItem.setSelected(true);
        }
        return radioItem;
    }

    public PitchbendData getData() {
        assert (curves.size() > 0);
        double startX = scaler.unscalePos(curves.get(0).getStartX()) - noteStartMs;
        double endY = scaler.unscaleY(curves.get(curves.size() - 1).getEndY());
        ImmutableList.Builder<Double> widths = ImmutableList.builder();
        ImmutableList.Builder<Double> heights = ImmutableList.builder();
        ImmutableList.Builder<String> shapes = ImmutableList.builder();
        for (int i = 0; i < curves.size(); i++) {
            Curve line = curves.get(i);
            widths.add(scaler.unscaleX((line.getEndX() - line.getStartX())));
            if (i < curves.size() - 1) {
                heights.add((endY - scaler.unscaleY(line.getEndY())) / Quantizer.ROW_HEIGHT * 10);
            }
            shapes.add(line.getType());
        }
        return new PitchbendData(
                ImmutableList.of(startX, 0.0),
                widths.build(),
                heights.build(),
                shapes.build());
    }
}
