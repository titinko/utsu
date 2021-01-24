package com.utsusynth.utsu.view.song.note.pitch.portamento;

import java.util.ArrayList;
import com.google.common.collect.ImmutableList;
import com.utsusynth.utsu.common.data.PitchbendData;
import com.utsusynth.utsu.common.i18n.Localizer;
import com.utsusynth.utsu.common.quantize.Quantizer;
import com.utsusynth.utsu.common.quantize.Scaler;
import com.utsusynth.utsu.view.song.note.pitch.PitchbendCallback;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ToggleGroup;
import javafx.scene.shape.Rectangle;

public class Portamento {
    private static final double RADIUS = 2;

    private final int noteStartMs;
    private final double maxX; // Maximum x-position.
    private final double maxY; // Maximum y-position.
    private final ArrayList<Curve> curves; // Curves, ordered.
    private final ArrayList<Rectangle> squares; // Control points, ordered.
    private final Group curveGroup; // Curves, unordered.
    private final Group squareGroup; // Control points, unordered.
    private final Group group;
    private final PitchbendCallback callback;
    private final CurveFactory curveFactory;
    private final Localizer localizer;
    private final Scaler scaler;

    // Temporary cache values.
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
        this.callback = callback;
        this.curveFactory = factory;
        this.localizer = localizer;
        this.scaler = scaler;
        this.squares = new ArrayList<>();
        // Add control points.
        for (int i = 0; i < curves.size(); i++) {
            Curve curve = curves.get(i);
            Rectangle square = new Rectangle(
                    curve.getStartX() - RADIUS,
                    curve.getStartY() - RADIUS,
                    RADIUS * 2,
                    RADIUS * 2);
            curve.bindStart(square);
            if (i > 0) {
                curves.get(i - 1).bindEnd(square);
            }
            squares.add(square);

            // Add last control point.
            if (i == curves.size() - 1) {
                Rectangle end = new Rectangle(
                        curve.getEndX() - RADIUS,
                        curve.getEndY() - RADIUS,
                        RADIUS * 2,
                        RADIUS * 2);
                curve.bindEnd(end);
                squares.add(end);
            }
        }
        this.curves = curves;
        this.curveGroup = new Group();
        for (Curve curve : this.curves) {
            this.curveGroup.getChildren().add(curve.getElement());
        }
        this.squareGroup = new Group();
        for (Rectangle square : squares) {
            initializeControlPoint(square);
            this.squareGroup.getChildren().add(square);
        }
        this.group = new Group(curveGroup, squareGroup);
    }

    public Group getElement() {
        return group;
    }

    private void initializeControlPoint(Rectangle square) {
        square.getStyleClass().add("pitchbend");
        square.setOnMouseEntered(event -> {
            square.getScene().setCursor(Cursor.HAND);
        });
        square.setOnMouseExited(event -> {
            square.getScene().setCursor(Cursor.DEFAULT);
        });
        square.setOnContextMenuRequested(event -> {
            createContextMenu(square).show(square, event.getScreenX(), event.getScreenY());
        });
        square.setOnMousePressed(event -> {
            changed = false;
            startData = getData();
        });
        square.setOnMouseDragged(event -> {
            int index = squares.indexOf(square);
            double newX = event.getX();
            if (index == 0) {
                if (newX > RADIUS && newX < Math.min(
                        maxX - RADIUS, squares.get(index + 1).getX() + RADIUS)) {
                    changed = true;
                    square.setX(newX - RADIUS);
                }
            } else if (index == squares.size() - 1) {
                if (newX > squares.get(index - 1).getX() + RADIUS
                        && newX < maxX - RADIUS) {
                    changed = true;
                    square.setX(newX - RADIUS);
                }
            } else if (newX > squares.get(index - 1).getX() + RADIUS
                    && newX < Math.min(maxX, squares.get(index + 1).getX() + RADIUS)) {
                changed = true;
                square.setX(newX - RADIUS);
            }

            if (index > 0 && index < squares.size() - 1) {
                double newY = event.getY();
                if (newY > RADIUS && newY < maxY - RADIUS) {
                    changed = true;
                    square.setY(newY - RADIUS);
                }
            }
        });
        square.setOnMouseReleased(event -> {
            if (changed && callback != null) {
                callback.modifySongPitchbend(startData, getData());
            }
        });
    }

    private ContextMenu createContextMenu(Rectangle square) {
        int squareIndex = squares.indexOf(square);
        int curveIndex = Math.min(squareIndex, curves.size() - 1);
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
            firstCurve.bindStart(squares.get(curveIndex));
            curves.add(curveIndex, firstCurve);
            Curve secondCurve = curveFactory.createCurve(
                    halfX,
                    halfY,
                    curveToSplit.getEndX(),
                    curveToSplit.getEndY(),
                    curveToSplit.getType());
            secondCurve.bindEnd(squares.get(curveIndex + 1));
            curves.add(curveIndex + 1, secondCurve);
            curveGroup.getChildren().remove(curveToSplit.getElement());
            curveGroup.getChildren().addAll(firstCurve.getElement(), secondCurve.getElement());

            // Insert a new control point between the two new curves.
            Rectangle newSquare = new Rectangle(halfX - 2, halfY - 2, 4, 4);
            firstCurve.bindEnd(newSquare);
            secondCurve.bindStart(newSquare);
            int insertIndex = squareIndex == squares.size() - 1 ? squareIndex : squareIndex + 1;
            squares.add(insertIndex, newSquare);
            initializeControlPoint(newSquare);
            squareGroup.getChildren().add(newSquare);
            if (callback != null) {
                callback.modifySongPitchbend(oldData, getData());
            }
        });
        addControlPoint.setDisable(squares.size() >= 50); // Arbitrary control point limit.
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
            combined.bindStart(squares.get(squareIndex - 1));
            combined.bindEnd(squares.get(squareIndex + 1));
            curves.add(curveIndex - 1, combined);
            curveGroup.getChildren().removeAll(firstCurve.getElement(), secondCurve.getElement());
            curveGroup.getChildren().add(combined.getElement());

            // Remove the control point between the two curves.
            squares.remove(squareIndex);
            squareGroup.getChildren().remove(square);
            if (callback != null) {
                callback.modifySongPitchbend(oldData, getData());
            }
        });
        removeControlPoint.setDisable(squareIndex == 0 || squareIndex == squares.size() - 1);

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
            newCurve.bindStart(squares.get(curveIndex));
            newCurve.bindEnd(squares.get(curveIndex + 1));
            curves.set(curveIndex, newCurve);
            curveGroup.getChildren().remove(oldCurve.getElement());
            curveGroup.getChildren().add(newCurve.getElement());
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
