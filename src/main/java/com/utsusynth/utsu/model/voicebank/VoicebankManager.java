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
    private final Map<File, Voicebank> voicebanks;
    private final Set<File> openForEdit;

    public VoicebankManager() {
        voicebanks = new HashMap<>();
        openForEdit = new HashSet<>();
    }

    public boolean hasVoicebank(File location) {
        return voicebanks.containsKey(location);
    }

    public Voicebank getVoicebank(File location) {
        return voicebanks.get(location);
    }

    public void setVoicebank(File location, Voicebank voicebank) {
        voicebanks.put(location, voicebank);
    }

    public void openVoicebankForEdit(File location) throws FileAlreadyOpenException {
        if (openForEdit.contains(location)) {
            // No two tabs should point at the same file, to prevent headaches.
            throw new FileAlreadyOpenException(location);
        }
        openForEdit.add(location);
    }

    public void removeVoicebank(File location) {
        voicebanks.remove(location);
        openForEdit.remove(location);
    }
}
