package com.utsusynth.utsu.model.song;

import java.util.Iterator;
import java.util.Optional;
import com.utsusynth.utsu.common.utils.RegionBounds;

/**
 * Iterator over song notes using a linked list implementation.
 */
public class NoteIterator implements Iterator<Note> {
    private final RegionBounds bounds;

    private Optional<NoteNode> prevNode;
    private Optional<NoteNode> curNode;
    int curIndex;
    int curDelta;

    NoteIterator(Optional<NoteNode> startNode, RegionBounds bounds) {
        this.bounds = bounds;
        this.prevNode = Optional.empty();
        this.curNode = startNode;
        this.curIndex = 0;
        this.curDelta = 0;

        // Start at first note contained within bounds, if it exists.
        while (curNode.isPresent()) {
            int newDelta = curDelta + curNode.get().getNote().getDelta();
            if (bounds.intersects(newDelta, newDelta + curNode.get().getNote().getDuration())) {
                break;
            } else {
                curIndex++;
                curDelta = newDelta;
                curNode = curNode.get().getNext();
            }
        }
    }

    @Override
    public boolean hasNext() {
        return curNode.isPresent();
    }

    @Override
    public Note next() {
        if (curNode.isEmpty()) {
            return null;
        }
        Note note = curNode.get().getNote();
        curIndex++;
        curDelta += note.getDelta();
        prevNode = curNode;

        // Set up next note, if within bounds.
        curNode = curNode.get().getNext();
        if (curNode.isPresent()) {
            int futureDelta = curDelta + curNode.get().getNote().getDelta();
            int futureDuration = curNode.get().getNote().getDuration();
            if (!bounds.intersects(futureDelta, futureDelta + futureDuration)) {
                curNode = Optional.empty();
            }
        }

        return note;
    }

    public int getCurIndex() {
        return curIndex;
    }

    public int getCurDelta() {
        return curDelta;
    }

    /** Look at the previous node without proceeding in iterator. */
    public Optional<Note> peekPrev() {
        if (!prevNode.isPresent() || !prevNode.get().getPrev().isPresent()) {
            return Optional.empty();
        } else {
            return Optional.of(prevNode.get().getPrev().get().getNote());
        }
    }

    /** Look at the next node without proceeding in iterator. */
    public Optional<Note> peekNext() {
        if (!curNode.isPresent()) {
            return Optional.empty();
        } else {
            return Optional.of(curNode.get().getNote());
        }
    }
}
