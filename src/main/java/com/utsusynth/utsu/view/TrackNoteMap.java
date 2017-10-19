package com.utsusynth.utsu.view;

import java.util.HashMap;
import java.util.Map;

import com.google.inject.Inject;
import com.utsusynth.utsu.common.exception.NoteAlreadyExistsException;
import com.utsusynth.utsu.common.quantize.QuantizedEnvelope;
import com.utsusynth.utsu.common.quantize.QuantizedPortamento;
import com.utsusynth.utsu.view.note.TrackNote;
import com.utsusynth.utsu.view.note.envelope.TrackEnvelope;
import com.utsusynth.utsu.view.note.envelope.TrackEnvelopeCallback;
import com.utsusynth.utsu.view.note.envelope.TrackEnvelopeFactory;
import com.utsusynth.utsu.view.note.portamento.TrackPortamento;
import com.utsusynth.utsu.view.note.portamento.TrackPortamentoCallback;
import com.utsusynth.utsu.view.note.portamento.TrackPortamentoFactory;

import javafx.scene.Group;

public class TrackNoteMap {
	private final TrackEnvelopeFactory envelopeFactory;
	private final TrackPortamentoFactory portamentoFactory;

	// Maps absolute position to track note's data.
	private Map<Integer, TrackNote> noteMap;
	private Map<Integer, TrackEnvelope> envelopeMap;
	private Map<Integer, TrackPortamento> portamentoMap;
	private Group notes;
	private Group envelopes;
	private Group pitchbends;

	@Inject
	public TrackNoteMap(TrackEnvelopeFactory envFactory, TrackPortamentoFactory portFactory) {
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

	TrackNote getNote(int position) {
		return noteMap.get(position);
	}

	void putNote(int position, TrackNote note) throws NoteAlreadyExistsException {
		if (noteMap.containsKey(position)) {
			throw new NoteAlreadyExistsException();
		}
		noteMap.put(position, note);
	}

	void addNoteElement(TrackNote note) {
		notes.getChildren().add(note.getElement());
	}

	void removeNoteElement(TrackNote note) {
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

	void putEnvelope(int position, QuantizedEnvelope qEnvelope, TrackEnvelopeCallback callback) {
		// Track note must exist before envelope is added.
		if (noteMap.containsKey(position)) {
			TrackEnvelope envelope =
					envelopeFactory.createEnvelope(noteMap.get(position), qEnvelope, callback);
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

	TrackEnvelope getEnvelope(int position) {
		return envelopeMap.get(position);
	}

	void putPortamento(
			int position,
			QuantizedPortamento qPortamento,
			TrackPortamentoCallback callback) {
		if (noteMap.containsKey(position)) {
			TrackPortamento pitchbend = portamentoFactory
					.createPortamento(noteMap.get(position), qPortamento, callback);
			// Overrides are expected here.
			if (portamentoMap.containsKey(position)) {
				pitchbends.getChildren().remove(portamentoMap.get(position).getElement());
			}
			portamentoMap.put(position, pitchbend);
			pitchbends.getChildren().add(pitchbend.getElement());
		}
	}

	boolean hasPortamento(int position) {
		return portamentoMap.containsKey(position);
	}

	TrackPortamento getPortamento(int position) {
		return portamentoMap.get(position);
	}

	boolean isEmpty() {
		return noteMap.isEmpty() && envelopeMap.isEmpty() && portamentoMap.isEmpty();
	}
}
