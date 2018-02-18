package com.utsusynth.utsu.view.song.note;

import com.google.common.base.Optional;

/**
 * Visual representation of the vibrato of a single note.
 */
public class Vibrato {
    private int[] vibrato;

    /**
     * Constructor to create an empty vibrato (all values are zero).
     */
    Vibrato() {
        clearVibrato();
    }

    Vibrato(int[] vibrato) {
        this.vibrato = vibrato;
    }

    Optional<int[]> getVibrato() {
        for (int value : vibrato) {
            if (value != 0) {
                return Optional.of(vibrato);
            }
        }
        // Return absent if all vibrato values are 0.
        return Optional.absent();
    }

    int[] addDefaultVibrato() {
        vibrato = new int[10];
        vibrato[0] = 70;
        vibrato[1] = 185;
        vibrato[2] = 40;
        vibrato[3] = 20;
        vibrato[4] = 20;
        vibrato[5] = 0;
        vibrato[6] = 0;
        vibrato[7] = 100;
        vibrato[8] = 0;
        vibrato[9] = 0;
        return vibrato;
    }

    void clearVibrato() {
        vibrato = new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    }
}
