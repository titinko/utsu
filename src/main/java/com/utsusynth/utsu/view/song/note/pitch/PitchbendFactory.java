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
        double maxX = Double.POSITIVE_INFINITY;
        double maxY = scaler.scaleY(Quantizer.ROW_HEIGHT * 12 * 7).get();
        return new Portamento(
                note.getAbsPositionMs(),
                maxX,
                maxY,
                pitchCurves,
                callback,
                curveFactory,
                localizer,
                scaler);
    }

    public Portamento createPortamentoEditor(
            double editorWidth,
            double editorHeight,
            Note note,
            int prevRowNum,
            PitchbendData pitchbend,
            Scaler editorScaler,
            boolean scaleToFit) {
        int buffer = 5;
        double halfRowHeight = editorScaler.scaleY(Quantizer.ROW_HEIGHT * .5).get();

        // X-scaling if editorWidth is a real width.
        if (editorWidth > 0) {
            double minMs =
                    note.getAbsPositionMs() + (Quantizer.COL_WIDTH * 4) + pitchbend.getPBS().get(0);
            double minX = editorScaler.scaleX(minMs).get();
            double maxX = editorScaler.scaleX(
                    minMs + pitchbend.getPBW().stream().reduce(Double::sum).get()).get();
            double excessX = 0;
            if (minX < buffer) {
                excessX = Math.max(excessX, Math.abs(minX) + buffer);
            }
            if (maxX > editorWidth - buffer) {
                excessX = Math.max(excessX, Math.abs(maxX - editorWidth) + buffer);
            }
            if (excessX > 0) {
                double halfWidth = editorWidth / 2.0;
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

        // Y-scaling if editorHeight is a real value and there are Y values to scale.
        if (!pitchbend.getPBY().isEmpty() && editorHeight > 0) {
            double excessY = 0;
            double finalY = editorScaler.scaleY((note.getRow() + .5) * Quantizer.ROW_HEIGHT).get();
            double topHeight = editorHeight - finalY;
            double bottomHeight = finalY;
            ImmutableList.Builder<Double> newPby = ImmutableList.builder();
            for (double pby : pitchbend.getPBY()) {
                double curY =
                        finalY - editorScaler.scaleY((pby / 10) * Quantizer.ROW_HEIGHT).get();
                if (curY < buffer) {
                    excessY = Math.max(excessY, Math.abs(curY) + buffer);
                    double multiplier = bottomHeight / (bottomHeight + Math.abs(curY) + buffer);
                    newPby.add(pby * multiplier);
                } else if (curY > editorHeight - buffer) {
                    excessY = Math.max(excessY, Math.abs(curY - editorHeight) + buffer);
                    double multiplier =
                            topHeight / (topHeight + Math.abs(curY - editorHeight) + buffer);
                    newPby.add(pby * multiplier);
                } else {
                    newPby.add(pby);
                }
            }
            if (excessY > 0) {
                if (scaleToFit) {
                    double halfHeight = editorHeight / 2;
                    double yMultiplier = halfHeight / (halfHeight + excessY);
                    editorScaler = editorScaler.derive(1, yMultiplier);
                } else {
                    pitchbend = new PitchbendData(
                            pitchbend.getPBS(),
                            pitchbend.getPBW(),
                            newPby.build(),
                            pitchbend.getPBM());
                }
            }
        }

        // Calculate curves with current pitchbend/scaler.
        double noteX;
        double curY;
        double finalY;
        double baseY;
        if (scaleToFit) {
            // Center everything on screen if scaling to fit.
            noteX = editorWidth / 2.0;
            curY = editorHeight / 2.0 + halfRowHeight;
            finalY = editorHeight / 2.0 + halfRowHeight;
            baseY = editorHeight / 2.0;
        } else {
            noteX = editorScaler.scaleX(note.getAbsPositionMs() + (Quantizer.COL_WIDTH * 4)).get();
            curY = editorScaler.scaleY((prevRowNum + .5) * Quantizer.ROW_HEIGHT).get();
            finalY = editorScaler.scaleY((note.getRow() + .5) * Quantizer.ROW_HEIGHT).get();
            baseY = finalY;
        }
        double curX = noteX + editorScaler.scaleX(pitchbend.getPBS().get(0)).get();

        ArrayList<Curve> pitchCurves = new ArrayList<>();
        ImmutableList<Double> widths = pitchbend.getPBW();
        for (int i = 0; i < widths.size(); i++) {
            double tempX = curX;
            curX += editorScaler.scaleX(widths.get(i)).get();
            double tempY = curY;
            if (i == widths.size() - 1) {
                curY = finalY;
            } else {
                if (pitchbend.getPBY().size() > i) {
                    // Leave curCents as-is if PBY has no value for this width.
                    double yDiff = editorScaler.scaleY(
                            (pitchbend.getPBY().get(i) / 10) * Quantizer.ROW_HEIGHT).get();
                    curY = baseY - yDiff;
                }
            }
            String type = pitchbend.getPBM().size() > i ? pitchbend.getPBM().get(i) : "";
            pitchCurves.add(curveFactory.createCurve(tempX, tempY, curX, curY, type));
        }

        return new Portamento(
                note.getAbsPositionMs(),
                editorWidth,
                editorHeight,
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
