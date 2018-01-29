package com.utsusynth.utsu.model.voicebank;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import com.utsusynth.utsu.common.exception.ErrorLogger;

/**
 * Manages all voicebanks in use by Utsu. This class is a singleton to ensure the same voicebank
 * does not open on two editors.
 */
public class VoicebankManager {
    private static final ErrorLogger errorLogger = ErrorLogger.getLogger();

    private final Map<File, Voicebank> voicebanks;

    public VoicebankManager() {
        voicebanks = new HashMap<>();
    }

    public boolean hasVoicebank(File location) {
        File normalized = normalize(location);
        return voicebanks.containsKey(normalized);
    }

    public Voicebank getVoicebank(File location) {
        File normalized = normalize(location);
        return voicebanks.get(normalized);
    }

    public void setVoicebank(File location, Voicebank voicebank) {
        File normalized = normalize(location);
        voicebanks.put(normalized, voicebank);
    }

    public void removeVoicebank(File location) {
        File normalized = normalize(location);
        voicebanks.remove(normalized);
    }

    private File normalize(File rawFile) {
        try {
            return rawFile.getCanonicalFile();
        } catch (IOException e) {
            // TODO: Handle this
            errorLogger.logError(e);
        }
        // Return raw file if it cannot be normalized.
        return rawFile;
    }
}
