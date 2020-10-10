package com.utsusynth.utsu.model.song;

import java.util.Optional;
import com.utsusynth.utsu.common.exception.NoteAlreadyExistsException;
import com.utsusynth.utsu.model.voicebank.Voicebank;

/**
 * Node of a linked list of SongNotes.
 */
public class NoteNode {
    private Note note;
    private Optional<NoteNode> prev;
    private Optional<NoteNode> next;

    NoteNode(Note note) {
        this.note = note;
        this.prev = Optional.empty();
        this.next = Optional.empty();
    }

    public Note getNote() {
        return this.note;
    }

    public Optional<NoteNode> getPrev() {
        return this.prev;
    }

    public Optional<NoteNode> getNext() {
        return this.next;
    }

    /**
     * Inserts new node directly to the right of the current one. Should only be used when reading
     * song notes from a file.
     */
    NoteNode appendRightFromFile(NoteNode appendMe, int overrideDelta) {
        if (overrideDelta > 0) {
            appendMe.note.setDelta(overrideDelta);
        }
        if (appendMe.note.getDelta() == -1) {
            appendMe.note.setDelta(this.note.getLength());
        }
        if (appendMe.note.getLength() == -1) {
            appendMe.note.setLength(appendMe.note.getDuration());
        }
        this.next = Optional.of(appendMe);
        appendMe.prev = Optional.of(this);
        return appendMe;
    }

    /**
     * Inserts first note into a note list. Should only be used when reading song notes from a file.
     */
    static void appendFirstFromFile(NoteNode appendMe, int overrideDelta) {
        if (overrideDelta > 0) {
            appendMe.note.setDelta(overrideDelta);
        }
        if (appendMe.note.getDelta() == -1) {
            appendMe.note.setDelta(0);
        }
        if (appendMe.note.getLength() == -1) {
            appendMe.note.setLength(appendMe.note.getDuration());
        }
    }

    NoteNode insertFirstNote(Note noteToInsert, int deltaToInsert) {
        int newDelta = this.note.getDelta() - deltaToInsert;
        this.note.setDelta(newDelta);
        noteToInsert.safeSetLength(newDelta);
        noteToInsert.setDelta(deltaToInsert);
        NoteNode nodeToInsert = new NoteNode(noteToInsert);
        nodeToInsert.next = Optional.of(this);
        this.prev = Optional.of(nodeToInsert);
        return nodeToInsert;
    }

    /**
     * Inserts a note into the linked list.
     * 
     * @param noteToInsert
     * @param deltaToInsert
     * @param prevDelta
     * @return The new node that was inserted.
     * @throws NoteAlreadyExistsException
     */
    NoteNode insertNote(Note noteToInsert, int deltaToInsert, int prevDelta)
            throws NoteAlreadyExistsException {
        int curDelta = prevDelta + this.note.getDelta();
        if (deltaToInsert < curDelta) {
            if (!this.prev.isPresent()) {
                // TODO: Throw an error.
                System.out.println("ERROR: Tried to replace head from inside SongNode!");
                return null;
            } else {
                // Update with new lengths and deltas.
                NoteNode prevNode = this.prev.get();
                int prevToInsertedNote = deltaToInsert - prevDelta;
                prevNode.getNote().safeSetLength(prevToInsertedNote);
                noteToInsert.setDelta(prevToInsertedNote);
                int insertedToCurNote = curDelta - deltaToInsert;
                noteToInsert.safeSetLength(insertedToCurNote);
                this.note.setDelta(insertedToCurNote);
                // Insert note before.
                NoteNode nodeToInsert = new NoteNode(noteToInsert);
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
            this.note.safeSetLength(curToInsertedNote);
            noteToInsert.setDelta(curToInsertedNote);
            noteToInsert.safeSetLength(noteToInsert.getDuration());
            // Insert note after.
            NoteNode nodeToInsert = new NoteNode(noteToInsert);
            this.next = Optional.of(nodeToInsert);
            nodeToInsert.prev = Optional.of(this);
            return nodeToInsert;
        } else {
            // Continue down the linked list.
            return this.next.get().insertNote(noteToInsert, deltaToInsert, curDelta);
        }
    }

    Optional<NoteNode> removeFirstNote() {
        if (this.next.isPresent()) {
            NoteNode nextNode = this.next.get();
            nextNode.note.setDelta(this.note.getDelta() + nextNode.note.getDelta());
            nextNode.prev = Optional.empty();
            return Optional.of(nextNode);
        }
        return Optional.empty();
    }

    /**
     * Removes current node from the linked list.
     */
    void removeNote() {
        // Remove this node.
        if (this.prev.isPresent() && this.next.isPresent()) {
            // Deleting a node in the middle of the linked list.
            NoteNode prevNode = this.prev.get();
            NoteNode nextNode = this.next.get();
            int newDelta = this.note.getDelta() + this.note.getLength();
            prevNode.note.safeSetLength(newDelta);
            nextNode.note.setDelta(newDelta);
            prevNode.next = this.next;
            nextNode.prev = this.prev;
        } else if (this.prev.isPresent()) {
            // Deleting the final node.
            NoteNode prevNode = this.prev.get();
            prevNode.note.safeSetLength(prevNode.note.getDuration());
            prevNode.next = Optional.empty();
        } else if (this.next.isPresent()) {
            // TODO: Throw error.
            System.out.println("ERROR: Tried to delete first node from inside NoteNode!");
        } else {
            // TODO: Throw error.
            System.out.println("ERROR: Tried to delete the only node from inside NoteNode!");
        }
    }

    void standardize(NoteStandardizer standardizer, Voicebank voicebank) {
        Optional<Note> prevNote = getOptionalNote(this.prev);
        Optional<Note> nextNote = getOptionalNote(this.next);
        standardizer.standardize(prevNote, this.note, nextNote, voicebank);
    }

    private static Optional<Note> getOptionalNote(Optional<NoteNode> fromNode) {
        Optional<Note> maybeNote = Optional.empty();
        if (fromNode.isPresent()) {
            maybeNote = Optional.of(fromNode.get().getNote());
        }
        return maybeNote;
    }
}
