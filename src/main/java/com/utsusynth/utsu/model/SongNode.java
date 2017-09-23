package com.utsusynth.utsu.model;

import com.google.common.base.Optional;
import com.utsusynth.utsu.common.exception.NoteAlreadyExistsException;

/**
 * Implementation of a linked list of SongNotes.
 */
public class SongNode {
	private SongNote note;
	private Optional<SongNode> prev;
	private Optional<SongNode> next;

	SongNode(SongNote note) {
		this.note = note;
		this.prev = Optional.absent();
		this.next = Optional.absent();
	}
	
	public SongNote getNote() {
		return this.note;
	}
	
	public Optional<SongNode> getPrev() {
		return this.prev;
	}
	
	public Optional<SongNode> getNext() {
		return this.next;
	}
	
	/** 
	 * Inserts new node directly to the right of the current one. 
	 * Should only be used when reading song notes from a file.
	 */
	SongNode appendRightFromFile(SongNode appendMe) {
		if (appendMe.note.getDelta() == -1) {
			appendMe.note.setDelta(this.note.getLength());
		}
		if (appendMe.note.getLength() == -1) {
			appendMe.note.setLength(appendMe.note.getDuration());
		}
		if (appendMe.note.getDuration() > appendMe.note.getLength()) {
			// TODO: Assert that this never appears, since the problem should be caught sooner.
			System.out.println("Warning: note's length is greater than its duration.");
			appendMe.note.setDuration(appendMe.note.getLength());
		}
		this.next = Optional.of(appendMe);
		appendMe.prev = Optional.of(this);
		return appendMe;
	}
	
	/**
	 * Inserts first note into a note list.  
	 * Should only be used when reading song notes from a file.
	 */
	static void appendFirstFromFile(SongNode appendMe) {
		if (appendMe.note.getDelta() == -1) {
			appendMe.note.setDelta(0);
		}
		if (appendMe.note.getLength() == -1) {
			appendMe.note.setLength(appendMe.note.getDuration());
		}
		if (appendMe.note.getDuration() > appendMe.note.getLength()) {
			// TODO: Assert that this never appears, since the problem should be caught sooner.
			System.out.println("Warning: note's length is greater than its duration.");
			appendMe.note.setDuration(appendMe.note.getLength());
		}
	}
	
	Optional<SongNode> insertFirstNote(SongNote noteToInsert, int deltaToInsert) {
		int newDelta = this.note.getDelta() - deltaToInsert;
		this.note.setDelta(newDelta);
		noteToInsert.setLength(newDelta);
		noteToInsert.setDelta(deltaToInsert);
		SongNode nodeToInsert = new SongNode(noteToInsert);
		nodeToInsert.next = Optional.of(this);
		this.prev = Optional.of(nodeToInsert);
		return Optional.of(nodeToInsert);
	}
		
	/**
	 * Inserts a note into the linked list.
	 * @param noteToInsert
	 * @param deltaToInsert
	 * @param prevDelta
	 * @return The new node that was inserted.
	 * @throws NoteAlreadyExistsException 
	 */
	SongNode insertNote(SongNote noteToInsert, int deltaToInsert, int prevDelta)
			throws NoteAlreadyExistsException {
		int curDelta = prevDelta + this.note.getDelta();
		if (deltaToInsert < curDelta) {
			if (!this.prev.isPresent()) {
				// TODO: Throw an error.
				System.out.println("ERROR: Tried to replace head from inside SongNode!");
				return null;
			} else {
				// Update with new lengths and deltas.
				SongNode prevNode = this.prev.get();
				int prevToInsertedNote = deltaToInsert - prevDelta;
				prevNode.getNote().setLength(prevToInsertedNote);
				noteToInsert.setDelta(prevToInsertedNote);
				int insertedToCurNote = curDelta - deltaToInsert;
				noteToInsert.setLength(insertedToCurNote);
				this.note.setDelta(insertedToCurNote);
				// Insert note before.
				SongNode nodeToInsert = new SongNode(noteToInsert);
				prevNode.next = Optional.of(nodeToInsert);
				nodeToInsert.prev = Optional.of(prevNode);
				nodeToInsert.next = Optional.of(this);
				this.prev = Optional.of(nodeToInsert);
				return nodeToInsert;
			}
		} else if (deltaToInsert == curDelta) {
			// Don't insert note.
			throw new NoteAlreadyExistsException();
		} else if (!this.next.isPresent()) {
			// Update with new length and delta.
			int curToInsertedNote = deltaToInsert - curDelta;
			this.note.setLength(curToInsertedNote);
			noteToInsert.setDelta(curToInsertedNote);
			noteToInsert.setLength(noteToInsert.getDuration());
			// Insert note after.
			SongNode nodeToInsert = new SongNode(noteToInsert);
			this.next = Optional.of(nodeToInsert);
			nodeToInsert.prev = Optional.of(this);
			return nodeToInsert;
		} else {
			// Continue down the linked list.
			return this.next.get().insertNote(noteToInsert, deltaToInsert, curDelta);
		}
	}
	
	Optional<SongNode> removeFirstNote() {
		if (this.next.isPresent()) {
			SongNode nextNode = this.next.get();
			nextNode.note.setDelta(this.note.getDelta() + nextNode.note.getDelta());
			nextNode.prev = Optional.absent();
			return Optional.of(nextNode);
		}
		return Optional.absent();
	}
	
	/**
	 * Removes a note from the linked list.
	 * @param deltaToRemove
	 * @param prevDelta
	 * @return true if the deletion was successful, false otherwise
	 */
	SongNode removeNote(int deltaToRemove, int prevDelta) {
		int curDelta = prevDelta + this.note.getDelta();
		if (deltaToRemove == curDelta) {
			// Remove this node.
			if (this.prev.isPresent() && this.next.isPresent()) {
				// Deleting a node in the middle of the linked list.
				SongNode prevNode = this.prev.get();
				SongNode nextNode = this.next.get();
				int newDelta = this.note.getDelta() + this.note.getLength();
				prevNode.note.setLength(newDelta);
				nextNode.note.setDelta(newDelta);
				prevNode.next = this.next;
				nextNode.prev = this.prev;
			} else if (this.prev.isPresent()) {
				// Deleting the final node.
				SongNode prevNode = this.prev.get();
				prevNode.note.setLength(prevNode.note.getDuration());
				prevNode.next = Optional.absent();
			} else if (this.next.isPresent()) {
				// TODO: Throw error.
				System.out.println("ERROR: Tried to delete first node from inside SongNode!");
				return null;
			} else {
				// TODO: Throw error.
				System.out.println("ERROR: Tried to delete the only node from inside SongNode!");
				return null;
			}
			return this;
		} else if (!this.next.isPresent()) {
			// TODO: Throw error.
			System.out.println("Failed to find note :(");
			return null;
		} else {
			return this.next.get().removeNote(deltaToRemove, curDelta);
		}
	}
}
