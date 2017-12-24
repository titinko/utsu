package com.utsusynth.utsu.common.data;

public class NoteConfigData {
    private final String trueLyric;
    private final double velocity; // Consonant velocity.
    private final double startPoint; // User-set start point of a note.
    private final int intensity;
    private final int modulation;
    private final String noteFlags;

    // The "true" preutterance and duration or a note depending on its config and oto.
    private final double realPreutter;
    private final double realDuration;

    public NoteConfigData(
            String trueLyric,
            double velocity,
            double startPoint,
            int intensity,
            int modulation,
            String noteFlags,
            double realPreutter,
            double realDuration) {
        this.trueLyric = trueLyric;
        this.velocity = velocity;
        this.startPoint = startPoint;
        this.intensity = intensity;
        this.modulation = modulation;
        this.noteFlags = noteFlags;
        this.realPreutter = realPreutter;
        this.realDuration = realDuration;
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

    public double getRealPreutter() {
        return this.realPreutter;
    }

    public double getRealDuration() {
        return this.realDuration;
    }
}
