package com.utsusynth.utsu.view.song.note.pitch;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import com.utsusynth.utsu.common.data.PitchbendData;
import com.utsusynth.utsu.view.song.track.TrackItem;
import com.utsusynth.utsu.view.song.note.pitch.portamento.Portamento;
import javafx.beans.property.BooleanProperty;
import javafx.scene.Group;

public class Pitchbend implements TrackItem {
    private final Portamento portamento;
    private final Vibrato vibrato;
    private final BooleanProperty showPitchbend;
    private final Set<Integer> drawnColumns;

    Pitchbend(Portamento portamento, Vibrato vibrato, BooleanProperty showPitchbend) {
        this.portamento = portamento;
        this.vibrato = vibrato;
        this.showPitchbend = showPitchbend;
        drawnColumns = new HashSet<>();
    }

    @Override
    public TrackItemType getType() {
        return TrackItemType.PITCHBEND;
    }

    @Override
    public double getStartX() {
        if (!hasVibrato()) {
            return portamento.getStartX();
        }
        return Math.min(portamento.getStartX(), vibrato.getStartX());
    }

    @Override
    public double getWidth() {
        if (!hasVibrato()) {
            return portamento.getWidth();
        }
        return Math.max(
                portamento.getStartX() + portamento.getWidth(),
                vibrato.getStartX() + vibrato.getWidth())
                - getStartX();
    }

    @Override
    public Group redraw() {
        return redraw(-1, 0);
    }

    @Override
    public Group redraw(int colNum, double offsetX) {
        drawnColumns.add(colNum);
        Group group =
                new Group(portamento.redraw(colNum, offsetX), vibrato.redraw(colNum, offsetX));
        group.visibleProperty().bind(showPitchbend);
        return group;
    }

    @Override
    public Set<Integer> getColumns() {
        return drawnColumns;
    }

    @Override
    public void clearColumns() {
        drawnColumns.clear();
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
