package com.utsusynth.utsu.view.song.note.portamento;

import java.util.ArrayList;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.utsusynth.utsu.common.PitchUtils;
import com.utsusynth.utsu.common.data.PitchbendData;
import com.utsusynth.utsu.common.quantize.Quantizer;
import com.utsusynth.utsu.common.quantize.Scaler;
import com.utsusynth.utsu.view.song.note.Note;

public class PortamentoFactory {
    private final CurveFactory curveFactory;
    private final Scaler scaler;

    @Inject
    public PortamentoFactory(CurveFactory curveFactory, Scaler scaler) {
        this.curveFactory = curveFactory;
        this.scaler = scaler;
    }

    public Portamento createPortamento(
            Note note,
            String prevPitch,
            PitchbendData pitchbend,
            PortamentoCallback callback) {
        double finalY = (note.getRow() + .5) * Quantizer.ROW_HEIGHT;

        double curX = note.getAbsPosition() + pitchbend.getPBS().get(0);
        double curY = (PitchUtils.pitchToRowNum(prevPitch) + .5) * Quantizer.ROW_HEIGHT;

        ArrayList<Curve> pitchCurves = new ArrayList<>();
        ImmutableList<Double> widths = pitchbend.getPBW();
        for (int i = 0; i < widths.size(); i++) {
            double tempX = curX;
            curX += widths.get(i);
            double tempY = curY;
            if (i == widths.size() - 1) {
                curY = finalY;
            } else {
                if (pitchbend.getPBY().size() > i) {
                    // Leave curY as-is if PBY has no value for this width.
                    curY = finalY - (pitchbend.getPBY().get(i) / 10) * Quantizer.ROW_HEIGHT;
                }
            }
            String type = pitchbend.getPBM().size() > i ? pitchbend.getPBM().get(i) : "";
            pitchCurves.add(
                    curveFactory.createCurve(
                            scaler.scaleX(tempX),
                            scaler.scaleY(tempY),
                            scaler.scaleX(curX),
                            scaler.scaleY(curY),
                            type));
        }
        return new Portamento(pitchCurves, callback, curveFactory, scaler);
    }
}
