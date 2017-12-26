package com.utsusynth.utsu.view.note;

import com.google.common.base.Optional;

public class TrackVibrato {
    private Optional<int[]> vibrato;

    TrackVibrato(Optional<int[]> vibrato) {
        this.vibrato = vibrato;
    }

    Optional<int[]> getVibrato() {
        return vibrato;
    }

    int[] addDefaultVibrato() {
        int[] ustVibrato = new int[10];
        ustVibrato[0] = 70;
        ustVibrato[1] = 185;
        ustVibrato[2] = 40;
        ustVibrato[3] = 20;
        ustVibrato[4] = 20;
        ustVibrato[5] = 0;
        ustVibrato[6] = 0;
        ustVibrato[7] = 100;
        ustVibrato[8] = 0;
        ustVibrato[9] = 0;
        vibrato = Optional.of(ustVibrato);
        return vibrato.get();
    }

    void clearVibrato() {
        vibrato = Optional.absent();
    }
}
