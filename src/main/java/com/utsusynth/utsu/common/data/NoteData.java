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
    private final Optional<String> trueLyric;
    private final Optional<EnvelopeData> envelope;
    private final Optional<PitchbendData> pitchbend;

    public NoteData(int position, int duration, String pitch, String lyric) {
        this.position = position;
        this.duration = duration;
        this.pitch = pitch;
        this.lyric = lyric;
        this.trueLyric = Optional.absent();
        this.envelope = Optional.absent();
        this.pitchbend = Optional.absent();
    }

    public NoteData(
            int position,
            int duration,
            String pitch,
            String lyric,
            Optional<String> trueLyric,
            Optional<EnvelopeData> envelope,
            Optional<PitchbendData> pitchbend) {
        this.position = position;
        this.duration = duration;
        this.pitch = pitch;
        this.lyric = lyric;
        this.trueLyric = trueLyric;
        this.envelope = envelope;
        this.pitchbend = pitchbend;
    }

    public NoteData(int position, int duration, String pitch, String lyric, EnvelopeData envelope) {
        this.position = position;
        this.duration = duration;
        this.pitch = pitch;
        this.lyric = lyric;
        this.trueLyric = Optional.absent();
        this.envelope = Optional.of(envelope);
        this.pitchbend = Optional.absent();
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
        this.trueLyric = Optional.absent();
        this.envelope = Optional.absent();
        this.pitchbend = Optional.of(pitchbend);
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

    public Optional<String> getTrueLyric() {
        return this.trueLyric;
    }

    public Optional<EnvelopeData> getEnvelope() {
        return this.envelope;
    }

    public Optional<PitchbendData> getPitchbend() {
        return this.pitchbend;
    }
}
