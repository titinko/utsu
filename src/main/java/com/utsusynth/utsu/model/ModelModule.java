package com.utsusynth.utsu.model;

import java.io.File;
import java.util.HashMap;
import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.utsusynth.utsu.files.VoicebankReader;
import com.utsusynth.utsu.model.song.NoteList;
import com.utsusynth.utsu.model.song.NoteStandardizer;
import com.utsusynth.utsu.model.song.Song;
import com.utsusynth.utsu.model.song.SongManager;
import com.utsusynth.utsu.model.song.pitch.PitchCurve;
import com.utsusynth.utsu.model.song.pitch.portamento.PortamentoFactory;
import com.utsusynth.utsu.model.voicebank.DisjointLyricSet;
import com.utsusynth.utsu.model.voicebank.Voicebank;
import com.utsusynth.utsu.model.voicebank.VoicebankContainer;
import com.utsusynth.utsu.model.voicebank.VoicebankManager;

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
            NoteList noteList,
            PitchCurve pitchCurve) {
        return new Song(voicebankContainer, noteStandardizer, noteList, pitchCurve);
    }

    @Provides
    private Voicebank provideEmptyVoicebank(DisjointLyricSet conversionSet) {
        return new Voicebank(new HashMap<>(), new HashMap<>(), conversionSet);
    }

    @Provides
    @Singleton
    private VoicebankReader provideVoicebankReadera(Provider<Voicebank> voicebankProvider) {
        return new VoicebankReader(
                new File("./assets/voice/Iona_Beta/"),
                new File("./assets/config/lyric_conversions.txt"),
                voicebankProvider);
    }
}
