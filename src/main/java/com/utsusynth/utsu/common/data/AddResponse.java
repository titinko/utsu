package com.utsusynth.utsu.common.data;

import com.google.common.base.Optional;

public class AddResponse {
    private final NoteData note;
    private final Optional<NeighborData> prev;
    private final Optional<NeighborData> next;

    public AddResponse(NoteData note, Optional<NeighborData> prev, Optional<NeighborData> next) {
        this.note = note;
        this.prev = prev;
        this.next = next;
    }

    public NoteData getNote() {
        return this.note;
    }

    public Optional<NeighborData> getPrev() {
        return this.prev;
    }

    public Optional<NeighborData> getNext() {
        return this.next;
    }
}
