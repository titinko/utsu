package com.utsusynth.utsu.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.utsusynth.utsu.engine.FrqGenerator;
import com.utsusynth.utsu.files.CacheManager;
import com.utsusynth.utsu.model.song.NoteList;
import com.utsusynth.utsu.model.song.NoteStandardizer;
import com.utsusynth.utsu.model.song.Song;
import com.utsusynth.utsu.model.song.SongManager;
import com.utsusynth.utsu.model.song.pitch.PitchCurve;
import com.utsusynth.utsu.model.song.pitch.portamento.PortamentoFactory;
import com.utsusynth.utsu.model.voicebank.*;
import com.utsusynth.utsu.model.voicebank.PresampConfig.AliasType;

public class ModelModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(PortamentoFactory.class).asEagerSingleton();
        bind(SongManager.class).asEagerSingleton();
        bind(VoicebankManager.class).asEagerSingleton();
    }

    @Provides
    private Song provideEmptySong(
            VoicebankContainer voicebankContainer,
            NoteStandardizer noteStandardizer,
            CacheManager cacheManager,
            NoteList noteList,
            PitchCurve pitchCurve) {
        return new Song(voicebankContainer, noteStandardizer, cacheManager, noteList, pitchCurve);
    }

    @Provides
    private Voicebank provideEmptyVoicebank(
            LyricConfigMap configMap,
            PitchMap pitchMap,
            DisjointLyricSet conversionSet,
            FrqGenerator frqGen) {
        return new Voicebank(configMap, pitchMap, conversionSet, new HashSet<>(), frqGen);
    }

    @Provides
    private PresampConfig provideEmptyPresampConfig() {
        Map<AliasType, String[]> formats = new HashMap<>();
        formats.put(AliasType.VCV, new String[] {"%v%%VCVPAD%%CV%"}); // "a ka"
        formats.put(AliasType.BEGINNING_CV, new String[] {"-%VCVPAD%%CV%"}); // "- ka"
        formats.put(AliasType.CROSS_CV, new String[] {"*%VCVPAD%%CV%"}); // "* ka"
        formats.put(AliasType.VC, new String[] {"%v%%vcpad%%c%", "%c%%vcpad%%c%"}); // "a k", "k k"
        formats.put(AliasType.CV, new String[] {"%CV%", "%c%%V%"}); // "ka", "ka"
        formats.put(AliasType.C, new String[] {"%c%"}); // "k"
        formats.put(AliasType.LONG_V, new String[] {"%V%-"}); // "a-"
        formats.put(AliasType.VCPAD, new String[] {" "});
        formats.put(AliasType.VCVPAD, new String[] {" "});
        formats.put(AliasType.ENDING_1, new String[] {"%v%%VCPAD%R"}); // "a R"
        formats.put(AliasType.ENDING_2, new String[] {"-"});
        return new PresampConfig(
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>(),
                new HashSet<>(),
                new HashSet<>(),
                formats,
                new HashSet<>(),
                new HashMap<>(),
                new HashSet<>(),
                new HashSet<>());
    }
}
