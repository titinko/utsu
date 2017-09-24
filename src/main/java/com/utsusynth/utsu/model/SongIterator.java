package com.utsusynth.utsu.model;

import java.util.Iterator;

import com.google.common.base.Optional;

/**
 * Iterator over song notes using a linked list implementation.
 */
public class SongIterator implements Iterator<SongNote> {
	private Optional<SongNode> prevNode;
	private Optional<SongNode> curNode;

	SongIterator(Optional<SongNode> startNode) {
		this.prevNode = Optional.absent();
		this.curNode = startNode;
	}

	@Override
	public boolean hasNext() {
		return curNode.isPresent();
	}

	@Override
	public SongNote next() {
		if (!curNode.isPresent()) {
			return null;
		}
		SongNote note = curNode.get().getNote();
		prevNode = curNode;
		curNode = curNode.get().getNext();
		return note;
	}

	/** Look at the previous node without proceeding in iterator. */
	public Optional<SongNote> peekPrev() {
		if (!prevNode.isPresent() || !prevNode.get().getPrev().isPresent()) {
			return Optional.absent();
		} else {
			return Optional.of(prevNode.get().getPrev().get().getNote());
		}
	}

	/** Look at the next node without proceeding in iterator. */
	public Optional<SongNote> peekNext() {
		if (!curNode.isPresent()) {
			return Optional.absent();
		} else {
			return Optional.of(curNode.get().getNote());
		}
	}
}
