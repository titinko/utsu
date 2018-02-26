package com.utsusynth.utsu.view.song.note.pitch;

import com.google.common.base.Optional;
import javafx.scene.Group;

/**
 * Visual representation of the vibrato of a single note.
 */
public class Vibrato {
    private final PitchbendCallback callback;
    private int[] vibrato;

    Vibrato(PitchbendCallback callback, int[] vibrato) {
        this.callback = callback;
        this.vibrato = vibrato;
    }

    Group getElement() {
        return new Group();
    }

    public Optional<int[]> getVibrato() {
        for (int value : vibrato) {
            if (value != 0) {
                return Optional.of(vibrato);
            }
        }
        // Return absent if all vibrato values are 0.
        return Optional.absent();
    }

    public void addDefaultVibrato() {
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
        callback.modifySongPitchbend();
    }

    public void clearVibrato() {
        vibrato = new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        callback.modifySongPitchbend();
    }
}
