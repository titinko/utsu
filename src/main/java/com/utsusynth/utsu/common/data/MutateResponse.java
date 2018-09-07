package com.utsusynth.utsu.common.data;

import com.google.common.base.Optional;

public class MutateResponse {
    private final NoteUpdateData note;
    private final Optional<NoteUpdateData> prev;
    private final Optional<NoteUpdateData> next;

    public MutateResponse(
            NoteUpdateData note,
            Optional<NoteUpdateData> prev,
            Optional<NoteUpdateData> next) {
        this.note = note;
        this.prev = prev;
        this.next = next;
    }

    public NoteUpdateData getNote() {
        return this.note;
    }

    public Optional<NoteUpdateData> getPrev() {
        return this.prev;
    }

    public Optional<NoteUpdateData> getNext() {
        return this.next;
    }
}
