package com.utsusynth.utsu.view;

import java.util.HashMap;
import java.util.Map;

import com.google.inject.Inject;
import com.utsusynth.utsu.common.exception.NoteAlreadyExistsException;
import com.utsusynth.utsu.common.quantize.QuantizedEnvelope;
import com.utsusynth.utsu.common.quantize.QuantizedPitchbend;
import com.utsusynth.utsu.view.note.TrackEnvelope;
import com.utsusynth.utsu.view.note.TrackEnvelopeCallback;
import com.utsusynth.utsu.view.note.TrackNote;
import com.utsusynth.utsu.view.note.TrackNoteFactory;
import com.utsusynth.utsu.view.note.TrackPitchbend;
import com.utsusynth.utsu.view.note.TrackPitchbendCallback;

import javafx.scene.Group;

public class TrackNoteMap {
	private final TrackNoteFactory noteFactory;

	// Maps absolute position to track note's data.
	private Map<Integer, TrackNote> noteMap;
	private Map<Integer, TrackEnvelope> envelopeMap;
	private Map<Integer, TrackPitchbend> pitchbendMap;
	private Group notes;
	private Group envelopes;
	private Group pitchbends;

	@Inject
	public TrackNoteMap(TrackNoteFactory noteFactory) {
		this.noteFactory = noteFactory;
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
		pitchbendMap = new HashMap<>();
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
		if (pitchbendMap.containsKey(position)) {
			pitchbends.getChildren().remove(pitchbendMap.get(position).getElement());
			pitchbendMap.remove(position);
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

	void putPitchbend(
			int position,
			QuantizedPitchbend qPitchbend,
			TrackPitchbendCallback callback) {
		if (noteMap.containsKey(position)) {
			TrackPitchbend pitchbend =
					noteFactory.createPitchbend(noteMap.get(position), qPitchbend, callback);
			// Overrides are expected here.
			if (pitchbendMap.containsKey(position)) {
				pitchbends.getChildren().remove(pitchbendMap.get(position).getElement());
			}
			pitchbendMap.put(position, pitchbend);
			pitchbends.getChildren().add(pitchbend.getElement());
		}
	}

	boolean hasPitchbend(int position) {
		return pitchbendMap.containsKey(position);
	}

	TrackPitchbend getPitchbend(int position) {
		return pitchbendMap.get(position);
	}

	boolean isEmpty() {
		return noteMap.isEmpty() && envelopeMap.isEmpty() && pitchbendMap.isEmpty();
	}
}
