package com.utsusynth.utsu.common.data;

import java.util.Optional;

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
    private final Optional<NoteConfigData> configData;

    public NoteData(int position, int duration, String pitch, String lyric) {
        this.position = position;
        this.duration = duration;
        this.pitch = pitch;
        this.lyric = lyric;
        this.trueLyric = Optional.empty();
        this.envelope = Optional.empty();
        this.pitchbend = Optional.empty();
        this.configData = Optional.empty();
    }

    public NoteData(
            int position,
            int duration,
            String pitch,
            String lyric,
            Optional<String> trueLyric,
            Optional<EnvelopeData> envelope,
            Optional<PitchbendData> pitchbend,
            Optional<NoteConfigData> configData) {
        this.position = position;
        this.duration = duration;
        this.pitch = pitch;
        this.lyric = lyric;
        this.trueLyric = trueLyric;
        this.envelope = envelope;
        this.pitchbend = pitchbend;
        this.configData = configData;
    }

    public NoteData(int position, int duration, String pitch, String lyric, EnvelopeData envelope) {
        this.position = position;
        this.duration = duration;
        this.pitch = pitch;
        this.lyric = lyric;
        this.trueLyric = Optional.empty();
        this.envelope = Optional.of(envelope);
        this.pitchbend = Optional.empty();
        this.configData = Optional.empty();
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
        this.trueLyric = Optional.empty();
        this.envelope = Optional.empty();
        this.pitchbend = Optional.of(pitchbend);
        this.configData = Optional.empty();
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

    public Optional<NoteConfigData> getConfigData() {
        return this.configData;
    }

    public NoteData withNewLyric(String newLyric) {
        return new NoteData(
                position,
                duration,
                pitch,
                newLyric,
                Optional.empty(), // Clear true lyric.
                envelope,
                pitchbend,
                configData
        );
    }

    public NoteData withEnvelope(EnvelopeData envelope) {
        return new NoteData(
                position,
                duration,
                pitch,
                lyric,
                trueLyric,
                Optional.of(envelope),
                pitchbend,
                configData);
    }

    public NoteData withPitchbend(PitchbendData pitchbend) {
        return new NoteData(
                position,
                duration,
                pitch,
                lyric,
                trueLyric,
                envelope,
                Optional.of(pitchbend),
                configData);
    }

    public NoteData withConfigData(NoteConfigData configData) {
        return new NoteData(
                position,
                duration,
                pitch,
                lyric,
                trueLyric,
                envelope,
                pitchbend,
                Optional.of(configData));
    }
}
