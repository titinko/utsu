package com.utsusynth.utsu.common.data;

import java.util.Optional;

/** Information about a note and its preceeding and succeeding notes if present. */
public class NoteContextData {
    private final NoteData note;
    private final Optional<NoteData> prev;
    private final Optional<NoteData> next;

    public NoteContextData(
            NoteData note,
            Optional<NoteData> prev,
            Optional<NoteData> next) {
        this.note = note;
        this.prev = prev;
        this.next = next;
    }

    public NoteData getNote() {
        return note;
    }

    public Optional<NoteData> getPrev() {
        return prev;
    }

    public Optional<NoteData> getNext() {
        return next;
    }
}
