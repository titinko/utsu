package com.utsusynth.utsu.view.song.note.pitch;

import java.util.*;
import java.util.function.Function;

import com.google.common.collect.ImmutableSet;
import com.utsusynth.utsu.common.i18n.Localizer;
import com.utsusynth.utsu.common.quantize.Quantizer;
import com.utsusynth.utsu.common.quantize.Scaler;
import com.utsusynth.utsu.view.song.track.TrackItem;
import javafx.beans.binding.DoubleExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ObservableDoubleValue;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

/**
 * Visual representation of the vibrato of a single note.
 */
public class Vibrato implements TrackItem {
    private final int noteStartMs;
    private final int noteEndMs;
    private final double noteY;
    private final BooleanProperty showEditor;

    private final PitchbendCallback callback;
    private final HashMap<Double, Path> drawnVibratoPaths;
    private final HashMap<Double, Group> drawnEditors;
    private final HashSet<Integer> drawnColumns;
    private final Localizer localizer;
    private final Scaler scaler;

    // UI-independent state.
    private Editor editor = null;
    private int[] vibrato;

    // UI-dependent state.
    private ContextMenu contextMenu;

    Vibrato(
            int noteStartMs,
            int noteEndMs,
            double noteY,
            PitchbendCallback callback,
            Localizer localizer,
            Scaler scaler,
            int[] startVibrato,
            BooleanProperty showEditor) {
        this.noteStartMs = noteStartMs;
        this.noteEndMs = noteEndMs;
        this.noteY = noteY;
        this.showEditor = showEditor;
        this.callback = callback;
        this.localizer = localizer;
        this.scaler = scaler;
        this.vibrato = startVibrato;
        drawnVibratoPaths = new HashMap<>();
        drawnEditors = new HashMap<>();
        drawnColumns = new HashSet<>();

        showEditor.addListener((event, oldValue, newValue) -> {
            if (newValue != oldValue) {
                redrawEditor();
            }
        });
    }

    @Override
    public TrackItemType getType() {
        return TrackItemType.PITCHBEND;
    }

    @Override
    public double getStartX() {
        double lengthMs = (noteEndMs - noteStartMs) * (vibrato[0] / 100.0); // Vibrato length.
        return scaler.scalePos(noteEndMs - lengthMs);
    }

    @Override
    public double getWidth() {
        return scaler.scaleX((noteEndMs - noteStartMs) * (vibrato[0] / 100.0));
    }

    public Group redraw() {
        return redraw(0);
    }

    @Override
    public Group redraw(double offsetX) {
        Path vibratoPath = new Path();
        vibratoPath.getStyleClass().add("pitchbend");
        vibratoPath.setMouseTransparent(true);
        redrawVibrato(vibratoPath, offsetX);
        drawnVibratoPaths.put(offsetX, vibratoPath);

        Group editorGroup = new Group();
        // Vibrato editor has its own context menu.
        editorGroup.setOnContextMenuRequested(event -> {
            if (contextMenu != null) {
                contextMenu.hide(); // Hide any existing context menu.
            }
            createContextMenu().show(editorGroup, event.getScreenX(), event.getScreenY());
        });
        redrawEditor(editorGroup, offsetX);
        drawnEditors.put(offsetX, editorGroup);

        // Create final group.
        return new Group(vibratoPath, editorGroup);
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
        double offsetX = colNum * scaler.scaleX(Quantizer.TRACK_COL_WIDTH);
        drawnVibratoPaths.remove(offsetX);
        drawnEditors.remove(offsetX);
    }

    @Override
    public void removeAllColumns() {
        drawnColumns.clear();
        drawnVibratoPaths.clear();
        drawnEditors.clear();
    }

    /** Fetch just the vibrato, never showing the editor. */
    public Path getVibratoElement() {
        if (drawnVibratoPaths.isEmpty()) {
            redraw();
        }
        return drawnVibratoPaths.values().iterator().next();
    }

    /** Fetch just the editor, never showing the vibrato. */
    public Group getEditorElement() {
        if (drawnEditors.isEmpty()) {
            redraw();
        }
        return drawnEditors.values().iterator().next();
    }

    public Optional<int[]> getVibrato() {
        // Return absent if vibrato cannot render properly.
        if (vibrato.length != 10 || vibrato[0] == 0) {
            return Optional.empty();
        }
        for (int value : vibrato) {
            if (value != 0) {
                return Optional.of(Arrays.copyOf(vibrato, vibrato.length));
            }
        }
        // Return absent if all vibrato values are 0.
        return Optional.empty();
    }

    public void addDefaultVibrato() {
        int[] oldVibrato = Arrays.copyOf(vibrato, vibrato.length);
        vibrato = new int[10];
        vibrato[0] = 70; // Vibrato length (% of note)
        vibrato[1] = 185; // Cycle length (ms, range of 10 to 512)
        vibrato[2] = 40; // Amplitude (cents) (range of 5 to 200)
        vibrato[3] = 20; // Phase in (% of vibrato)
        vibrato[4] = 20; // Phase out (% of vibrato)
        vibrato[5] = 0; // Phase shift (% of cycle)
        vibrato[6] = 0; // Pitch shift from note (cents, range of -100 to 100)
        vibrato[7] = 100; // Vibrato inversion, unused.
        vibrato[8] = 0; // Frequency slope (range of -100 to 100)
        vibrato[9] = 0; // Unused.
        callback.modifySongVibrato(oldVibrato, Arrays.copyOf(vibrato, vibrato.length));
        redrawVibrato();
        redrawEditor();

        // Bounds might have been changed, so refresh track columns.
        callback.readjust();
    }

    public void clearVibrato() {
        int[] oldVibrato = Arrays.copyOf(vibrato, vibrato.length);
        int[] newVibrato = new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        callback.modifySongVibrato(oldVibrato, newVibrato);
        vibrato = Arrays.copyOf(newVibrato, newVibrato.length);
        redrawVibrato();
        redrawEditor();

        // Bounds might have been changed, so refresh track columns.
        callback.readjust();
    }

    public void adjustVibrato(int index, int newValue) {
        if (index < 0 || index >= vibrato.length || vibrato[index] == newValue) {
            return;
        }
        vibrato[index] = newValue;
        redrawVibrato();
        // This method does not include redrawing the editor.
    }

    public void redrawEditor() {
        // Only use when vibrato is edited by something besides the editor.
        editor = showEditor.get() && getVibrato().isPresent() ? new Editor() : null;
        for (double offsetX : drawnEditors.keySet()) {
            redrawEditor(drawnEditors.get(offsetX), offsetX);
        }
    }

    private void redrawEditor(Group editorGroup, double offsetX) {
        if (showEditor.get() && getVibrato().isPresent()) {
            if (editor == null) {
                editor = new Editor(); // Does not re-create editor unless needed.
            }
            editorGroup.getChildren().setAll(editor.render(offsetX));
        } else {
            editor = null;
            editorGroup.getChildren().clear();
        }
    }

    private ContextMenu createContextMenu() {
        // Vibrato editor has its own context menu.
        CheckMenuItem vibratoEditorMenuItem = new CheckMenuItem("Vibrato Editor");
        vibratoEditorMenuItem.selectedProperty().bindBidirectional(showEditor);
        contextMenu = new ContextMenu(vibratoEditorMenuItem);
        contextMenu.setOnShowing(event ->
                vibratoEditorMenuItem.setText(localizer.getMessage("song.note.vibratoEditor")));
        return contextMenu;
    }

    private void redrawVibrato() {
        for (double offsetX : drawnVibratoPaths.keySet()) {
            redrawVibrato(drawnVibratoPaths.get(offsetX), offsetX);
        }
    }

    private void redrawVibrato(Path vibratoPath, double offsetX) {
        // TODO: Validate that vibrato values are within correct ranges.
        vibratoPath.getElements().clear();
        if (vibrato.length != 10) {
            // Ensure that vibrato values are valid.
            vibrato = new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        }
        if (vibrato[0] == 0) {
            // Show nothing for vibrato of length 0.
            return;
        }
        double humpMs = vibrato[1] / 2.0; // One hump is half a cycle.
        double lengthMs = (noteEndMs - noteStartMs) * (vibrato[0] / 100.0); // Vibrato length.
        ArrayList<VibratoCurve> humps = new ArrayList<>();

        // Calculate first hump
        int amplitudeDir = 1; // Defaults to positive when phase is 0 or 100.
        double firstHumpMs = humpMs; // Default when phase is 0 or 100.
        if (vibrato[5] > 0 && vibrato[5] < 50) {
            // Initial direction is up.
            amplitudeDir = 1;
            firstHumpMs = firstHumpMs * ((50 - vibrato[5]) / 50.0);
        } else if (vibrato[5] >= 50 && vibrato[5] < 100) {
            // Initial direction is down.
            amplitudeDir = -1;
            firstHumpMs = firstHumpMs * ((100 - vibrato[5]) / 50.0);
        }
        // Find hump length and compare with vibrato length. Truncate if needed.
        firstHumpMs = Math.min(firstHumpMs, lengthMs);
        humps.add(
                new VibratoCurve(
                        noteY,
                        scaler,
                        firstHumpMs,
                        vibrato[2] * amplitudeDir * (firstHumpMs / humpMs)));

        // Find each succeeding hump.
        double positionMs = firstHumpMs;
        while (positionMs + 1 < lengthMs) {
            double newHumpMs = Math.min(humpMs, lengthMs - positionMs);
            amplitudeDir *= -1;
            humps.add(
                    new VibratoCurve(
                            noteY,
                            scaler,
                            newHumpMs,
                            vibrato[2] * amplitudeDir * (newHumpMs / humpMs)));
            positionMs += newHumpMs;
        }

        // Cycle through humps again, adjusting for amplitude/frequency changes.
        double originalPositionMs = 0;
        double newPositionMs = 0;
        // Width.
        double startMultiplier = (-1 * vibrato[8] / 125.0) + 1; // Min is 0.2, max is 1.8
        double endMultiplier = (vibrato[8] / 125.0) + 1;
        double slope = (endMultiplier - startMultiplier) / lengthMs;
        Function<Double, Double> widthMultiplier = posMs -> startMultiplier + (posMs * slope);
        // Height.
        double phaseInMs = vibrato[3] / 100.0 * lengthMs;
        double phaseOutMs = vibrato[4] / 100.0 * lengthMs;
        Function<Double, Double> heightMultiplier = posMs -> {
            double multiplier = 1.0;
            if (posMs < phaseInMs) {
                multiplier *= posMs / phaseInMs;
            }
            if (posMs > lengthMs - phaseOutMs) {
                multiplier *= (lengthMs - posMs) / phaseOutMs;
            }
            return multiplier;
        };
        for (VibratoCurve hump : humps) {
            originalPositionMs = hump.adjustWidth(widthMultiplier, originalPositionMs);
            newPositionMs = hump.adjustHeight(heightMultiplier, vibrato[6], newPositionMs);
        }
        // TODO: Resize humps at the end so total length is correct.

        // Draw path.
        double curStartMs = noteEndMs - lengthMs;
        vibratoPath.getElements().add(
                new MoveTo(scaler.scalePos(curStartMs) - offsetX, noteY));
        for (VibratoCurve hump : humps) {
            hump.addToPath(vibratoPath, curStartMs, offsetX);
            curStartMs += hump.getWidthMs();
        }
    }

    // Vibrato editor. Lines can be dragged around to alter vibrato values.
    private class Editor {
        private final DoubleProperty minX;
        private final DoubleProperty maxX;
        private final DoubleProperty baseY;

        private final DoubleProperty centerY;
        private final DoubleProperty amplitudeY;

        private final DoubleProperty startX;
        private final DoubleProperty fadeInX;
        private final DoubleProperty fadeOutX;

        private final DoubleProperty maxSliderX; // Max value for the sliders below.
        private final DoubleProperty frqX;
        private final DoubleProperty phaseX;
        private final DoubleProperty frqSlopeX;

        // Temporary cache values.
        private boolean changed = false;
        int[] startVibrato;

        public Editor() {
            // Values that don't change.
            this.minX = new SimpleDoubleProperty(scaler.scalePos(noteStartMs));
            this.maxX = new SimpleDoubleProperty(scaler.scalePos(noteEndMs));
            this.baseY = new SimpleDoubleProperty(noteY);

            this.centerY = new SimpleDoubleProperty(
                    scaler.scaleY(-vibrato[6] / 100.0 * Quantizer.ROW_HEIGHT) + noteY);
            this.amplitudeY = new SimpleDoubleProperty(
                    scaler.scaleY(vibrato[2] / 100.0 * Quantizer.ROW_HEIGHT));

            double lengthMs = (noteEndMs - noteStartMs) * (vibrato[0] / 100.0); // Vibrato length.
            this.startX = new SimpleDoubleProperty(scaler.scalePos(noteEndMs - lengthMs));
            double fadeInMs = noteEndMs - lengthMs + (lengthMs * (vibrato[3] / 100.0));
            this.fadeInX = new SimpleDoubleProperty(scaler.scalePos(fadeInMs));
            double fadeOutMs = noteEndMs - (lengthMs * (vibrato[4] / 100.0));
            this.fadeOutX = new SimpleDoubleProperty(scaler.scalePos(fadeOutMs));

            this.maxSliderX = new SimpleDoubleProperty(
                    Math.min(maxX.get(), startX.get() + scaler.scaleX(Quantizer.COL_WIDTH)));
            this.frqX = new SimpleDoubleProperty(
                    startX.get() + (maxSliderX.get() - startX.get()) * (vibrato[1] - 10) / 448.0);
            this.phaseX = new SimpleDoubleProperty(
                    startX.get() + (maxSliderX.get() - startX.get()) * vibrato[5] / 100.0);
            this.frqSlopeX = new SimpleDoubleProperty(
                    startX.get() + (maxSliderX.get() - startX.get()) * (vibrato[8] + 100) / 200.0);

            // Set event handlers that change and re-draw vibrato.
            centerY.addListener(event -> adjustVibrato(6, yToCents(baseY.get() - centerY.get())));
            amplitudeY.addListener(event -> adjustVibrato(2, yToCents(amplitudeY.get())));
            startX.addListener(event -> {
                double ratio = (maxX.get() - startX.get()) / (maxX.get() - minX.get());
                adjustVibrato(0, (int) Math.round(ratio * 100)); // Percent of note.
            });
            fadeInX.addListener(event -> {
                double ratio = (fadeInX.get() - startX.get()) / (maxX.get() - startX.get());
                adjustVibrato(3, (int) Math.round(ratio * 100)); // Percent of vibrato.
            });
            fadeOutX.addListener(event -> {
                double ratio = (maxX.get() - fadeOutX.get()) / (maxX.get() - startX.get());
                adjustVibrato(4, (int) Math.round(ratio * 100)); // Percent of vibrato.
            });
            frqX.addListener(event -> {
                double ratio = (frqX.get() - startX.get()) / (maxSliderX.get() - startX.get());
                adjustVibrato(1, (int) Math.round(ratio * 448 + 10));
            });
            phaseX.addListener(event -> {
                double ratio = (phaseX.get() - startX.get()) / (maxSliderX.get() - startX.get());
                adjustVibrato(5, (int) Math.round(ratio * 100));
            });
            frqSlopeX.addListener(event -> {
                double ratio = (frqSlopeX.get() - startX.get()) / (maxSliderX.get() - startX.get());
                adjustVibrato(8, (int) Math.round(ratio * 200 - 100));
            });
        }

        public Node[] render(double offsetX) {
            ObservableDoubleValue offsetMaxX = maxX.subtract(offsetX);
            ObservableDoubleValue offsetStartX = startX.subtract(offsetX);
            ObservableDoubleValue offsetFadeInX = fadeInX.subtract(offsetX);
            ObservableDoubleValue offsetFadeOutX = fadeOutX.subtract(offsetX);
            ObservableDoubleValue offsetMaxSliderX = maxSliderX.subtract(offsetX);

            ObservableDoubleValue highY = centerY.subtract(amplitudeY);
            ObservableDoubleValue lowY = centerY.add(amplitudeY);
            ObservableDoubleValue highBaseY = baseY.subtract(centsToY(75));
            ObservableDoubleValue lowBaseY = baseY.add(centsToY(75));
            Node[] nodes = new Node[16];

            // Non-draggable nodes.
            nodes[0] = createLine(offsetStartX, baseY, offsetFadeInX, highY); // Start -> Top
            nodes[1] = createLine(offsetStartX, baseY, offsetFadeInX, centerY); // Start -> Center
            nodes[2] = createLine(offsetStartX, baseY, offsetFadeInX, lowY); // Start -> Bottom
            nodes[3] = createLine(offsetFadeOutX, highY, offsetMaxX, baseY); // Top -> End
            nodes[4] = createLine(offsetFadeOutX, centerY, offsetMaxX, baseY); // Center -> End
            nodes[5] = createLine(offsetFadeOutX, lowY, offsetMaxX, baseY); // Bottom -> End
            Line endOfSlider = createLine(offsetMaxSliderX, highBaseY, offsetMaxSliderX, lowBaseY);
            endOfSlider.setStroke(Color.ROYALBLUE);
            nodes[6] = endOfSlider;

            // Draggable nodes.
            nodes[7] = createLine(
                    offsetFadeInX, centerY, offsetFadeOutX, centerY, Cursor.V_RESIZE); // Center
            setUpBackendUpdate(nodes[7]);
            nodes[7].setOnMouseDragged(event -> {
                double y = event.getY();
                if (y >= baseY.get() - centsToY(100) && y <= baseY.get() + centsToY(100)) {
                    changed = true;
                    centerY.set(y);
                }
            });
            nodes[8] = createLine(
                    offsetFadeInX, highY, offsetFadeOutX, highY, Cursor.V_RESIZE); // Top
            setUpBackendUpdate(nodes[8]);
            nodes[8].setOnMouseDragged(event -> {
                double y = event.getY();
                if (y >= centerY.get() - centsToY(200) && y <= centerY.get() - centsToY(5)) {
                    changed = true;
                    amplitudeY.set(centerY.get() - y);
                }
            });
            nodes[9] = createLine(
                    offsetFadeInX, lowY, offsetFadeOutX, lowY, Cursor.V_RESIZE); // Bottom
            setUpBackendUpdate(nodes[9]);
            nodes[9].setOnMouseDragged(event -> {
                double y = event.getY();
                if (y >= centerY.get() + centsToY(5) && y <= centerY.get() + centsToY(200)) {
                    changed = true;
                    amplitudeY.set(y - centerY.get());
                }
            });
            nodes[10] = createLine(
                    offsetFadeInX, highY, offsetFadeInX, lowY, Cursor.H_RESIZE); // Fade in
            setUpBackendUpdate(nodes[10]);
            nodes[10].setOnMouseDragged(event -> {
                double x = event.getX() + offsetX;
                if (x > startX.get() && x < maxX.get()) {
                    changed = true;
                    fadeInX.set(x);
                }
            });
            nodes[11] = createLine(
                    offsetFadeOutX, highY, offsetFadeOutX, lowY, Cursor.H_RESIZE); // Fade out
            setUpBackendUpdate(nodes[11]);
            nodes[11].setOnMouseDragged(event -> {
                double x = event.getX() + offsetX;
                if (x > startX.get() && x < maxX.get()) {
                    changed = true;
                    fadeOutX.set(x);
                }
            });
            nodes[12] = createLine(
                    offsetStartX, highBaseY, offsetStartX, lowBaseY, Cursor.H_RESIZE); // Start
            setUpBackendUpdate(nodes[12]);
            nodes[12].setOnMouseDragged(event -> {
                double x = event.getX() + offsetX;
                if (x > minX.get() && x < maxX.get()) {
                    changed = true;
                    startX.set(x);
                    fadeInX.set(x + ((maxX.get() - x) * vibrato[3] / 100.0));
                    fadeOutX.set(maxX.get() - ((maxX.get() - x) * vibrato[4] / 100.0));
                    maxSliderX.set(Math.min(maxX.get(), x + scaler.scaleX(Quantizer.COL_WIDTH)));
                    frqX.set(x + (maxSliderX.get() - x) * (vibrato[1] - 10) / 448.0);
                    phaseX.set(x + (maxSliderX.get() - x) * vibrato[5] / 100.0);
                    frqSlopeX.set(x + (maxSliderX.get() - x) * (vibrato[8] + 100) / 200.0);
                }
            });
            // Frequency slider.
            nodes[13] = createSlider(frqX, offsetX, baseY.subtract(centsToY(30)), "F");
            // Phase shift slider.
            nodes[14] = createSlider(phaseX, offsetX, baseY, "P");
            // Frequency slope slider.
            nodes[15] = createSlider(frqSlopeX, offsetX, baseY.add(centsToY(30)), "F'");
            return nodes;
        }

        private Line createLine(
                ObservableDoubleValue x1,
                ObservableDoubleValue y1,
                ObservableDoubleValue x2,
                ObservableDoubleValue y2,
                Cursor cursor) {
            Line line = createLine(x1, y1, x2, y2);
            line.setCursor(cursor);
            return line;
        }

        private Line createLine(
                ObservableDoubleValue x1,
                ObservableDoubleValue y1,
                ObservableDoubleValue x2,
                ObservableDoubleValue y2) {
            Line line = new Line();
            line.setStroke(Color.CRIMSON);
            line.setStrokeWidth(2);
            line.startXProperty().bind(x1);
            line.startYProperty().bind(y1);
            line.endXProperty().bind(x2);
            line.endYProperty().bind(y2);
            return line;
        }

        private StackPane createSlider(
                DoubleProperty xProp,
                double offsetX,
                DoubleExpression y,
                String label) {
            double radius = 5;
            Circle point = new Circle(radius);
            point.setStroke(Color.CRIMSON);
            point.setFill(Color.WHITE);
            point.setCursor(Cursor.HAND);

            Text text = new Text(label);
            text.setFont(Font.font(9));
            text.setFill(Color.CRIMSON);
            text.setMouseTransparent(true);

            StackPane slider = new StackPane(point, text);
            slider.translateXProperty().bind(xProp.subtract(radius + offsetX));
            slider.translateYProperty().bind(y.subtract(radius));
            setUpBackendUpdate(slider);
            slider.setOnMouseDragged(event -> {
                double newX = event.getX() + slider.getTranslateX() + offsetX;
                if (newX >= startX.get() && newX <= maxSliderX.get()) {
                    changed = true;
                    xProp.set(newX);
                }
            });
            return slider;
        }

        private void setUpBackendUpdate(Node node) {
            node.setOnMousePressed(event -> {
                changed = false;
                startVibrato = Arrays.copyOf(vibrato, vibrato.length);
            });
            node.setOnMouseReleased(event -> {
                if (changed) {
                    callback.modifySongVibrato(
                            startVibrato,
                            Arrays.copyOf(vibrato, vibrato.length));
                }
            });
        }

        private double centsToY(double cents) {
            return scaler.scaleY(cents / 100.0 * Quantizer.ROW_HEIGHT);
        }

        private int yToCents(double y) {
            return (int) Math.round(scaler.unscaleY(y) / Quantizer.ROW_HEIGHT * 100);
        }
    }
}
