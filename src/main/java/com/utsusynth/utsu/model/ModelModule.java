package com.utsusynth.utsu.model;

import java.io.File;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.utsusynth.utsu.model.song.NoteList;
import com.utsusynth.utsu.model.song.NoteStandardizer;
import com.utsusynth.utsu.model.song.Song;
import com.utsusynth.utsu.model.song.pitch.PitchCurve;
import com.utsusynth.utsu.model.song.pitch.portamento.PortamentoFactory;
import com.utsusynth.utsu.model.voicebank.VoicebankReader;

public class ModelModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(PortamentoFactory.class).asEagerSingleton();
    }

    @Provides
    private Song provideSong(
            VoicebankReader voicebankReader,
            NoteStandardizer noteStandardizer,
            NoteList noteList,
            PitchCurve pitchCurve) {
        return new Song(voicebankReader, noteStandardizer, noteList, pitchCurve);
    }

    @Provides
    @Singleton
    private VoicebankReader provideVoicebankReadera() {
        return new VoicebankReader(
                new File("./assets/voice/Iona_Beta/"),
                new File("./assets/config/lyric_conversions.txt"));
    }
}
