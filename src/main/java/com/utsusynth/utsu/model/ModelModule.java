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
            FrqGenerator frqGen,
            PresampConfig presampConfig) {
        return new Voicebank(configMap, pitchMap, new HashSet<>(), frqGen, presampConfig);
    }

    @Provides
    private PresampConfig provideEmptyPresampConfig(DisjointLyricSet conversionSet) {
        return new PresampConfig(
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>(),
                new HashSet<>(),
                conversionSet,
                new HashMap<>(),
                new HashSet<>(),
                new HashMap<>(),
                new HashSet<>(),
                new HashSet<>());
    }
}
