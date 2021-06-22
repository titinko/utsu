package com.utsusynth.utsu.view.song.note.pitch;

import java.util.HashSet;
import java.util.Optional;

import com.google.common.collect.ImmutableSet;
import com.utsusynth.utsu.common.data.PitchbendData;
import com.utsusynth.utsu.view.song.track.TrackItem;
import com.utsusynth.utsu.view.song.note.pitch.portamento.Portamento;
import javafx.beans.property.BooleanProperty;
import javafx.scene.Group;

public class Pitchbend implements TrackItem {
    private final Portamento portamento;
    private final Vibrato vibrato;
    private final BooleanProperty showPitchbend;
    private final HashSet<Integer> drawnColumns;

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
        return redraw(0);
    }

    @Override
    public Group redraw(double offsetX) {
        Group group =
                new Group(portamento.redraw(offsetX), vibrato.redraw(offsetX));
        group.visibleProperty().bind(showPitchbend);
        return group;
    }

    @Override
    public ImmutableSet<Integer> getColumns() {
        return ImmutableSet.copyOf(drawnColumns);
    }

    @Override
    public void addColumn(int colNum) {
        drawnColumns.add(colNum);
    }

    @Override
    public void removeColumn(int colNum) {
        drawnColumns.remove(colNum);
    }

    @Override
    public void removeAllColumns() {
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
