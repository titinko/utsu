package com.utsusynth.utsu.model.song.pitch;

import java.util.HashMap;
import com.google.common.base.Optional;
import com.utsusynth.utsu.model.song.pitch.portamento.Portamento;

class Pitchbend implements PitchMutation {
    private HashMap<Integer, Portamento> portamento;
    private Optional<Vibrato> vibrato;

    static Pitchbend makePitchbend(int deltaOfNote, Portamento portamento) {
        Pitchbend pitchbend = new Pitchbend();
        pitchbend.portamento.put(deltaOfNote, portamento);
        return pitchbend;
    }

    static Pitchbend makePitchbend(Vibrato vibrato) {
        Pitchbend pitchbend = new Pitchbend();
        pitchbend.vibrato = Optional.of(vibrato);
        return pitchbend;
    }

    private Pitchbend() {
        portamento = new HashMap<>();
        vibrato = Optional.absent();
    }

    void addPortamento(int noteStartMs, Portamento portamento) {
        if (this.portamento.containsKey(noteStartMs)) {
            // TODO: Handle this.
            System.out.println("Error: tried to add portamento twice.");
        } else {
            this.portamento.put(noteStartMs, portamento);
        }
    }

    void removePortamento(int noteStartMs) {
        this.portamento.remove(noteStartMs);
    }

    /** Always fetches the portamento of the note with the highest delta. */
    Optional<Portamento> getPortamento() {
        int maxKey = -1;
        for (int key : portamento.keySet()) {
            if (key > maxKey) {
                maxKey = key;
            }
        }
        return Optional.fromNullable(portamento.get(maxKey));
    }

    void addVibrato(Vibrato vibrato) {
        if (this.vibrato.isPresent()) {
            // TODO: Handle this.
            System.out.println("Error: tried to add overlapping vibrato.");
        } else {
            this.vibrato = Optional.of(vibrato);
        }
    }

    void removeVibrato() {
        this.vibrato = Optional.absent();
    }

    boolean isEmpty() {
        return this.portamento.isEmpty() && !this.vibrato.isPresent();
    }

    @Override
    public double apply(int positionMs) {
        double portamentoVal = 0;
        Optional<Portamento> lastPortamento = getPortamento();
        if (lastPortamento.isPresent()) {
            // Portamento pitch is absolute.
            portamentoVal = lastPortamento.get().apply(positionMs);
        }
        double vibratoVal = 0;
        if (vibrato.isPresent()) {
            // Vibrato pitch is centered on zero, meant to modify portamento pitch.
            vibratoVal = vibrato.get().apply(positionMs);
        }
        return portamentoVal + vibratoVal;
    }
}
