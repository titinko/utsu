package com.utsusynth.utsu.view.song.note.pitch;

import java.util.Optional;
import com.utsusynth.utsu.common.data.PitchbendData;
import com.utsusynth.utsu.view.song.note.pitch.portamento.Portamento;
import javafx.beans.property.BooleanProperty;
import javafx.scene.Group;

public class Pitchbend {
    private final Portamento portamento;
    private final Vibrato vibrato;
    private final Group group;

    Pitchbend(Portamento portamento, Vibrato vibrato, BooleanProperty showPitchbend) {
        this.portamento = portamento;
        this.vibrato = vibrato;
        group = new Group(portamento.getElement(), vibrato.getElement());
        group.visibleProperty().bind(showPitchbend);
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
