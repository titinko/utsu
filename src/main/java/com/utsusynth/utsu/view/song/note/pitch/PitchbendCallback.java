package com.utsusynth.utsu.view.song.note.pitch;

import com.utsusynth.utsu.common.data.PitchbendData;
import com.utsusynth.utsu.view.song.DragHandler;

public interface PitchbendCallback {
    void modifySongPitchbend(PitchbendData oldData, PitchbendData newData);

    void modifySongVibrato(int[] oldVibrato, int[] newVibrato);

    void startDrag(DragHandler dragHandler);

    // Add pitchbend to new columns if necessary.
    void readjust();
}
