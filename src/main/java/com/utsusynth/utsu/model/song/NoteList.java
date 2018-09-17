package com.utsusynth.utsu.model.song;

import java.util.HashMap;
import java.util.Map;
import com.google.common.base.Optional;
import com.utsusynth.utsu.common.RegionBounds;
import com.utsusynth.utsu.common.exception.NoteAlreadyExistsException;
import com.utsusynth.utsu.model.voicebank.Voicebank;

/**
 * Implementation of a linked list of SongNotes, including head.
 */
public class NoteList implements Iterable<Note> {
    private Optional<NoteNode> head;
    private Map<Integer, NoteNode> nodeMap;

    public class Builder {
        private NoteList noteList;
        private Optional<NoteNode> tail;
        private int totalDelta;
        // Delta that is calculated from invalid notes in multi-track UST.
        private int overrideDelta;

        private Builder(NoteList noteList) {
            this.noteList = noteList;
            this.tail = Optional.absent();
            this.totalDelta = 0;
            this.overrideDelta = 0;
        }

        private Builder setHead(Optional<NoteNode> newHead) {
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

        public Builder appendNote(Note note) {
            NoteNode newNode = new NoteNode(note);
            if (!noteList.head.isPresent()) {
                NoteNode.appendFirstFromFile(newNode, overrideDelta);
                overrideDelta = 0;
                if (tail.isPresent()) {
                    // Case where first note was a rest note.
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

        public Builder appendRestNote(Note note) {
            NoteNode newNode = new NoteNode(note);
            if (!noteList.head.isPresent()) {
                if (!tail.isPresent()) {
                    tail = Optional.of(newNode);
                } else {
                    tail.get().getNote()
                            .setDuration(tail.get().getNote().getDuration() + note.getDuration());
                }
            } else if (tail.isPresent()) {
                int tailLength = tail.get().getNote().getLength();
                tail.get().getNote().setLength(tailLength + note.getDuration());
            } else {
                // TODO: throw error
                System.out.println("Unexpected error while making note list.");
                return this;
            }
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

        public Builder standardize(NoteStandardizer standardizer, Voicebank voicebank) {
            Optional<NoteNode> cur = tail;
            while (cur.isPresent()) {
                cur.get().standardize(standardizer, voicebank);
                cur = cur.get().getPrev();
            }
            return this;
        }

        public Optional<Note> getLatestNote() {
            // Search for an existing non-rest note.
            if (noteList.head.isPresent() && tail.isPresent()) {
                return Optional.of(tail.get().getNote());
            }
            return Optional.absent();
        }

        public int getLatestDelta() {
            return totalDelta;
        }

        public NoteList build() {
            return noteList;
        }
    }

    public NoteList() {
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
    NoteNode insertNote(Note noteToInsert, int deltaToInsert) throws NoteAlreadyExistsException {
        NoteNode inserted;
        if (!head.isPresent()) {
            this.head = Optional.of(new NoteNode(noteToInsert));
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
     * Adds a note to the note list, starting search at a specific node.
     */
    NoteNode insertNote(Note noteToInsert, int deltaToInsert, NoteNode startNode, int startDelta)
            throws NoteAlreadyExistsException {
        NoteNode inserted = startNode.insertNote(noteToInsert, deltaToInsert, startDelta);
        nodeMap.put(deltaToInsert, inserted);
        return inserted;
    }

    /**
     * Removes a note from the note list.
     * 
     * @param deltaToRemove
     * @return The node that was removed.
     */
    NoteNode removeNote(int deltaToRemove) {
        NoteNode toRemove;
        if (!head.isPresent()) {
            // TODO: Throw an error here.
            return null;
        } else if (head.get().getNote().getDelta() == deltaToRemove) {
            toRemove = this.head.get();
            this.head = this.head.get().removeFirstNote();
        } else {
            toRemove = nodeMap.get(deltaToRemove);
            toRemove.removeNote();
        }
        nodeMap.remove(deltaToRemove);
        return toRemove;
    }

    /**
     * Fetches a note from the note list.
     * 
     * @param deltaOfNote
     * @return The node found at that delta.
     */
    NoteNode getNote(int deltaOfNote) {
        // TODO: Handle case where note not found at that delta.
        return nodeMap.get(deltaOfNote);
    }

    /**
     * Returns the total number of notes in the note list.
     * 
     * @return number of notes
     */
    int getSize() {
        return nodeMap.size();
    }

    Builder toBuilder() {
        // Creates a new SongNoteList but reuses existing SongNodes.
        return new Builder(new NoteList()).setHead(this.head);
    }

    @Override
    public NoteIterator iterator() {
        return new NoteIterator(this.head, RegionBounds.WHOLE_SONG);
    }

    NoteIterator boundedIterator(RegionBounds bounds) {
        return new NoteIterator(this.head, bounds);
    }
}
