package com.utsusynth.utsu.view.song.note.pitch;

import com.google.common.base.Optional;
import com.utsusynth.utsu.common.data.PitchbendData;
import com.utsusynth.utsu.view.song.note.pitch.portamento.Portamento;
import javafx.scene.Group;

public class Pitchbend {
    private final Portamento portamento;
    private final Vibrato vibrato;
    private final Group group;

    Pitchbend(Portamento portamento, Vibrato vibrato) {
        this.portamento = portamento;
        this.vibrato = vibrato;
        this.group = new Group(portamento.getElement(), vibrato.getElement());
    }

    public Group getElement() {
        return group;
    }

    public boolean hasVibrato() {
        return vibrato.getVibrato().isPresent();
    }

    public void setHasVibrato(boolean hasVibrato) {
        if (hasVibrato) {
            vibrato.addDefaultVibrato();
        } else {
            vibrato.clearVibrato();
        }
    }

    public Optional<int[]> getVibrato() {
        return vibrato.getVibrato();
    }

    public PitchbendData getData() {
        return portamento.getData().withVibrato(vibrato.getVibrato());
    }
}
