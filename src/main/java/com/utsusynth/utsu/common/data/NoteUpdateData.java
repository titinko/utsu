package com.utsusynth.utsu.common.data;

/**
 * Relevant data needed to update a note that already exists.
 */
public class NoteUpdateData {
    private final int position; // Note's position in ms.
    private final String trueLyric;
    private final EnvelopeData envelope;
    private final PitchbendData pitchbend;
    private final NoteConfigData configData;

    public NoteUpdateData(
            int position,
            String trueLyric,
            EnvelopeData envelope,
            PitchbendData pitchbend,
            NoteConfigData configData) {
        this.position = position;
        this.trueLyric = trueLyric;
        this.envelope = envelope;
        this.pitchbend = pitchbend;
        this.configData = configData;
    }

    public int getPosition() {
        return this.position;
    }

    public String getTrueLyric() {
        return this.trueLyric;
    }

    public EnvelopeData getEnvelope() {
        return this.envelope;
    }

    public PitchbendData getPitchbend() {
        return this.pitchbend;
    }

    public NoteConfigData getConfigData() {
        return this.configData;
    }
}
