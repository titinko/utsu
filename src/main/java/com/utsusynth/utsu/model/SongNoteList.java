package com.utsusynth.utsu.model;

import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Optional;
import com.utsusynth.utsu.common.exception.NoteAlreadyExistsException;
import com.utsusynth.utsu.model.voicebank.Voicebank;

/**
 * Implementation of a linked list of SongNotes, including head.
 */
public class SongNoteList implements Iterable<SongNote> {
	private Optional<SongNode> head;
	private Map<Integer, SongNode> nodeMap;

	public class Builder {
		private SongNoteList noteList;
		private Optional<SongNode> tail;
		private int totalDelta;
		// Delta that is calculated from invalid notes in multi-track UST.
		private int overrideDelta;

		private Builder(SongNoteList noteList) {
			this.noteList = noteList;
			this.tail = Optional.absent();
			this.totalDelta = 0;
			this.overrideDelta = 0;
		}

		private Builder setHead(Optional<SongNode> newHead) {
			noteList.head = newHead;
			noteList.nodeMap = new HashMap<>();
			totalDelta = 0;
			overrideDelta = 0;
			if (newHead.isPresent()) {
				tail = newHead;
				totalDelta += newHead.get().getNote().getDelta();
				noteList.nodeMap.put(totalDelta, newHead.get());
				while (tail.get().getNext().isPresent()) {
					tail = tail.get().getNext();
					totalDelta += tail.get().getNote().getDelta();
					noteList.nodeMap.put(totalDelta, tail.get());
				}
			}
			return this;
		}

		public Builder appendNote(SongNote note) {
			SongNode newNode = new SongNode(note);
			if (!noteList.head.isPresent()) {
				SongNode.appendFirstFromFile(newNode, overrideDelta);
				overrideDelta = 0;
				if (tail.isPresent()) {
					// Case where first note was a rest node.
					note.setDelta(note.getDelta() + tail.get().getNote().getDuration());
				}
				tail = Optional.of(newNode);
				noteList.head = Optional.of(newNode);
			} else if (tail.isPresent()) {
				tail = Optional.of(tail.get().appendRightFromFile(newNode, overrideDelta));
				overrideDelta = 0;
			} else {
				// TODO: throw error
				System.out.println("Unexpected error while making note list.");
				return this;
			}
			totalDelta += note.getDelta();
			if (tail.isPresent()) {
				noteList.nodeMap.put(totalDelta, tail.get());
			}
			return this;
		}

		public Builder appendRestNote(SongNote note) {
			SongNode newNode = new SongNode(note);
			if (!noteList.head.isPresent()) {
				if (!tail.isPresent()) {
					tail = Optional.of(newNode);
				} else {
					tail.get().getNote().setDuration(
							tail.get().getNote().getDuration() + note.getDuration());
				}
			} else if (tail.isPresent()) {
				int tailLength = tail.get().getNote().getLength();
				tail.get().getNote().setLength(tailLength + note.getDuration());
			} else {
				// TODO: throw error
				System.out.println("Unexpected error while making note list.");
				return this;
			}
			totalDelta += note.getDelta();
			return this;
		}

		public Builder appendInvalidNote(int noteDelta, int noteLength) {
			if (tail.isPresent()) {
				// Add note length to current tail's length.
				overrideDelta = tail.get().getNote().getLength() + noteLength;
				// Current tail's length will be the next note's delta.
				tail.get().getNote().setLength(overrideDelta);
			} else {
				// If tail does not exist, adjust the delta for the first valid note.
				if (overrideDelta == 0) {
					overrideDelta += noteDelta;
				}
				overrideDelta += noteLength;
			}
			return this;
		}

		public Builder standardize(SongNoteStandardizer standardizer, Voicebank voicebank) {
			Optional<SongNode> cur = tail;
			while (cur.isPresent()) {
				cur.get().standardize(standardizer, voicebank);
				cur = cur.get().getPrev();
			}
			return this;
		}

		public Optional<SongNote> getLatestNote() {
			return tail.isPresent() ? Optional.of(tail.get().getNote()) : Optional.absent();
		}

		public int getLatestDelta() {
			return totalDelta;
		}

		public SongNoteList build() {
			return noteList;
		}
	}

	public SongNoteList() {
		this.head = Optional.absent();
		this.nodeMap = new HashMap<>();
	}

	/**
	 * Adds a note to the note list.
	 * 
	 * @param noteToInsert
	 * @param deltaToInsert
	 * @return The node that was added.
	 * @throws NoteAlreadyExistsException
	 */
	SongNode insertNote(SongNote noteToInsert, int deltaToInsert)
			throws NoteAlreadyExistsException {
		SongNode inserted;
		if (!head.isPresent()) {
			this.head = Optional.of(new SongNode(noteToInsert));
			this.head.get().getNote().setDelta(deltaToInsert);
			inserted = this.head.get();
		} else if (head.get().getNote().getDelta() > deltaToInsert) {
			this.head = Optional.of(this.head.get().insertFirstNote(noteToInsert, deltaToInsert));
			inserted = this.head.get();
		} else {
			inserted = this.head.get().insertNote(noteToInsert, deltaToInsert, 0);
		}
		nodeMap.put(deltaToInsert, inserted);
		return inserted;
	}

	/**
	 * Removes a note from the note list.
	 * 
	 * @param deltaToRemove
	 * @return The node that was removed.
	 */
	SongNode removeNote(int deltaToRemove) {
		SongNode removed;
		if (!head.isPresent()) {
			// TODO: Throw an error here.
			return null;
		} else if (head.get().getNote().getDelta() == deltaToRemove) {
			removed = this.head.get();
			this.head = this.head.get().removeFirstNote();
		} else {
			removed = head.get().removeNote(deltaToRemove, 0);
		}
		nodeMap.remove(deltaToRemove);
		return removed;
	}

	/**
	 * Fetches a note from the note list.
	 * 
	 * @param deltaOfNote
	 * @return The node found at that delta.
	 */
	SongNode getNote(int deltaOfNote) {
		// TODO: Handle case where note not found at that delta.
		return nodeMap.get(deltaOfNote);
	}

	Builder toBuilder() {
		// Creates a new SongNoteList but reuses existing SongNodes.
		return new Builder(new SongNoteList()).setHead(this.head);
	}

	@Override
	public SongIterator iterator() {
		return new SongIterator(this.head);
	}
}
