package com.utsusynth.utsu.common.data;

/**
 * Relevant data for the frontend to know about a note's neighbors when the note is added/removed.
 */
public class NeighborData {
    private final int delta;
    private final EnvelopeData envelope;
    private final PitchbendData pitchbend;

    public NeighborData(int delta, EnvelopeData envelope, PitchbendData pitchbend) {
        this.delta = delta;
        this.envelope = envelope;
        this.pitchbend = pitchbend;
    }

    public int getDelta() {
        return this.delta;
    }

    public EnvelopeData getEnvelope() {
        return this.envelope;
    }

    public PitchbendData getPitchbend() {
        return this.pitchbend;
    }
}
