package com.utsusynth.utsu.model.voicebank;

import java.io.File;
import com.google.inject.Inject;
import com.utsusynth.utsu.files.VoicebankReader;

/** Manages a single voicebank and its save settings. */
public class VoicebankContainer {
    private File location;

    private final VoicebankManager voicebankManager;
    private final VoicebankReader voicebankReader;

    @Inject
    public VoicebankContainer(VoicebankManager voicebankManager, VoicebankReader voicebankReader) {
        this.voicebankManager = voicebankManager;
        this.voicebankReader = voicebankReader;
        setVoicebank(voicebankReader.getDefaultPath()); // Start with default voicebank.
    }

    public Voicebank get() {
        return voicebankManager.getVoicebank(location);
    }

    public void mutate(Voicebank newVoicebank) {
        voicebankManager.setVoicebank(location, newVoicebank);
    }

    public Voicebank setVoicebank(File newLocation) {
        location = newLocation;
        if (voicebankManager.hasVoicebank(location)) {
            return voicebankManager.getVoicebank(location);
        } else {
            Voicebank voicebank = voicebankReader.loadVoicebankFromDirectory(newLocation);
            voicebankManager.setVoicebank(newLocation, voicebank);
            return voicebank;
        }
    }

    public File getLocation() {
        return location;
    }
}
