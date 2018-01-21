package com.utsusynth.utsu.view.song;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import com.google.inject.Inject;
import com.utsusynth.utsu.common.data.EnvelopeData;
import com.utsusynth.utsu.common.data.PitchbendData;
import com.utsusynth.utsu.common.exception.NoteAlreadyExistsException;
import com.utsusynth.utsu.view.song.note.Note;
import com.utsusynth.utsu.view.song.note.envelope.Envelope;
import com.utsusynth.utsu.view.song.note.envelope.EnvelopeCallback;
import com.utsusynth.utsu.view.song.note.envelope.EnvelopeFactory;
import com.utsusynth.utsu.view.song.note.portamento.Portamento;
import com.utsusynth.utsu.view.song.note.portamento.PortamentoCallback;
import com.utsusynth.utsu.view.song.note.portamento.PortamentoFactory;
import javafx.scene.Group;

public class NoteMap {
    private final EnvelopeFactory envelopeFactory;
    private final PortamentoFactory portamentoFactory;

    // Maps absolute position to track note's data.
    private Map<Integer, Note> noteMap;
    private Map<Integer, Envelope> envelopeMap;
    private Map<Integer, Portamento> portamentoMap;
    private Group notes;
    private Group envelopes;
    private Group pitchbends;

    @Inject
    public NoteMap(EnvelopeFactory envFactory, PortamentoFactory portFactory) {
        this.envelopeFactory = envFactory;
        this.portamentoFactory = portFactory;
        clear();
    }

    Group getNotesElement() {
        return notes;
    }

    Group getEnvelopesElement() {
        return envelopes;
    }

    Group getPitchbendsElement() {
        return pitchbends;
    }

    void clear() {
        noteMap = new HashMap<>();
        envelopeMap = new HashMap<>();
        portamentoMap = new HashMap<>();
        notes = new Group();
        envelopes = new Group();
        pitchbends = new Group();
    }

    boolean hasNote(int position) {
        return noteMap.containsKey(position);
    }

    Note getNote(int position) {
        return noteMap.get(position);
    }

    Collection<Note> getAllNotes() {
        return noteMap.values();
    }

    void putNote(int position, Note note) throws NoteAlreadyExistsException {
        if (noteMap.containsKey(position)) {
            throw new NoteAlreadyExistsException();
        }
        noteMap.put(position, note);
    }

    void addNoteElement(Note note) {
        notes.getChildren().add(note.getElement());
    }

    void removeNoteElement(Note note) {
        notes.getChildren().remove(note.getElement());
    }

    void removeFullNote(int position) {
        if (noteMap.containsKey(position)) {
            noteMap.remove(position);
        } else {
            // TODO: Handle this better.
            System.out.println("Could not find note in map of track notes :(");
        }
        if (envelopeMap.containsKey(position)) {
            envelopes.getChildren().remove(envelopeMap.get(position).getElement());
            envelopeMap.remove(position);
        }
        if (portamentoMap.containsKey(position)) {
            pitchbends.getChildren().remove(portamentoMap.get(position).getElement());
            portamentoMap.remove(position);
        }
    }

    void putEnvelope(int position, EnvelopeData envelopeData, EnvelopeCallback callback) {
        // Track note must exist before envelope is added.
        if (noteMap.containsKey(position)) {
            Envelope envelope =
                    envelopeFactory.createEnvelope(noteMap.get(position), envelopeData, callback);
            // Overrides are expected here.
            if (envelopeMap.containsKey(position)) {
                envelopes.getChildren().remove(envelopeMap.get(position).getElement());
            }
            envelopeMap.put(position, envelope);
            envelopes.getChildren().add(envelope.getElement());
        }
    }

    boolean hasEnvelope(int position) {
        return envelopeMap.containsKey(position);
    }

    Envelope getEnvelope(int position) {
        return envelopeMap.get(position);
    }

    void putPortamento(
            int position,
            String prevPitch,
            PitchbendData pitchbend,
            PortamentoCallback callback) {
        if (noteMap.containsKey(position)) {
            Portamento portamento = portamentoFactory
                    .createPortamento(noteMap.get(position), prevPitch, pitchbend, callback);
            // Overrides are expected here.
            if (portamentoMap.containsKey(position)) {
                pitchbends.getChildren().remove(portamentoMap.get(position).getElement());
            }
            portamentoMap.put(position, portamento);
            pitchbends.getChildren().add(portamento.getElement());
        }
    }

    boolean hasPortamento(int position) {
        return portamentoMap.containsKey(position);
    }

    Portamento getPortamento(int position) {
        return portamentoMap.get(position);
    }

    boolean isEmpty() {
        return noteMap.isEmpty() && envelopeMap.isEmpty() && portamentoMap.isEmpty();
    }
}
