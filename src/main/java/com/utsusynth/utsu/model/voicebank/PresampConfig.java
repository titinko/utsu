package com.utsusynth.utsu.model.voicebank;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration parsed from a voicebank's presamp.ini file, if present.
 */
public class PresampConfig {
    // If a lyric ends with the key, the value is the vowel used in the succeeding VC.
    private final Map<String, String> vowelMappings;
    // Key is a vowel, value is the volume of any note with that vowel.
    private final Map<String, Integer> vowelVolumes;
    // If a lyric is exactly the key, the value is the consonant used in the preceding VC.
    private final Map<String, String> consonantMappings;
    // Key is a consonant, value is whether that consonant should overlap preceding VC or skip it.
    // True -> Overlap with preceding VC. (default)
    // False -> Don't overlap preceding vc.
    private final Map<String, Boolean> consonantOverlaps;

    public static class Builder {
        private final PresampConfig newConfig;

        private Builder(PresampConfig newConfig) {
            this.newConfig = newConfig;
        }

        public void addVowelMapping(String key, String vowel) {
            newConfig.vowelMappings.put(key, vowel);
        }

        public void addVowelVolume(String vowel, Integer volume) {
            newConfig.vowelVolumes.put(vowel, volume);
        }

        public PresampConfig build() {
            return newConfig;
        }
    }

    public PresampConfig() {
        vowelMappings = new HashMap<>();
        vowelVolumes = new HashMap<>();
        consonantMappings = new HashMap<>();
        consonantOverlaps = new HashMap<>();
    }

    public Builder toBuilder() {
        // Returns the builder of a new PresampConfig with this one's attributes.
        // The old config's final fields are used--the objects are not regenerated.
        return new Builder(this);
    }
}
