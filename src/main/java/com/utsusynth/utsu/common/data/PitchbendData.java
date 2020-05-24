package com.utsusynth.utsu.common.data;

import java.util.Optional;
import com.google.common.collect.ImmutableList;

public class PitchbendData {
    private final ImmutableList<Double> pbs; // Pitch bend start, in milliseconds.
    private final ImmutableList<Double> pbw; // Pitch bend widths, in milliseconds.
    private final ImmutableList<Double> pby; // Pitch bend shifts, in 1/10 of a semitone.
    private final ImmutableList<String> pbm; // Pitch bend curves: "s" or "r" or "j" or ""
    private final int[] vibrato;

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

    public PitchbendData(
            ImmutableList<Double> pbs,
            ImmutableList<Double> pbw,
            ImmutableList<Double> pby,
            ImmutableList<String> pbm) {
        this(pbs, pbw, pby, pbm, new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0});
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

    public PitchbendData withVibrato(Optional<int[]> newVibrato) {
        if (newVibrato.isPresent()) {
            return new PitchbendData(this.pbs, this.pbw, this.pby, this.pbm, newVibrato.get());
        } else {
            return new PitchbendData(this.pbs, this.pbw, this.pby, this.pbm);
        }
    }

    @Override
    public String toString() {
        // String representation of a PitchbendData object.
        String result = "PBS: ";
        for (double pbsValue : pbs) {
            result += pbsValue + ", ";
        }
        result += "\nPBW: ";
        for (double pbwValue : pbw) {
            result += pbwValue + ", ";
        }
        result += "\nPBY: ";
        for (double pbyValue : pby) {
            result += pbyValue + ", ";
        }
        result += "\nPBM: ";
        for (String pbmValue : pbm) {
            result += pbmValue + ", ";
        }
        result += "\nVibrato: ";
        for (int vibratoValue : vibrato) {
            result += vibratoValue + ", ";
        }
        return result;
    }
}
