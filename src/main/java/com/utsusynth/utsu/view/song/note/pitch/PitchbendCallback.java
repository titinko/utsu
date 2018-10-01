package com.utsusynth.utsu.view.song.note.pitch;

import com.utsusynth.utsu.common.data.PitchbendData;

public interface PitchbendCallback {
    void modifySongPitchbend(PitchbendData oldData, PitchbendData newData);

    void modifySongVibrato(int[] oldVibrato, int[] newVibrato);
}
