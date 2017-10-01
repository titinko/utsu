package com.utsusynth.utsu.model;

import com.google.common.base.Optional;
import com.utsusynth.utsu.common.exception.NoteAlreadyExistsException;

/**
 * Implementation of a linked list of SongNotes, including head.
 */
public class SongNoteList implements Iterable<SongNote> {
	private Optional<SongNode> head;

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
			if (newHead.isPresent()) {
				tail = newHead;
				while (tail.get().getNext().isPresent()) {
					tail = tail.get().getNext();
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
		if (!head.isPresent()) {
			this.head = Optional.of(new SongNode(noteToInsert));
			this.head.get().getNote().setDelta(deltaToInsert);
			return this.head.get();
		} else if (head.get().getNote().getDelta() > deltaToInsert) {
			this.head = Optional.of(this.head.get().insertFirstNote(noteToInsert, deltaToInsert));
			return this.head.get();
		} else {
			return this.head.get().insertNote(noteToInsert, deltaToInsert, 0);
		}
	}

	/**
	 * Removes a note from the note list.
	 * 
	 * @param deltaToRemove
	 * @return The node that was removed.
	 */
	SongNode removeNote(int deltaToRemove) {
		if (!head.isPresent()) {
			// TODO: Throw an error here.
			return null;
		} else if (head.get().getNote().getDelta() == deltaToRemove) {
			SongNode removed = this.head.get();
			this.head = this.head.get().removeFirstNote();
			return removed;
		} else {
			return head.get().removeNote(deltaToRemove, 0);
		}
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
