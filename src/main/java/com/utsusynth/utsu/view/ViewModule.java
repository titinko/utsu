package com.utsusynth.utsu.view;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.utsusynth.utsu.common.quantize.Scaler;
import com.utsusynth.utsu.view.song.note.Lyric;
import com.utsusynth.utsu.view.song.note.NoteFactory;
import com.utsusynth.utsu.view.song.note.envelope.EnvelopeFactory;
import com.utsusynth.utsu.view.song.note.portamento.CurveFactory;
import com.utsusynth.utsu.view.song.note.portamento.PortamentoFactory;
import com.utsusynth.utsu.view.voicebank.LyricConfigFactory;

public class ViewModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(NoteFactory.class).asEagerSingleton();
        bind(EnvelopeFactory.class).asEagerSingleton();
        bind(PortamentoFactory.class).asEagerSingleton();
        bind(CurveFactory.class).asEagerSingleton();
        bind(LyricConfigFactory.class).asEagerSingleton();
    }

    @Provides
    private Lyric provideLyric(Scaler scaler) {
        return new Lyric("mi", scaler);
    }
}
