package com.utsusynth.utsu.view.song.note.pitch;

import java.util.ArrayList;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.utsusynth.utsu.common.data.PitchbendData;
import com.utsusynth.utsu.common.i18n.Localizer;
import com.utsusynth.utsu.common.quantize.Quantizer;
import com.utsusynth.utsu.common.quantize.Scaler;
import com.utsusynth.utsu.common.utils.PitchUtils;
import com.utsusynth.utsu.view.song.note.Note;
import com.utsusynth.utsu.view.song.note.pitch.portamento.Curve;
import com.utsusynth.utsu.view.song.note.pitch.portamento.CurveFactory;
import com.utsusynth.utsu.view.song.note.pitch.portamento.Portamento;
import javafx.beans.property.BooleanProperty;

public class PitchbendFactory {
    private final CurveFactory curveFactory;
    private final Localizer localizer;
    private final Scaler scaler;

    @Inject
    public PitchbendFactory(CurveFactory curveFactory, Localizer localizer, Scaler scaler) {
        this.curveFactory = curveFactory;
        this.localizer = localizer;
        this.scaler = scaler;
    }

    public Pitchbend createPitchbend(
            Note note,
            String prevPitch,
            PitchbendData pitchbend,
            PitchbendCallback callback,
            BooleanProperty vibratoEditor,
            BooleanProperty showPitchbend) {
        Portamento portamento = createPortamento(note, prevPitch, pitchbend, callback);
        Vibrato vibrato = createVibrato(note, pitchbend, callback, vibratoEditor);
        return new Pitchbend(portamento, vibrato, showPitchbend);
    }

    private Portamento createPortamento(
            Note note,
            String prevPitch,
            PitchbendData pitchbend,
            PitchbendCallback callback) {
        double finalY = (note.getRow() + .5) * Quantizer.ROW_HEIGHT;

        double curX = note.getAbsPositionMs() + pitchbend.getPBS().get(0);
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
                            scaler.scalePos(tempX).get(),
                            scaler.scaleY(tempY).get(),
                            scaler.scalePos(curX).get(),
                            scaler.scaleY(curY).get(),
                            type));
        }
        return new Portamento(
                note.getAbsPositionMs(), pitchCurves, callback, curveFactory, localizer, scaler);
    }

    public Portamento createPortamentoEditor(
            double editorWidth,
            double editorHeight,
            Note note,
            int prevRowNum,
            PitchbendData pitchbend,
            Scaler editorScaler,
            boolean scaleToFit) {
        // X scaling is editorWidth is a real width.
        if (editorWidth > 0) {
            double minMs =
                    note.getAbsPositionMs() + (Quantizer.COL_WIDTH * 4) + pitchbend.getPBS().get(0);
            double minX = editorScaler.scaleX(minMs).get();
            double maxX = editorScaler.scaleX(
                    minMs + pitchbend.getPBW().stream().reduce(Double::sum).get()).get();
            double halfWidth = editorWidth / 2.0;
            double excessX = 0;
            if (minX < 0) {
                excessX = Math.max(excessX, Math.abs(minX));
            }
            if (maxX > editorWidth) {
                excessX = Math.max(excessX, Math.abs(maxX - editorWidth));
            }
            if (excessX > 0) {
                double xMultiplier = halfWidth / (halfWidth + excessX);
                if (scaleToFit) {
                    editorScaler = editorScaler.derive(xMultiplier, 1);
                } else {
                    ImmutableList<Double> newPbs =
                            ImmutableList.of(pitchbend.getPBS().get(0) * xMultiplier, 0.0);
                    ImmutableList<Double> newPbw = ImmutableList.copyOf(
                            pitchbend.getPBW().stream()
                                    .map(width -> width * xMultiplier)
                                    .collect(Collectors.toList()));
                    pitchbend = new PitchbendData(
                            newPbs, newPbw, pitchbend.getPBY(), pitchbend.getPBM());
                }
            }
        }

        // Y scaling if editorHeight is a real value and there are Y values to scale.
        if (!pitchbend.getPBY().isEmpty() && editorHeight > 0) {
            double excessY = 0;
            double curY = editorScaler.scaleY((prevRowNum + .5) * Quantizer.ROW_HEIGHT).get();
            for (double height : pitchbend.getPBY()) {
                double newY = curY + editorScaler.scaleY(height).get();
                if (curY < 0) {

                }

            }
        }

        // Calculate curves with current pitchbend/scaler.
        double noteMs = note.getAbsPositionMs() + (Quantizer.COL_WIDTH * 4);
        double curMs = noteMs + pitchbend.getPBS().get(0);
        double curCents = (prevRowNum + .5) * Quantizer.ROW_HEIGHT;
        double finalCents = (note.getRow() + .5) * Quantizer.ROW_HEIGHT;

        ArrayList<Curve> pitchCurves = new ArrayList<>();
        ImmutableList<Double> widths = pitchbend.getPBW();
        for (int i = 0; i < widths.size(); i++) {
            double tempMs = curMs;
            curMs += widths.get(i);
            double tempCents = curCents;
            if (i == widths.size() - 1) {
                curCents = finalCents;
            } else {
                if (pitchbend.getPBY().size() > i) {
                    // Leave curCents as-is if PBY has no value for this width.
                    curCents = finalCents - (pitchbend.getPBY().get(i) / 10) * Quantizer.ROW_HEIGHT;
                }
            }
            String type = pitchbend.getPBM().size() > i ? pitchbend.getPBM().get(i) : "";
            pitchCurves.add(
                    curveFactory.createCurve(
                            editorScaler.scaleX(tempMs).get(),
                            editorScaler.scaleY(tempCents).get(),
                            editorScaler.scaleX(curMs).get(),
                            editorScaler.scaleY(curCents).get(),
                            type));
        }

        return new Portamento(
                note.getAbsPositionMs(),
                pitchCurves,
                null,
                curveFactory,
                localizer,
                editorScaler);
    }

    private Vibrato createVibrato(
            Note note,
            PitchbendData pitchbend,
            PitchbendCallback callback,
            BooleanProperty vibratoEditor) {
        return new Vibrato(
                note.getAbsPositionMs(),
                note.getAbsPositionMs() + note.getDurationMs(),
                scaler.scaleY((note.getRow() + .5) * Quantizer.ROW_HEIGHT).get(),
                callback,
                localizer,
                scaler,
                pitchbend.getVibrato(),
                vibratoEditor);
    }
}
