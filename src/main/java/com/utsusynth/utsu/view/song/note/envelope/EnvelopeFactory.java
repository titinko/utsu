package com.utsusynth.utsu.view.song.note.envelope;

import com.google.inject.Inject;
import com.utsusynth.utsu.common.data.EnvelopeData;
import com.utsusynth.utsu.common.quantize.Scaler;
import com.utsusynth.utsu.view.song.note.Note;
import javafx.beans.property.DoubleProperty;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.util.Pair;

public class EnvelopeFactory {
    private final Scaler scaler;

    @Inject
    public EnvelopeFactory(Scaler scaler) {
        this.scaler = scaler;
    }

    public Envelope createEnvelope(Note note, EnvelopeData envelope, EnvelopeCallback callback) {
        double preutter = envelope.getPreutter().isPresent() ? envelope.getPreutter().get() : 0;
        double length = envelope.getLength().isPresent() ? envelope.getLength().get() : 0;
        double startPos = note.getAbsPositionMs() - preutter;
        double endPos = startPos + length;

        double[] widths = envelope.getWidths();
        double p1 = widths[0];
        double p2 = widths[1];
        double p3 = widths[2];
        double p4 = widths[3];
        double p5 = widths[4];

        // Convert heights to a scale of 0-200.
        double[] heights = envelope.getHeights();
        double v1 = 100 - (heights[0] / 2.0);
        double v2 = 100 - (heights[1] / 2.0);
        double v3 = 100 - (heights[2] / 2.0);
        double v4 = 100 - (heights[3] / 2.0);
        double v5 = 100 - (heights[4] / 2.0);

        // Do not scale y axis for envelopes.
        double[] xValues = new double[] {
                scaler.scalePos(startPos),
                scaler.scalePos(startPos + p1),
                scaler.scalePos(startPos + p1 + p2),
                scaler.scalePos(startPos + p1 + p2 + p5),
                scaler.scalePos(endPos - p4 - p3),
                scaler.scalePos(endPos - p4),
                scaler.scalePos(endPos)};
        double[] yValues = new double[] {100, v1, v2, v5, v3, v4, 100};

        return new Envelope(xValues, yValues, callback, 100, scaler);
    }

    public Envelope createEnvelopeEditor(
            double editorWidth,
            double editorHeight,
            EnvelopeData envelope,
            Scaler editorScaler,
            boolean scaleToFit) {
        double[] widths = envelope.getWidths();
        double p1 = widths[0];
        double p2 = widths[1];
        double p3 = widths[2];
        double p4 = widths[3];
        double p5 = widths[4];

        double maxAllowedWidth = editorWidth - editorScaler.scaleX(p1 + p4);
        double totalWidth = editorScaler.scaleX(p2 + p3 + p5);
        if (maxAllowedWidth > 0 && totalWidth > maxAllowedWidth) {
            double scaleFactor = maxAllowedWidth / totalWidth;
            if (scaleToFit) {
                editorScaler = editorScaler.derive(scaleFactor, 1);
            } else {
                p2 *= scaleFactor;
                p3 *= scaleFactor;
                p5 *= scaleFactor;
            }
        }

        // Convert heights to a scale of 0-200.
        double[] heights = envelope.getHeights();
        double multiplier = editorHeight / 200;
        double v1 = editorHeight - (heights[0] * multiplier);
        double v2 = editorHeight - (heights[1] * multiplier);
        double v3 = editorHeight - (heights[2] * multiplier);
        double v4 = editorHeight - (heights[3] * multiplier);
        double v5 = editorHeight - (heights[4] * multiplier);

        // Do not scale y axis for envelopes.
        double[] xValues = new double[] {
                0,
                editorScaler.scaleX(p1),
                editorScaler.scaleX(p1 + p2),
                editorScaler.scaleX(p1 + p2 + p5),
                editorWidth - editorScaler.scaleX(p4 + p3),
                editorWidth - editorScaler.scaleX(p4),
                editorWidth};
        double[] yValues = new double[] {editorHeight, v1, v2, v5, v3, v4, editorHeight};

        return new Envelope(
                xValues, yValues, ((oldData, newData) -> {}), editorHeight, editorScaler);
    }
}
