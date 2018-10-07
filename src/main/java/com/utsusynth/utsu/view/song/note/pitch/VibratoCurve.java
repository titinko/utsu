package com.utsusynth.utsu.view.song.note.pitch;

import java.util.function.Function;
import com.utsusynth.utsu.common.quantize.Quantizer;
import com.utsusynth.utsu.common.quantize.Scaler;
import javafx.scene.shape.CubicCurveTo;
import javafx.scene.shape.Path;

/** A vibrato peak or valley. */
public class VibratoCurve {
    private final double noteY;
    private final Scaler scaler;

    private double startWidthMs;
    private double endWidthMs;
    private double amplitudeCents; // Positive if peak, negative if valley.
    private double startShiftCents = 0;
    private double endShiftCents = 0;

    public VibratoCurve(double noteY, Scaler scaler, double widthMs, double amplitudeCents) {
        this.noteY = noteY;
        this.scaler = scaler;

        this.startWidthMs = widthMs / 2;
        this.endWidthMs = widthMs / 2;
        this.amplitudeCents = amplitudeCents;
    }

    public double getWidthMs() {
        return startWidthMs + endWidthMs;
    }

    public double adjustWidth(Function<Double, Double> widthMultiplier, double positionMs) {
        double originalHalfMs = positionMs + startWidthMs;
        double originalEndMs = positionMs + getWidthMs();
        startWidthMs *= widthMultiplier.apply(positionMs);
        endWidthMs *= widthMultiplier.apply(originalHalfMs);
        return originalEndMs;
    }

    public double adjustHeight(
            Function<Double, Double> heightMultiplier,
            double pitchShift,
            double positionMs) {
        startShiftCents = pitchShift * heightMultiplier.apply(positionMs);
        amplitudeCents *= heightMultiplier.apply(positionMs + startWidthMs);
        amplitudeCents += pitchShift * heightMultiplier.apply(positionMs + startWidthMs);
        endShiftCents = pitchShift * heightMultiplier.apply(positionMs + getWidthMs());
        return positionMs + getWidthMs();
    }

    public void addToPath(Path path, double startMs) {
        CubicCurveTo start = renderStart(startMs);
        CubicCurveTo end = renderEnd(startMs + startWidthMs);
        path.getElements().addAll(start, end);
    }

    private CubicCurveTo renderStart(double startMs) {
        double startX = scaler.scalePos(startMs);
        double endX = scaler.scalePos(startMs + startWidthMs);
        double startY = -scaler.scaleY(startShiftCents / 100 * Quantizer.ROW_HEIGHT) + noteY;
        double endY = -scaler.scaleY(amplitudeCents / 100 * Quantizer.ROW_HEIGHT) + noteY;
        double controlX_1 = startX + ((endX - startX) * 0.32613); // Sine approximation.
        double controlY_1 = startY + ((endY - startY) * 0.51228); // Sine approximation.
        double controlX_2 = startX + ((endX - startX) * 0.63809); // Sine approximation.
        double controlY_2 = endY; // Sine approximation.
        return new CubicCurveTo(controlX_1, controlY_1, controlX_2, controlY_2, endX, endY);
    }

    private CubicCurveTo renderEnd(double startMs) {
        double startX = scaler.scalePos(startMs);
        double endX = scaler.scalePos(startMs + endWidthMs);
        double startY = -scaler.scaleY(amplitudeCents / 100 * Quantizer.ROW_HEIGHT) + noteY;
        double endY = -scaler.scaleY(endShiftCents / 100 * Quantizer.ROW_HEIGHT) + noteY;
        double controlX_1 = startX + ((endX - startX) * 0.36191); // Sine approximation.
        double controlY_1 = startY; // Sine approximation.
        double controlX_2 = startX + ((endX - startX) * 0.67387); // Sine approximation.
        double controlY_2 = startY + ((endY - startY) * 0.48772); // Sine approximation.
        return new CubicCurveTo(controlX_1, controlY_1, controlX_2, controlY_2, endX, endY);
    }
}

