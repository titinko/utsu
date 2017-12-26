package com.utsusynth.utsu.common.data;

public class NoteConfigData {
    private final String trueLyric;
    private final double velocity; // Consonant velocity.
    private final double startPoint; // User-set start point of a note.
    private final int intensity;
    private final int modulation;
    private final String noteFlags;

    public NoteConfigData(
            String trueLyric,
            double velocity,
            double startPoint,
            int intensity,
            int modulation,
            String noteFlags) {
        this.trueLyric = trueLyric;
        this.velocity = velocity;
        this.startPoint = startPoint;
        this.intensity = intensity;
        this.modulation = modulation;
        this.noteFlags = noteFlags;
    }

    public String getTrueLyric() {
        return this.trueLyric;
    }

    public double getVelocity() {
        return this.velocity;
    }

    public double getStartPoint() {
        return this.startPoint;
    }

    public int getIntensity() {
        return this.intensity;
    }

    public int getModulation() {
        return this.modulation;
    }

    public String getNoteFlags() {
        return this.noteFlags;
    }
}
