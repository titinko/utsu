package com.utsusynth.utsu.common.data;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.utsusynth.utsu.common.quantize.QuantizedPitchbend;
import com.utsusynth.utsu.common.quantize.QuantizedPortamento;
import com.utsusynth.utsu.common.quantize.QuantizedVibrato;
import com.utsusynth.utsu.common.quantize.Quantizer;

public class PitchbendData {
    private final ImmutableList<Double> pbs; // Pitch bend start.
    private final ImmutableList<Double> pbw; // Pitch bend widths.
    private final ImmutableList<Double> pby; // Pitch bend shifts.
    private final ImmutableList<String> pbm; // Pitch bend curves.
    private final int[] vibrato;

    public static PitchbendData fromQuantized(QuantizedPitchbend qPitchbend) {
        QuantizedPortamento qPortamento = qPitchbend.getPortamento();
        double quantSize = Quantizer.DEFAULT_NOTE_DURATION / QuantizedPortamento.QUANTIZATION;
        ImmutableList<Double> pbs = ImmutableList.of(qPortamento.getStart() * quantSize, 0.0);
        ImmutableList.Builder<Double> pbwBuilder = ImmutableList.builder();
        for (int i = 0; i < qPortamento.getNumWidths(); i++) {
            pbwBuilder.add(qPortamento.getWidth(i) * quantSize);
        }
        int[] vibrato =
                qPitchbend.getVibrato().isPresent() ? qPitchbend.getVibrato().get().toUstVibrato()
                        : new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        return new PitchbendData(
                pbs,
                pbwBuilder.build(),
                qPortamento.getShifts(),
                qPortamento.getCurves(),
                vibrato);
    }

    public PitchbendData(
            ImmutableList<Double> pbs,
            ImmutableList<Double> pbw,
            ImmutableList<Double> pby,
            ImmutableList<String> pbm,
            int[] vibrato) {
        this.pbs = pbs;
        this.pbw = pbw;
        this.pby = pby;
        this.pbm = pbm;
        this.vibrato = vibrato;
    }

    public ImmutableList<Double> getPBS() {
        return pbs;
    }

    public ImmutableList<Double> getPBW() {
        return pbw;
    }

    public ImmutableList<Double> getPBY() {
        return pby;
    }

    public ImmutableList<String> getPBM() {
        return pbm;
    }

    public int[] getVibrato() {
        return vibrato;
    }

    public int getVibrato(int index) {
        if (index < 0 || index >= vibrato.length) {
            return 0;
        }
        return vibrato[index];
    }

    public QuantizedPitchbend quantize(String prevPitch) {
        int quantSize = Quantizer.DEFAULT_NOTE_DURATION / QuantizedPortamento.QUANTIZATION;
        int start = (int) Math.ceil(pbs.get(0) / quantSize);
        ImmutableList.Builder<Integer> widths = ImmutableList.builder();
        for (double width : pbw) {
            widths.add((int) Math.floor(width / quantSize));
        }
        QuantizedPortamento qPortamento =
                new QuantizedPortamento(prevPitch, start, widths.build(), pby, pbm);
        QuantizedVibrato qVibrato = new QuantizedVibrato(vibrato);
        return new QuantizedPitchbend(qPortamento, Optional.of(qVibrato));
    }

    public QuantizedPortamento quantizePortamento(String prevPitch) {
        int quantSize = Quantizer.DEFAULT_NOTE_DURATION / QuantizedPortamento.QUANTIZATION;
        int start = (int) Math.ceil(pbs.get(0) / quantSize);
        ImmutableList.Builder<Integer> widths = ImmutableList.builder();
        for (double width : pbw) {
            widths.add((int) Math.floor(width / quantSize));
        }
        return new QuantizedPortamento(prevPitch, start, widths.build(), pby, pbm);
    }
}
