package com.utsusynth.utsu.model.voicebank;

import com.google.inject.Inject;
import com.utsusynth.utsu.common.exception.ErrorLogger;
import com.utsusynth.utsu.common.exception.FileAlreadyOpenException;
import com.utsusynth.utsu.files.voicebank.VoicebankReader;

import java.io.File;
import java.io.IOException;

/**
 * Manages a single voicebank and its save settings.
 */
public class VoicebankContainer {
    private static final ErrorLogger errorLogger = ErrorLogger.getLogger();

    private File normalizedLocation;

    private final VoicebankManager voicebankManager;
    private final VoicebankReader voicebankReader;

    @Inject
    public VoicebankContainer(VoicebankManager voicebankManager, VoicebankReader voicebankReader) {
        this.voicebankManager = voicebankManager;
        this.voicebankReader = voicebankReader;
        setVoicebankForRead(voicebankReader.getDefaultPath()); // Start with default voicebank.
    }

    public Voicebank get() {
        // Reloads voicebank from file if necessary.
        if (voicebankManager.hasVoicebank(normalizedLocation)) {
            return voicebankManager.getVoicebank(normalizedLocation);
        } else {
            Voicebank voicebank = voicebankReader.loadVoicebankFromDirectory(normalizedLocation);
            voicebankManager.setVoicebank(normalizedLocation, voicebank);
            return voicebank;
        }
    }

    public void mutate(Voicebank newVoicebank) {
        voicebankManager.setVoicebank(normalizedLocation, newVoicebank);
    }

    public void setVoicebankForRead(File newLocation) {
        normalizedLocation = normalize(newLocation);
    }

    public void setVoicebankForEdit(File newLocation) throws FileAlreadyOpenException {
        setVoicebankForRead(newLocation);
        voicebankManager.openVoicebankForEdit(normalizedLocation);
    }

    /**
     * Should only be called by voicebank editor.
     */
    public void removeVoicebankForEdit() {
        voicebankManager.removeVoicebank(normalizedLocation);
    }

    public File getLocation() {
        return normalizedLocation;
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
