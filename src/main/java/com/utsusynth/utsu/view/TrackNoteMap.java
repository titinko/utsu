package com.utsusynth.utsu.view;

import java.util.HashMap;
import java.util.Map;

import com.google.inject.Inject;
import com.utsusynth.utsu.common.exception.NoteAlreadyExistsException;
import com.utsusynth.utsu.common.quantize.QuantizedEnvelope;
import com.utsusynth.utsu.view.note.TrackEnvelope;
import com.utsusynth.utsu.view.note.TrackEnvelopeCallback;
import com.utsusynth.utsu.view.note.TrackNote;
import com.utsusynth.utsu.view.note.TrackNoteFactory;

import javafx.scene.Group;

public class TrackNoteMap {
	private final TrackNoteFactory noteFactory;

	// Maps absolute position to track note's data.
	private Map<Integer, TrackNote> noteMap;
	private Map<Integer, TrackEnvelope> envelopeMap;
	private Group envelopes;

	@Inject
	public TrackNoteMap(TrackNoteFactory noteFactory) {
		this.noteFactory = noteFactory;
		clear();
	}

	Group getEnvelopesElement() {
		return envelopes;
	}

	void clear() {
		noteMap = new HashMap<>();
		envelopeMap = new HashMap<>();
		envelopes = new Group();
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
	}

	void putEnvelope(int position, QuantizedEnvelope qEnvelope, TrackEnvelopeCallback callback) {
		// Track note must exist before envelope is added.
		if (noteMap.containsKey(position)) {
			TrackEnvelope envelope =
					noteFactory.createEnvelope(noteMap.get(position), qEnvelope, callback);
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

	boolean isEmpty() {
		return noteMap.isEmpty() && envelopeMap.isEmpty();
	}
}
