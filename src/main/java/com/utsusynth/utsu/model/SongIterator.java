package com.utsusynth.utsu.model;

import java.util.Iterator;
import com.google.common.base.Optional;
import com.utsusynth.utsu.common.RegionBounds;

/**
 * Iterator over song notes using a linked list implementation.
 */
public class SongIterator implements Iterator<SongNote> {
    private final RegionBounds bounds;

    private Optional<SongNode> prevNode;
    private Optional<SongNode> curNode;
    int curDelta;

    SongIterator(Optional<SongNode> startNode, RegionBounds bounds) {
        this.bounds = bounds;
        this.prevNode = Optional.absent();
        this.curNode = startNode;
        this.curDelta = 0;

        // Start at first note contained within bounds, if it exists.
        while (curNode.isPresent()) {
            int newDelta = curDelta + curNode.get().getNote().getDelta();
            if (bounds.intersects(newDelta, newDelta + curNode.get().getNote().getDuration())) {
                break;
            } else {
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
    public SongNote next() {
        if (!curNode.isPresent()) {
            return null;
        }
        SongNote note = curNode.get().getNote();
        curDelta += note.getDelta();
        prevNode = curNode;

        // Set up next note, if within bounds.
        curNode = curNode.get().getNext();
        if (curNode.isPresent()) {
            int futureDelta = curDelta + curNode.get().getNote().getDelta();
            int futureDuration = curNode.get().getNote().getDuration();
            if (!bounds.intersects(futureDelta, futureDelta + futureDuration)) {
                curNode = Optional.absent();
            }
        }

        return note;
    }

    public int getCurDelta() {
        return curDelta;
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
