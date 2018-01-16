package com.utsusynth.utsu.model;

import com.google.common.base.Optional;
import com.utsusynth.utsu.model.voicebank.LyricConfig;
import com.utsusynth.utsu.model.voicebank.Voicebank;

/** Standardizes a song note and prepares it for rendering. */
public class SongNoteStandardizer {
    void standardize(
            Optional<SongNote> prev,
            SongNote note,
            Optional<SongNote> next,
            Voicebank voicebank) {
        double realPreutter = 0;
        double realDuration = note.getDuration();
        double realOverlap = 0;
        double autoStartPoint = 0;
        String trueLyric = "";
        Optional<LyricConfig> config = voicebank.getLyricConfig(note.getLyric());
        if (config.isPresent()) {
            // Cap the preutterance at start of prev note or start of track.
            realPreutter = Math.min(config.get().getPreutterance(), note.getDelta());
            realOverlap = config.get().getOverlap();

            // Check correction factor.
            if (prev.isPresent()) {
                double maxLength = note.getDelta() - (prev.get().getDuration() / 2);
                if (realPreutter - realOverlap > maxLength) {
                    double correctionFactor = maxLength / (realPreutter - realOverlap);
                    double oldPreutter = realPreutter;
                    realPreutter *= correctionFactor;
                    realOverlap *= correctionFactor;
                    autoStartPoint = oldPreutter - realPreutter;
                }
            }
            realDuration = getAdjustedLength(voicebank, note, realPreutter, next);

            // Case where there is an adjacent next node.
            if (next.isPresent() && areNotesTouching(note, next.get(), voicebank)) {
                note.setFadeOut(next.get().getFadeIn());
            } else {
                note.setFadeOut(Math.min(35, realDuration)); // Default fade out.
            }

            // Ensure that envelope length is not greater than note length, ignoring fade out.
            double[] envWidths = note.getEnvelope().getWidths();
            double envLength = realOverlap + envWidths[1] + envWidths[2] + envWidths[4];
            if (envLength > realDuration - envWidths[3]) {
                double shrinkFactor = (realDuration - envWidths[3]) / envLength;
                String[] fullEnvelope = note.getFullEnvelope();
                realOverlap *= shrinkFactor;
                fullEnvelope[1] = Double.toString(envWidths[1] * shrinkFactor);
                fullEnvelope[2] = Double.toString(envWidths[2] * shrinkFactor);
                fullEnvelope[9] = Double.toString(envWidths[4] * shrinkFactor);
                note.setEnvelope(fullEnvelope);
            }

            // Adjust the envelopes to match overlap.
            note.setFadeIn(realOverlap);

            trueLyric = config.get().getTrueLyric();
        }

        // Set overlap.
        note.setRealPreutter(realPreutter);
        note.setRealDuration(realDuration);
        note.setAutoStartPoint(autoStartPoint);
        note.setTrueLyric(trueLyric);

        // TODO: Enforce pitchbend size/location limits.
    }

    // Find length of a note taking into account preutterance and overlap, but not tempo.
    private double getAdjustedLength(
            Voicebank voicebank,
            SongNote cur,
            double realPreutterance,
            Optional<SongNote> next) {
        // Increase length by this note's preutterance.
        double noteLength = cur.getDuration() + realPreutterance;

        // Decrease length by next note's preutterance.
        if (!next.isPresent()) {
            return noteLength;
        }

        Optional<LyricConfig> nextConfig = voicebank.getLyricConfig(next.get().getLyric());
        if (!nextConfig.isPresent()) {
            // Ignore next note if it has an invalid lyric.
            return noteLength;
        }

        if (!areNotesTouching(cur, next.get(), voicebank)) {
            // Ignore next note if it doesn't touch current note.
            return noteLength;
        }

        double nextPreutter = next.get().getRealPreutter();
        double encroachingPreutter = nextPreutter + cur.getDuration() - cur.getLength();
        noteLength -= encroachingPreutter;

        // Increase length by next note's overlap.
        double nextOverlap = Math.min(nextConfig.get().getOverlap(), next.get().getFadeIn());
        double nextBoundedOverlap = Math.max(0, Math.min(nextOverlap, next.get().getDuration()));
        noteLength += nextBoundedOverlap;

        return noteLength;
    }

    private boolean areNotesTouching(SongNote note, SongNote nextNote, Voicebank voicebank) {
        if (!voicebank.getLyricConfig(note.getLyric()).isPresent()
                || !voicebank.getLyricConfig(nextNote.getLyric()).isPresent()) {
            return false;
        }

        // Expect next preutterance to be set.
        double preutterance = Math.min(nextNote.getRealPreutter(), note.getLength());
        if (preutterance + note.getDuration() < note.getLength()) {
            return false;
        }
        return true;
    }
}
