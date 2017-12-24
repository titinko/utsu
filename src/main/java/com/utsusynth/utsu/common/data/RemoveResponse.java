package com.utsusynth.utsu.common.data;

import com.google.common.base.Optional;

public class RemoveResponse {
    private final Optional<NeighborData> prev;
    private final Optional<NeighborData> next;

    public RemoveResponse(Optional<NeighborData> prev, Optional<NeighborData> next) {
        this.prev = prev;
        this.next = next;
    }

    public Optional<NeighborData> getPrev() {
        return this.prev;
    }

    public Optional<NeighborData> getNext() {
        return this.next;
    }
}
