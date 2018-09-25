package com.utsusynth.utsu.view.song.note.pitch.portamento;

import java.util.ArrayList;
import com.google.common.collect.ImmutableList;
import com.utsusynth.utsu.common.data.PitchbendData;
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
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class Portamento {
    private final ArrayList<Curve> curves; // Curves, ordered.
    private final ArrayList<Rectangle> squares; // Control points, ordered.
    private final Group curveGroup; // Curves, unordered.
    private final Group squareGroup; // Control points, unordered.
    private final Group group;
    private final PitchbendCallback callback;
    private final CurveFactory curveFactory;
    private final Scaler scaler;

    public Portamento(
            ArrayList<Curve> curves,
            PitchbendCallback callback,
            CurveFactory factory,
            Scaler scaler) {
        this.callback = callback;
        this.curveFactory = factory;
        this.scaler = scaler;
        this.squares = new ArrayList<>();
        // Add control points.
        for (int i = 0; i < curves.size(); i++) {
            Curve curve = curves.get(i);
            Rectangle square = new Rectangle(curve.getStartX() - 2, curve.getStartY() - 2, 4, 4);
            curve.bindStart(square);
            if (i > 0) {
                curves.get(i - 1).bindEnd(square);
            }
            squares.add(square);

            // Add last control point.
            if (i == curves.size() - 1) {
                Rectangle end = new Rectangle(curve.getEndX() - 2, curve.getEndY() - 2, 4, 4);
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
        square.setStroke(Color.DARKSLATEBLUE);
        square.setFill(Color.TRANSPARENT);
        square.setOnMouseEntered(event -> {
            square.getScene().setCursor(Cursor.HAND);
        });
        square.setOnMouseExited(event -> {
            square.getScene().setCursor(Cursor.DEFAULT);
        });
        square.setOnContextMenuRequested(event -> {
            createContextMenu(square).show(square, event.getScreenX(), event.getScreenY());
        });
        square.setOnMouseDragged(event -> {
            int index = squares.indexOf(square);
            boolean changed = false;
            double newX = event.getX();
            if (index == 0) {
                if (newX > 0 && newX < squares.get(index + 1).getX() + 2) {
                    changed = true;
                    square.setX(newX - 2);
                }
            } else if (index == squares.size() - 1) {
                if (newX > squares.get(index - 1).getX() + 2) {
                    changed = true;
                    square.setX(newX - 2);
                }
            } else if (newX > squares.get(index - 1).getX() + 2
                    && newX < squares.get(index + 1).getX() + 2) {
                changed = true;
                square.setX(newX - 2);
            }

            if (index > 0 && index < squares.size() - 1) {
                double newY = event.getY();
                if (newY > 0 && newY < scaler.scaleY(Quantizer.ROW_HEIGHT * 12 * 7)) {
                    changed = true;
                    square.setY(newY - 2);
                }
            }
            if (changed) {
                callback.modifySongPitchbend();
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
            callback.modifySongPitchbend();
        });
        addControlPoint.setDisable(squares.size() >= 50); // Arbitrary control point limit.
        MenuItem removeControlPoint = new MenuItem("Remove control point");
        removeControlPoint.setOnAction(event -> {
            // Combine two curves into one.
            assert (curveIndex > 0); // Should disable removeControlPoint if this is true.
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
            callback.modifySongPitchbend();
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
        return contextMenu;
    }

    private RadioMenuItem createCurveChoice(String label, String curveType, int curveIndex) {
        RadioMenuItem radioItem = new RadioMenuItem(label);
        radioItem.setOnAction(event -> {
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
            callback.modifySongPitchbend();
        });
        if (curves.get(curveIndex).getType() == curveType) {
            radioItem.setSelected(true);
        }
        return radioItem;
    }

    public PitchbendData getData(int notePos) {
        assert (curves.size() > 0);
        double startX = scaler.unscalePos(curves.get(0).getStartX()) - notePos;
        double endY = scaler.unscaleY(curves.get(curves.size() - 1).getEndY());
        ImmutableList.Builder<Double> widths = ImmutableList.builder();
        ImmutableList.Builder<Double> heights = ImmutableList.builder();
        ImmutableList.Builder<String> shapes = ImmutableList.builder();
        for (int i = 0; i < curves.size(); i++) {
            Curve line = curves.get(i);
            widths.add(scaler.unscaleX((line.getEndX() - line.getStartX())));
            if (i < curves.size() - 1) {
                heights.add(scaler.unscaleY(endY - line.getEndY()) / Quantizer.ROW_HEIGHT * 10);
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
