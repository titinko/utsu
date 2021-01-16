package com.utsusynth.utsu.view.song.note.pitch;

import java.util.ArrayList;
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
        int prevRowNum = PitchUtils.pitchToRowNum(prevPitch);
        Portamento portamento = createPortamento(note, prevRowNum, pitchbend, callback);
        Vibrato vibrato = createVibrato(note, pitchbend, callback, vibratoEditor);
        return new Pitchbend(portamento, vibrato, showPitchbend);
    }

    public Portamento createPortamento(
            Note note,
            int prevRowNum,
            PitchbendData pitchbend,
            PitchbendCallback callback) {
        double finalY = (note.getRow() + .5) * Quantizer.ROW_HEIGHT;

        double curX = note.getAbsPositionMs() + pitchbend.getPBS().get(0);
        double curY = (prevRowNum + .5) * Quantizer.ROW_HEIGHT;

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
            double editorheight,
            Note note,
            int prevRowNum,
            PitchbendData pitchbend,
            Scaler editorScaler,
            boolean scaleToFit) {
        double finalY = (note.getRow() + .5) * Quantizer.ROW_HEIGHT;

        double curX = note.getAbsPositionMs() + pitchbend.getPBS().get(0);
        double curY = (prevRowNum + .5) * Quantizer.ROW_HEIGHT;

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
                            editorScaler.scalePos(tempX).get(),
                            editorScaler.scaleY(tempY).get(),
                            editorScaler.scalePos(curX).get(),
                            editorScaler.scaleY(curY).get(),
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
