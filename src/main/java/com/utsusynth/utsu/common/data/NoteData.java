package com.utsusynth.utsu.common.data;

import com.google.common.base.Optional;

/**
 * Note data meant to pass information about a note from frontend to backend and vice versa. Not
 * meant to store permanent information about a note.
 */
public class NoteData {
    private final int position;
    private final int duration;
    private final String pitch;
    private final String lyric;

    // Optional fields.
    private final Optional<EnvelopeData> envelope;
    private final Optional<PitchbendData> pitchbend;
    private final Optional<NoteConfigData> config;

    public NoteData(int position, int duration, String pitch, String lyric) {
        this.position = position;
        this.duration = duration;
        this.pitch = pitch;
        this.lyric = lyric;
        this.envelope = Optional.absent();
        this.pitchbend = Optional.absent();
        this.config = Optional.absent();
    }

    public NoteData(
            int position,
            int duration,
            String pitch,
            String lyric,
            Optional<EnvelopeData> envelope,
            Optional<PitchbendData> pitchbend,
            Optional<NoteConfigData> config) {
        this.position = position;
        this.duration = duration;
        this.pitch = pitch;
        this.lyric = lyric;
        this.envelope = envelope;
        this.pitchbend = pitchbend;
        this.config = config;
    }

    public NoteData(int position, int duration, String pitch, String lyric, EnvelopeData envelope) {
        this.position = position;
        this.duration = duration;
        this.pitch = pitch;
        this.lyric = lyric;
        this.envelope = Optional.of(envelope);
        this.pitchbend = Optional.absent();
        this.config = Optional.absent();
    }

    public NoteData(
            int position,
            int duration,
            String pitch,
            String lyric,
            PitchbendData pitchbend) {
        this.position = position;
        this.duration = duration;
        this.pitch = pitch;
        this.lyric = lyric;
        this.envelope = Optional.absent();
        this.pitchbend = Optional.of(pitchbend);
        this.config = Optional.absent();
    }

    public int getPosition() {
        return this.position;
    }

    public int getDuration() {
        return this.duration;
    }

    public String getPitch() {
        return this.pitch;
    }

    public String getLyric() {
        return this.lyric;
    }

    public Optional<EnvelopeData> getEnvelope() {
        return this.envelope;
    }

    public Optional<PitchbendData> getPitchbend() {
        return this.pitchbend;
    }

    public Optional<NoteConfigData> getConfig() {
        return this.config;
    }
}
