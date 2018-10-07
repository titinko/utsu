package com.utsusynth.utsu.view.song.note.envelope;

import com.google.inject.Inject;
import com.utsusynth.utsu.common.data.EnvelopeData;
import com.utsusynth.utsu.common.quantize.Scaler;
import com.utsusynth.utsu.view.song.note.Note;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;

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

        double[] heights = envelope.getHeights();
        double v1 = 100 - (heights[0] / 2.0);
        double v2 = 100 - (heights[1] / 2.0);
        double v3 = 100 - (heights[2] / 2.0);
        double v4 = 100 - (heights[3] / 2.0);
        double v5 = 100 - (heights[4] / 2.0);

        // Do not scale y axis for envelopes.
        return new Envelope(
                new MoveTo(scaler.scalePos(startPos), 100),
                new LineTo(scaler.scalePos(startPos + p1), v1),
                new LineTo(scaler.scalePos(startPos + p1 + p2), v2),
                new LineTo(scaler.scalePos(startPos + p1 + p2 + p5), v5),
                new LineTo(scaler.scalePos(endPos - p4 - p3), v3),
                new LineTo(scaler.scalePos(endPos - p4), v4),
                new LineTo(scaler.scalePos(endPos), 100),
                callback,
                scaler);
    }
}
