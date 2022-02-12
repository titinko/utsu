package com.utsusynth.utsu.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import com.google.common.collect.ImmutableList;
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
            FrqGenerator frqGen,
            PresampConfig presampConfig) {
        return new Voicebank(
                configMap, pitchMap, conversionSet, new HashSet<>(), frqGen, presampConfig);
    }

    @Provides
    private PresampConfig provideEmptyPresampConfig() {
        Map<AliasType, ImmutableList<String>> formats = new HashMap<>();
        formats.put(AliasType.VCV, ImmutableList.of("%v%%VCVPAD%%CV%")); // "a ka"
        formats.put(AliasType.BEGINNING_CV, ImmutableList.of("-%VCVPAD%%CV%")); // "- ka"
        formats.put(AliasType.CROSS_CV, ImmutableList.of("*%VCVPAD%%CV%")); // "* ka"
        formats.put(
                AliasType.VC, ImmutableList.of("%v%%vcpad%%c%", "%c%%vcpad%%c%")); // "a k", "k k"
        formats.put(AliasType.CV, ImmutableList.of("%CV%", "%c%%V%")); // "ka", "ka"
        formats.put(AliasType.C, ImmutableList.of("%c%")); // "k"
        formats.put(AliasType.LONG_V, ImmutableList.of("%V%-")); // "a-"
        formats.put(AliasType.VCPAD, ImmutableList.of(" "));
        formats.put(AliasType.VCVPAD, ImmutableList.of(" "));
        formats.put(AliasType.ENDING_1, ImmutableList.of("%v%%VCPAD%R")); // "a R"
        formats.put(AliasType.ENDING_2, ImmutableList.of("-"));
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
