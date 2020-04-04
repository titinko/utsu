package com.utsusynth.utsu.engine;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import com.google.inject.Provider;
import com.utsusynth.utsu.files.VoicebankReader;
import com.utsusynth.utsu.model.voicebank.DisjointLyricSet;
import com.utsusynth.utsu.model.voicebank.LyricConfigMap;
import com.utsusynth.utsu.model.voicebank.PitchMap;
import com.utsusynth.utsu.model.voicebank.Voicebank;
import com.utsusynth.utsu.model.voicebank.VoicebankContainer;
import com.utsusynth.utsu.model.voicebank.VoicebankManager;

public class EngineHelper {

    public static final String DEFAULT_VOICE_PATH = "assets/voice/Iona_Beta";

    public static VoicebankReader createVoicebankReader(ExternalProcessRunner runner, String voicePath) {

        LyricConfigMap lyricConfigs = new LyricConfigMap();
        PitchMap pitchMap = new PitchMap();
        DisjointLyricSet conversionSet = new DisjointLyricSet();
        Set<File> soundFiles = new HashSet<>();
        FrqGenerator frqGenerator = createFrqGenerator(runner);

        Provider<Voicebank> voicebankProvider = () -> new Voicebank(lyricConfigs, pitchMap, conversionSet, soundFiles, frqGenerator);

        File defaultVoicePath = new File(voicePath);
        File lyricConversionPath = new File("assets/config/lyric_conversions.txt");

        VoicebankReader voicebankReader = new VoicebankReader(defaultVoicePath, lyricConversionPath, voicebankProvider);
        
        return voicebankReader;
    }

    public static FrqGenerator createFrqGenerator(ExternalProcessRunner runner) {

        String os = System.getProperty("os.name").toLowerCase();
        String frqGeneratorPath;

        if (os.contains("win")) {
            frqGeneratorPath = "assets/win64/frq0003gen.exe";
        } else if (os.contains("mac")) {
            frqGeneratorPath = "assets/Mac/frq0003gen";
        } else {
            frqGeneratorPath = "assets/linux64/frq0003gen";
        }

        return new FrqGenerator(runner, new File(frqGeneratorPath), 256);
    }
    
    public static VoicebankContainer createVoicebankContainer(ExternalProcessRunner runner, String voicePath) {

        VoicebankReader voicebankReader = createVoicebankReader(runner, voicePath);
        VoicebankManager voicebankManager = new VoicebankManager();

        return new VoicebankContainer(voicebankManager, voicebankReader);
    }    
}