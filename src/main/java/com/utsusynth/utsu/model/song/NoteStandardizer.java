package com.utsusynth.utsu.model.song;

import com.utsusynth.utsu.common.quantize.Quantizer;
import com.utsusynth.utsu.common.utils.PitchUtils;
import com.utsusynth.utsu.model.voicebank.LyricConfig;
import com.utsusynth.utsu.model.voicebank.Voicebank;

import java.util.Optional;

/**
 * Standardizes a song note and prepares it for rendering.
 */
public class NoteStandardizer {
    // This function should be called in the order: last note -> first note
    void standardize(Optional<Note> prev, Note note, Optional<Note> next, Voicebank voicebank) {
        double consonantScaleFactor = Math.pow(2, 1 - (note.getVelocity() / 100.0));
        double realPreutter = 0;
        double realDuration = note.getDuration();
        double realOverlap;
        double realStartPoint = note.getStartPoint() * consonantScaleFactor;
        String trueLyric = "";

        // Find lyric config, applying auto-aliasing if necessary.
        String pitch = PitchUtils.noteNumToPitch(note.getNoteNum());
        String prevLyric = getNearbyPrevLyric(prev);
        Optional<LyricConfig> config = voicebank.getLyricConfig(prevLyric, note.getLyric(), pitch);

        if (config.isPresent()) {
            trueLyric = config.get().getTrueLyric();

            // Note preutter and overlap can override those in the config.
            realPreutter = (note.getPreutter().isPresent() ? note.getPreutter().get()
                    : config.get().getPreutterance()) * consonantScaleFactor;
            realOverlap = (note.getOverlap().isPresent() ? note.getOverlap().get()
                    : config.get().getOverlap()) * consonantScaleFactor;

            // Check correction factor.
            if (prev.isPresent()) {
                double maxLength = note.getDelta() - (prev.get().getDuration() / 2.0);
                // The original correction factor code.
                double comparator = (realPreutter - realOverlap);
                if (comparator > maxLength) {
                    double correctionFactor = maxLength / comparator;
                    double oldPreutter = realPreutter;
                    realPreutter *= correctionFactor;
                    realOverlap *= correctionFactor;
                    realStartPoint += oldPreutter - realPreutter;
                }
            }
            realDuration = getAdjustedLength(voicebank, note, trueLyric, realPreutter, next);

            // Case where there is an adjacent next node.
            if (next.isPresent() && areNotesTouching(note, trueLyric, next.get(), voicebank)) {
                if (next.get().getFadeIn() > realDuration) {
                    next.get().setFadeIn(realDuration); // Shrink next note's fade in if necessary.
                }
                note.setFadeOut(next.get().getFadeIn());
            } else {
                note.setFadeOut(Math.min(35, realDuration)); // Default fade out.
            }

            // Ensure that envelope length is not greater than note length, ignoring fade out.
            double[] envWidths = note.getEnvelope().getWidths();
            double envLength = realOverlap + envWidths[1] + envWidths[2] + envWidths[4];
            if (envLength > realDuration - envWidths[3]) {
                double shrinkFactor = Math.abs(realDuration - envWidths[3]) / envLength;
                String[] fullEnvelope = note.getFullEnvelope();
                realOverlap *= shrinkFactor;
                fullEnvelope[1] = Double.toString(envWidths[1] * shrinkFactor);
                fullEnvelope[2] = Double.toString(envWidths[2] * shrinkFactor);
                fullEnvelope[9] = Double.toString(envWidths[4] * shrinkFactor);
                note.setEnvelope(fullEnvelope);
             }

            // Adjust the envelopes to match overlap.
            note.setFadeIn(realOverlap);
        }

        // Set overlap.
        note.setRealPreutter(realPreutter);
        note.setRealDuration(realDuration);
        note.setRealStartPoint(realStartPoint);
        note.setTrueLyric(trueLyric);

        // TODO: Enforce pitchbend size/location limits.
    }

    // Returns empty string if there is no nearby (within DEFAULT_NOTE_DURATION) previous note.
    private static String getNearbyPrevLyric(Optional<Note> prev) {
        if (prev.isPresent() && prev.get().getLength()
                - prev.get().getDuration() < Quantizer.DEFAULT_NOTE_DURATION / 2) {
            return prev.get().getLyric();
        }
        return "";
    }

    // Find length of a note taking into account preutterance and overlap, but not tempo.
    private static double getAdjustedLength(
            Voicebank voicebank,
            Note cur,
            String trueLyric,
            double realPreutterance,
            Optional<Note> next) {
        // Increase length by this note's preutterance.
        double noteLength = cur.getDuration() + realPreutterance;

        // Decrease length by next note's preutterance.
        if (!next.isPresent()) {
            return noteLength;
        }

        Optional<LyricConfig> nextConfig = voicebank.getLyricConfig(next.get().getTrueLyric());
        if (next.get().getTrueLyric().isEmpty()) {
            // Ignore next note if it doesn't have a true lyric set.
            return noteLength;
        }

        if (!areNotesTouching(cur, trueLyric, next.get(), voicebank)) {
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

    private static boolean areNotesTouching(
            Note note,
            String trueLyric,
            Note nextNote,
            Voicebank voicebank) {
        // Confirm both notes can be rendered.
        if (!voicebank.getLyricConfig(trueLyric).isPresent()
                || !voicebank.getLyricConfig(nextNote.getTrueLyric()).isPresent()) {
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
