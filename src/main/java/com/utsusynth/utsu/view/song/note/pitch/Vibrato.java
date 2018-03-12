package com.utsusynth.utsu.view.song.note.pitch;

import java.util.ArrayList;
import java.util.function.Function;
import com.google.common.base.Optional;
import com.utsusynth.utsu.common.quantize.Quantizer;
import com.utsusynth.utsu.common.quantize.Scaler;
import javafx.scene.paint.Color;
import javafx.scene.shape.CubicCurveTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;

/**
 * Visual representation of the vibrato of a single note.
 */
public class Vibrato {
    private final int noteStartMs;
    private final int noteEndMs;
    private final double noteY;
    private final Path path;
    private final PitchbendCallback callback;
    private final Scaler scaler;
    private int[] vibrato;

    Vibrato(
            int noteStartMs,
            int noteEndMs,
            double noteY,
            PitchbendCallback callback,
            Scaler scaler,
            int[] vibrato) {
        this.noteStartMs = noteStartMs;
        this.noteEndMs = noteEndMs;
        this.noteY = noteY;
        this.path = new Path();
        path.setStroke(Color.DARKSLATEBLUE);
        this.callback = callback;
        this.scaler = scaler;

        this.vibrato = vibrato;
        refreshView();
    }

    Path getElement() {
        return path;
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
        refreshView();
    }

    public void clearVibrato() {
        vibrato = new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        callback.modifySongPitchbend();
        refreshView();
    }

    private void refreshView() {
        path.getElements().clear();
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
        ArrayList<Hump> humps = new ArrayList<>();

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
        humps.add(new Hump(firstHumpMs, vibrato[2] * amplitudeDir * (firstHumpMs / humpMs)));

        // Find each succeeding hump.
        double positionMs = firstHumpMs;
        while (positionMs + 1 < lengthMs) {
            double newHumpMs = Math.min(humpMs, lengthMs - positionMs);
            amplitudeDir *= -1;
            humps.add(new Hump(newHumpMs, vibrato[2] * amplitudeDir * (newHumpMs / humpMs)));
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
        for (Hump hump : humps) {
            positionMs = hump.adjust(widthMultiplier, heightMultiplier, positionMs);
        }
        // TODO: If necessary, resize humps at the end so total length is correct.

        // Draw path.
        double curStartMs = noteEndMs - lengthMs;
        path.getElements().add(new MoveTo(scaler.scaleX(curStartMs), noteY));
        for (Hump hump : humps) {
            hump.addToPath(path, curStartMs);
            curStartMs += hump.getWidthMs();
        }
    }

    private class Hump {
        private double startWidthMs;
        private double endWidthMs;
        private double amplitudeCents;
        private double startShiftCents = 0;
        private double endShiftCents = 0;

        public Hump(double widthMs, double amplitudeCents) {
            this.startWidthMs = widthMs / 2;
            this.endWidthMs = widthMs / 2;
            this.amplitudeCents = amplitudeCents;
        }

        public double getWidthMs() {
            return startWidthMs + endWidthMs;
        }

        public double adjust(
                Function<Double, Double> widthMultiplier,
                Function<Double, Double> heightMultiplier,
                double positionMs) {
            startWidthMs *= widthMultiplier.apply(positionMs);
            startShiftCents = vibrato[6] * heightMultiplier.apply(positionMs);
            endWidthMs *= widthMultiplier.apply(positionMs + startWidthMs);
            amplitudeCents *= heightMultiplier.apply(positionMs + startWidthMs);
            amplitudeCents += vibrato[6] * heightMultiplier.apply(positionMs + startWidthMs);
            endShiftCents = vibrato[6] * heightMultiplier.apply(positionMs + getWidthMs());
            return positionMs + getWidthMs();
        }

        public void addToPath(Path path, double startMs) {
            CubicCurveTo start = renderStart(startMs);
            CubicCurveTo end = renderEnd(startMs + startWidthMs);
            path.getElements().addAll(start, end);
        }

        private CubicCurveTo renderStart(double startMs) {
            double startX = scaler.scaleX(startMs);
            double endX = scaler.scaleX(startMs + startWidthMs);
            double startY = scaler.scaleY(startShiftCents / 100 * Quantizer.ROW_HEIGHT) + noteY;
            double endY = scaler.scaleY(amplitudeCents / 100 * Quantizer.ROW_HEIGHT) + noteY;
            double controlX_1 = startX + ((endX - startX) * 0.32613); // Sine approximation.
            double controlY_1 = startY + ((endY - startY) * 0.51228); // Sine approximation.
            double controlX_2 = startX + ((endX - startX) * 0.63809); // Sine approximation.
            double controlY_2 = endY; // Sine approximation.
            return new CubicCurveTo(controlX_1, controlY_1, controlX_2, controlY_2, endX, endY);
        }

        private CubicCurveTo renderEnd(double startMs) {
            double startX = scaler.scaleX(startMs);
            double endX = scaler.scaleX(startMs + endWidthMs);
            double startY = scaler.scaleY(amplitudeCents / 100 * Quantizer.ROW_HEIGHT) + noteY;
            double endY = scaler.scaleY(endShiftCents / 100 * Quantizer.ROW_HEIGHT) + noteY;
            double controlX_1 = startX + ((endX - startX) * 0.36191); // Sine approximation.
            double controlY_1 = startY; // Sine approximation.
            double controlX_2 = startX + ((endX - startX) * 0.67387); // Sine approximation.
            double controlY_2 = startY + ((endY - startY) * 0.48772); // Sine approximation.
            return new CubicCurveTo(controlX_1, controlY_1, controlX_2, controlY_2, endX, endY);
        }
    }
}
