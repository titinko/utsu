package com.utsusynth.utsu.common.data;

public class NoteConfigData {
    private final String trueLyric;
    private final double velocity; // Consonant velocity.
    private final int intensity;
    private final String noteFlags;

    public NoteConfigData(String trueLyric, double velocity, int intensity, String noteFlags) {
        this.trueLyric = trueLyric;
        this.velocity = velocity;
        this.intensity = intensity;
        this.noteFlags = noteFlags;
    }

    public String getTrueLyric() {
        return this.trueLyric;
    }

    public double getVelocity() {
        return this.velocity;
    }

    public int getIntensity() {
        return this.intensity;
    }

    public String getNoteFlags() {
        return this.noteFlags;
    }
}
