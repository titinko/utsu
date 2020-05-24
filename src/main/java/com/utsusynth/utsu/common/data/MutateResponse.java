package com.utsusynth.utsu.common.data;

import java.util.Optional;

public class MutateResponse {
    private final Iterable<NoteUpdateData> notes;
    private final Optional<NoteUpdateData> prev;
    private final Optional<NoteUpdateData> next;

    public MutateResponse(
            Iterable<NoteUpdateData> notes,
            Optional<NoteUpdateData> prev,
            Optional<NoteUpdateData> next) {
        this.notes = notes;
        this.prev = prev;
        this.next = next;
    }

    public Iterable<NoteUpdateData> getNotes() {
        return this.notes;
    }

    public Optional<NoteUpdateData> getPrev() {
        return this.prev;
    }

    public Optional<NoteUpdateData> getNext() {
        return this.next;
    }
}
