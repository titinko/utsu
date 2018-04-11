package com.utsusynth.utsu.view.song.note.pitch;

import java.util.ArrayList;
import java.util.function.Function;
import com.google.common.base.Optional;
import com.utsusynth.utsu.common.quantize.Quantizer;
import com.utsusynth.utsu.common.quantize.Scaler;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ObservableDoubleValue;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Shape;

/**
 * Visual representation of the vibrato of a single note.
 */
public class Vibrato {
    private final int noteStartMs;
    private final int noteEndMs;
    private final double noteY;
    private final BooleanProperty showEditor;

    private final Path vibratoPath;
    private final Group editorGroup;
    private final PitchbendCallback callback;
    private final Scaler scaler;

    private Optional<Editor> editor;
    private int[] vibrato;

    Vibrato(
            int noteStartMs,
            int noteEndMs,
            double noteY,
            PitchbendCallback callback,
            Scaler scaler,
            int[] startVibrato,
            BooleanProperty showEditor) {
        this.noteStartMs = noteStartMs;
        this.noteEndMs = noteEndMs;
        this.noteY = noteY;
        this.showEditor = showEditor;
        this.callback = callback;
        this.scaler = scaler;
        this.vibrato = startVibrato;

        vibratoPath = new Path();
        vibratoPath.setStroke(Color.DARKSLATEBLUE);
        vibratoPath.setMouseTransparent(true);
        redrawVibrato();

        editor = Optional.absent();
        editorGroup = new Group();
        editorGroup.setOnMouseExited(event -> editorGroup.getScene().setCursor(Cursor.DEFAULT));
        redrawEditor();
        showEditor.addListener((event, oldValue, newValue) -> {
            if (newValue != oldValue) {
                redrawEditor();
            }
        });

        // Vibrato editor has its own context menu.
        CheckMenuItem vibratoEditorMenuItem = new CheckMenuItem("Vibrato Editor");
        vibratoEditorMenuItem.selectedProperty().bindBidirectional(showEditor);
        ContextMenu menu = new ContextMenu(vibratoEditorMenuItem);
        editorGroup.setOnContextMenuRequested(
                event -> menu.show(editorGroup, event.getScreenX(), event.getScreenY()));
    }

    Group getElement() {
        return new Group(vibratoPath, editorGroup);
    }

    public Optional<int[]> getVibrato() {
        for (int value : vibrato) {
            if (value != 0) {
                return Optional.of(vibrato);
            }
        }
        // Return absent if all vibrato values are 0.
        return Optional.absent();
    }

    public void addDefaultVibrato() {
        vibrato = new int[10];
        vibrato[0] = 70; // Vibrato length (% of note)
        vibrato[1] = 185; // Cycle length (ms, range of 64 to 512)
        vibrato[2] = 40; // Amplitude (cents) (range of 5 to 200)
        vibrato[3] = 20; // Phase in (% of vibrato)
        vibrato[4] = 20; // Phase out (% of vibrato)
        vibrato[5] = 0; // Phase shift (% of cycle)
        vibrato[6] = 0; // Pitch shift from note (cents, range of -100 to 100)
        vibrato[7] = 100; // Vibrato inversion, unused.
        vibrato[8] = 0; // Frequency slope (range of -100 to 100)
        vibrato[9] = 0; // Unused.
        callback.modifySongPitchbend();
        redrawVibrato();
        redrawEditor();
    }

    public void clearVibrato() {
        vibrato = new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        callback.modifySongPitchbend();
        redrawVibrato();
        redrawEditor();
    }

    private void redrawVibrato() {
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
            // Initial direction is positive.
            amplitudeDir = 1;
            firstHumpMs = firstHumpMs * ((50 - vibrato[5]) / 50.0);
        } else if (vibrato[5] >= 50 && vibrato[5] < 100) {
            // Initial direction is negative
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
        positionMs = 0;
        // Width.
        double startMultiplier = (-1 * vibrato[8] / 200.0) + 1; // Min is 0.5, max is 1.5
        double endMultiplier = (vibrato[8] / 200.0) + 1;
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
            positionMs = hump.adjust(widthMultiplier, heightMultiplier, vibrato[6], positionMs);
        }
        // TODO: If necessary, resize humps at the end so total length is correct.

        // Draw path.
        double curStartMs = noteEndMs - lengthMs;
        vibratoPath.getElements().add(new MoveTo(scaler.scaleX(curStartMs), noteY));
        for (VibratoCurve hump : humps) {
            hump.addToPath(vibratoPath, curStartMs);
            curStartMs += hump.getWidthMs();
        }
    }

    private void redrawEditor() {
        // Only use when vibrato is edited by something besides the editor.
        if (showEditor.get() && getVibrato().isPresent()) {
            editor = Optional.of(new Editor());
            editorGroup.getChildren().setAll(editor.get().render());
        } else {
            editor = Optional.absent();
            editorGroup.getChildren().clear();
        }
    }

    // Vibrato editor. Lines can be dragged around to alter vibrato values.
    private class Editor {
        private final ObservableDoubleValue minX;
        private final ObservableDoubleValue maxX;
        private final ObservableDoubleValue baseY;

        private final DoubleProperty startX;
        private final DoubleProperty fadeInX;
        private final DoubleProperty fadeOutX;

        private final DoubleProperty centerY;
        private final DoubleProperty amplitudeY;

        public Editor() {
            // Values that don't change.
            this.minX = new SimpleDoubleProperty(scaler.scaleX(noteStartMs));
            this.maxX = new SimpleDoubleProperty(scaler.scaleX(noteEndMs));
            this.baseY = new SimpleDoubleProperty(noteY);

            double lengthMs = (noteEndMs - noteStartMs) * (vibrato[0] / 100.0); // Vibrato length.
            this.startX = new SimpleDoubleProperty(scaler.scaleX(noteEndMs - lengthMs));
            this.fadeInX = new SimpleDoubleProperty(
                    scaler.scaleX(noteEndMs - lengthMs + (lengthMs * (vibrato[3] / 100.0))));
            this.fadeOutX = new SimpleDoubleProperty(
                    scaler.scaleX(noteEndMs - (lengthMs * (vibrato[4] / 100.0))));

            this.centerY = new SimpleDoubleProperty(
                    scaler.scaleY(vibrato[6] / 100.0 * Quantizer.ROW_HEIGHT) + noteY);
            this.amplitudeY = new SimpleDoubleProperty(
                    scaler.scaleY(vibrato[2] / 100.0 * Quantizer.ROW_HEIGHT));

            // Set event handlers that change and re-draw vibrato.
            startX.addListener(event -> {
                double ratio = (maxX.get() - startX.get()) / (maxX.get() - minX.get());
                int percentOfNote = (int) Math.round(ratio * 100);
                if (percentOfNote != vibrato[0]) {
                    vibrato[0] = percentOfNote;
                    callback.modifySongPitchbend();
                    redrawVibrato();
                }
            });
            fadeInX.addListener(event -> {
                double ratio = (fadeInX.get() - startX.get()) / (maxX.get() - startX.get());
                int percentOfVibrato = (int) Math.round(ratio * 100);
                if (percentOfVibrato != vibrato[3]) {
                    vibrato[3] = percentOfVibrato;
                    callback.modifySongPitchbend();
                    redrawVibrato();
                }
            });
            fadeOutX.addListener((event, oldFadeOutX, newFadeOutX) -> {
                double ratio = (maxX.get() - fadeOutX.get()) / (maxX.get() - startX.get());
                int percentOfVibrato = (int) Math.round(ratio * 100);
                if (percentOfVibrato != vibrato[4]) {
                    vibrato[4] = percentOfVibrato;
                    callback.modifySongPitchbend();
                    redrawVibrato();
                }
            });
            centerY.addListener((event, oldCenterY, newCenterY) -> {
                int pitchShiftCents = yToCents(centerY.get() - baseY.get());
                if (pitchShiftCents != vibrato[6]) {
                    vibrato[6] = pitchShiftCents;
                    callback.modifySongPitchbend();
                    redrawVibrato();
                }
            });
            amplitudeY.addListener((event, oldAmplitudeY, newAmplitudeY) -> {
                int amplitudeCents = yToCents(amplitudeY.get());
                if (amplitudeCents != vibrato[2]) {
                    vibrato[2] = amplitudeCents;
                    callback.modifySongPitchbend();
                    redrawVibrato();
                }
            });
        }

        public Shape[] render() {
            System.out.println(minX);
            ObservableDoubleValue highY = centerY.subtract(amplitudeY);
            ObservableDoubleValue lowY = centerY.add(amplitudeY);
            Shape[] shapes = new Shape[12];

            // Non-draggable shapes.
            shapes[0] = createLine(startX, baseY, fadeInX, highY); // Start -> Top
            shapes[1] = createLine(startX, baseY, fadeInX, centerY); // Start -> Center
            shapes[2] = createLine(startX, baseY, fadeInX, lowY); // Start -> Bottom
            shapes[3] = createLine(fadeOutX, highY, maxX, baseY); // Top -> End
            shapes[4] = createLine(fadeOutX, centerY, maxX, baseY); // Center -> End
            shapes[5] = createLine(fadeOutX, lowY, maxX, baseY); // Bottom -> End

            // Draggable shapes.
            shapes[6] = createLine(fadeInX, centerY, fadeOutX, centerY, Cursor.V_RESIZE); // Center
            shapes[6].setOnMouseDragged(event -> {
                double y = event.getY();
                if (y >= baseY.get() - centsToY(100) && y <= baseY.get() + centsToY(100)) {
                    centerY.set(y);
                }
            });
            shapes[7] = createLine(fadeInX, highY, fadeOutX, highY, Cursor.V_RESIZE); // Top
            shapes[7].setOnMouseDragged(event -> {
                double y = event.getY();
                if (y >= centerY.get() - centsToY(200) && y <= centerY.get() - centsToY(5)) {
                    amplitudeY.set(centerY.get() - y);
                }
            });
            shapes[8] = createLine(fadeInX, lowY, fadeOutX, lowY, Cursor.V_RESIZE); // Bottom
            shapes[8].setOnMouseDragged(event -> {
                double y = event.getY();
                if (y >= centerY.get() + centsToY(5) && y <= centerY.get() + centsToY(200)) {
                    amplitudeY.set(y - centerY.get());
                }
            });
            shapes[9] = createPoint(startX, baseY, Cursor.HAND); // Start point
            shapes[9].setOnMouseDragged(event -> {
                double x = event.getX();
                if (x > minX.get() && x < maxX.get()) {
                    startX.set(x);
                    fadeInX.set(x + ((maxX.get() - x) * vibrato[3] / 100.0));
                    fadeOutX.set(maxX.get() - ((maxX.get() - x) * vibrato[4] / 100.0));
                }
            });
            shapes[10] = createLine(fadeInX, highY, fadeInX, lowY, Cursor.H_RESIZE); // Fade in
            shapes[10].setOnMouseDragged(event -> {
                double x = event.getX();
                if (x > startX.get() && x < maxX.get()) {
                    fadeInX.set(x);
                }
            });
            shapes[11] = createLine(fadeOutX, highY, fadeOutX, lowY, Cursor.H_RESIZE); // Fade out
            shapes[11].setOnMouseDragged(event -> {
                double x = event.getX();
                if (x > startX.get() && x < maxX.get()) {
                    fadeOutX.set(x);
                }
            });
            return shapes;
        }

        private Line createLine(
                ObservableDoubleValue x1,
                ObservableDoubleValue y1,
                ObservableDoubleValue x2,
                ObservableDoubleValue y2,
                Cursor cursor) {
            Line line = createLine(x1, y1, x2, y2);
            line.setOnMouseEntered(event -> line.getScene().setCursor(cursor));
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

        private Circle createPoint(
                ObservableDoubleValue x,
                ObservableDoubleValue y,
                Cursor cursor) {
            Circle point = new Circle(3);
            point.setStroke(Color.CRIMSON);
            point.setFill(Color.CRIMSON);
            point.centerXProperty().bind(x);
            point.centerYProperty().bind(y);
            point.setOnMouseEntered(event -> point.getScene().setCursor(cursor));
            return point;
        }

        private double centsToY(double cents) {
            return scaler.scaleY(cents / 100.0 * Quantizer.ROW_HEIGHT);
        }

        private int yToCents(double y) {
            return (int) Math.round(scaler.unscaleY(y) / Quantizer.ROW_HEIGHT * 100);
        }
    }
}
