package com.utsusynth.utsu.view.song.note.pitch;

import java.util.ArrayList;
import java.util.function.Function;
import com.google.common.base.Optional;
import com.utsusynth.utsu.common.quantize.Quantizer;
import com.utsusynth.utsu.common.quantize.Scaler;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ObservableDoubleValue;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;

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
            int[] vibrato,
            BooleanProperty showEditor) {
        this.noteStartMs = noteStartMs;
        this.noteEndMs = noteEndMs;
        this.noteY = noteY;
        this.showEditor = showEditor;
        this.callback = callback;
        this.scaler = scaler;
        this.vibrato = vibrato;

        vibratoPath = new Path();
        vibratoPath.setStroke(Color.DARKSLATEBLUE);
        vibratoPath.setMouseTransparent(true);
        redrawVibrato();

        editor = Optional.absent();
        editorGroup = new Group();
        redrawEditor();
        showEditor.addListener((event, oldValue, newValue) -> {
            if (newValue != oldValue) {
                redrawEditor();
            }
        });
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
        vibrato[1] = 185; // Cycle length (ms)
        vibrato[2] = 40; // Amplitude (cents)
        vibrato[3] = 20; // Phase in (% of vibrato)
        vibrato[4] = 20; // Phase out (% of vibrato)
        vibrato[5] = 0; // Phase shift (% of cycle)
        vibrato[6] = 0; // Pitch shift from note (cents)
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

    void redrawVibrato() {
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
        if (showEditor.get()) {
            editor = Optional.of(new Editor());
            editorGroup.getChildren().setAll(editor.get().render());
        } else {
            editor = Optional.absent();
            editorGroup.getChildren().clear();
        }
    }

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
        }

        public Line[] render() {
            System.out.println(minX);
            DoubleBinding highY = centerY.add(amplitudeY);
            DoubleBinding lowY = centerY.subtract(amplitudeY);
            Line[] lines = new Line[11];
            lines[0] = createLine(startX, baseY, fadeInX, highY); // Start -> Top
            lines[1] = createLine(startX, baseY, fadeInX, centerY); // Start -> Center
            lines[2] = createLine(startX, baseY, fadeInX, lowY); // Start -> Bottom
            lines[3] = createLine(fadeOutX, highY, maxX, baseY); // Top -> End
            lines[4] = createLine(fadeOutX, centerY, maxX, baseY); // Center -> End
            lines[5] = createLine(fadeOutX, lowY, maxX, baseY); // Bottom -> End

            lines[6] = createLine(fadeInX, centerY, fadeOutX, centerY); // Center
            lines[7] = createLine(fadeInX, highY, fadeOutX, highY); // Top
            lines[8] = createLine(fadeInX, lowY, fadeOutX, lowY); // Bottom
            lines[9] = createLine(fadeInX, highY, fadeInX, lowY); // Fade in
            lines[10] = createLine(fadeOutX, highY, fadeOutX, lowY); // Fade out
            return lines;
        }

        private Line createLine(
                ObservableDoubleValue x1,
                ObservableDoubleValue y1,
                ObservableDoubleValue x2,
                ObservableDoubleValue y2) {
            Line line = new Line();
            line.setStroke(Color.STEELBLUE);
            line.setStrokeWidth(1.8);
            line.startXProperty().bind(x1);
            line.startYProperty().bind(y1);
            line.endXProperty().bind(x2);
            line.endYProperty().bind(y2);
            return line;
        }
    }
}
