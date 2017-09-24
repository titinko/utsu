package com.utsusynth.utsu.model;

import com.google.common.base.Optional;
import com.utsusynth.utsu.common.exception.NoteAlreadyExistsException;

/**
 * Implementation of a linked list of SongNotes.
 */
public class SongNoteList implements Iterable<SongNote> {
	private Optional<SongNode> head;

	public class Builder {
		private SongNoteList noteList;
		private Optional<SongNode> tail;
		private int totalDelta;

		private Builder(SongNoteList noteList) {
			this.noteList = noteList;
			this.noteList.head = Optional.absent();
			this.tail = Optional.absent();
			this.totalDelta = 0;
		}

		public Builder appendNote(SongNote note) {
			SongNode newNode = new SongNode(note);
			if (!noteList.head.isPresent()) {
				SongNode.appendFirstFromFile(newNode);
				if (tail.isPresent()) {
					// Case where first note was a rest node.
					note.setDelta(note.getDelta() + tail.get().getNote().getDuration());
				}
				tail = Optional.of(newNode);
				noteList.head = Optional.of(newNode);
			} else if (tail.isPresent()) {
				tail = Optional.of(tail.get().appendRightFromFile(newNode));
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
				tail = Optional.of(newNode);
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

	static Builder newBuilder() {
		return new SongNoteList().toBuilder();
	}

	private SongNoteList() {
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
			this.head = this.head.get().insertFirstNote(noteToInsert, deltaToInsert);
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

	private Builder toBuilder() {
		return new Builder(this);
	}

	@Override
	public SongIterator iterator() {
		return new SongIterator(this.head);
	}
}
