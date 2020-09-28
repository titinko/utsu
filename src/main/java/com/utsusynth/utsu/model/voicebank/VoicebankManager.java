package com.utsusynth.utsu.model.voicebank;

import com.utsusynth.utsu.common.exception.ErrorLogger;
import com.utsusynth.utsu.common.exception.FileAlreadyOpenException;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Manages all voicebanks in use by Utsu. This class is a singleton to ensure the same voicebank
 * does not open on two editors.
 */
public class VoicebankManager {
    private static final ErrorLogger errorLogger = ErrorLogger.getLogger();

    private final Map<File, Voicebank> voicebanks;
    private final Set<File> openForEdit;

    public VoicebankManager() {
        voicebanks = new HashMap<>();
        openForEdit = new HashSet<>();
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

    public void openVoicebankForEdit(File location) throws FileAlreadyOpenException {
        File normalized = normalize(location);
        if (openForEdit.contains(normalized)) {
            // No two tabs should point at the same file, to prevent headaches.
            throw new FileAlreadyOpenException(normalized);
        }
        openForEdit.add(normalized);
    }

    public void removeVoicebank(File location) {
        File normalized = normalize(location);
        voicebanks.remove(normalized);
        openForEdit.remove(normalized);
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
