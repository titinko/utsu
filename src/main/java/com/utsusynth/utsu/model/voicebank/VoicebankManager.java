package com.utsusynth.utsu.model.voicebank;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.google.inject.Inject;
import com.utsusynth.utsu.common.exception.ErrorLogger;
import com.utsusynth.utsu.files.VoicebankFileManager;
import com.utsusynth.utsu.files.VoicebankReader;

/**
 * Manages all voicebanks in use by Utsu. This class is a singleton to ensure the same voicebank
 * does not open on two editors.
 */
public class VoicebankManager {
    private static final ErrorLogger errorLogger = ErrorLogger.getLogger();
    public static final String DEFAULT_VOICE_PATH = "assets/voice";

    private final Map<File, Voicebank> voicebanks;

    @Inject
    public VoicebankManager(VoicebankReader voicebankReader) {
        voicebanks = new HashMap<>();
        loadDefaultBanks(voicebankReader);
    }

    private void loadDefaultBanks(VoicebankReader voicebankReader) {
        ArrayList<File> dirs = new VoicebankFileManager().getVoiceBankDirs(new File(DEFAULT_VOICE_PATH));
        dirs.forEach(d -> {
            Voicebank bank = voicebankReader.loadVoicebankFromDirectory(d);
            setVoicebank(normalize(d), bank);
        });
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
