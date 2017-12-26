package com.utsusynth.utsu;

import java.io.File;
import java.util.Locale;
import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.utsusynth.utsu.common.i18n.Localizer;
import com.utsusynth.utsu.common.i18n.NativeLocale;
import com.utsusynth.utsu.common.quantize.Quantizer;
import com.utsusynth.utsu.common.quantize.Scaler;
import com.utsusynth.utsu.engine.Engine;
import com.utsusynth.utsu.engine.Resampler;
import com.utsusynth.utsu.engine.Wavtool;
import com.utsusynth.utsu.model.Song;
import com.utsusynth.utsu.model.SongManager;
import com.utsusynth.utsu.model.SongNoteList;
import com.utsusynth.utsu.model.SongNoteStandardizer;
import com.utsusynth.utsu.model.pitch.PitchCurve;
import com.utsusynth.utsu.model.pitch.portamento.PortamentoFactory;
import com.utsusynth.utsu.model.voicebank.VoicebankReader;
import com.utsusynth.utsu.view.note.TrackLyric;
import com.utsusynth.utsu.view.note.TrackNoteFactory;
import com.utsusynth.utsu.view.note.envelope.TrackEnvelopeFactory;
import com.utsusynth.utsu.view.note.portamento.CurveFactory;
import com.utsusynth.utsu.view.note.portamento.TrackPortamentoFactory;
import javafx.fxml.FXMLLoader;

public class UtsuModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(SongManager.class).asEagerSingleton();
        bind(PortamentoFactory.class).asEagerSingleton();
        bind(TrackNoteFactory.class).asEagerSingleton();
        bind(TrackEnvelopeFactory.class).asEagerSingleton();
        bind(TrackPortamentoFactory.class).asEagerSingleton();
        bind(CurveFactory.class).asEagerSingleton();
    }

    @Provides
    private FXMLLoader provideFXMLLoader(final Injector injector) {
        FXMLLoader loader = new FXMLLoader();
        loader.setControllerFactory(p -> {
            return injector.getInstance(p);
        });
        return loader;
    }

    @Provides
    private Song provideSong(
            VoicebankReader voicebankReader,
            SongNoteStandardizer noteStandardizer,
            SongNoteList noteList,
            PitchCurve pitchCurve) {
        return new Song(voicebankReader, noteStandardizer, noteList, pitchCurve);
    }

    @Provides
    private TrackLyric provideLyric(Scaler scaler) {
        return new TrackLyric("mi", scaler);
    }

    @Provides
    @Singleton
    private Localizer provideLocalizer() {
        NativeLocale defaultLocale = new NativeLocale(new Locale("en"));
        ImmutableList<NativeLocale> allLocales =
                ImmutableList.of(defaultLocale, new NativeLocale(new Locale("ja")));
        return new Localizer(defaultLocale, allLocales);
    }

    @Provides
    @Singleton
    private Engine engine(Resampler resampler, Wavtool wavtool) {
        String os = System.getProperty("os.name").toLowerCase();
        String resamplerPath;
        String wavtoolPath;
        if (os.contains("win")) {
            resamplerPath = "assets/win64/macres.exe";
            wavtoolPath = "assets/win64/wavtool-yawu.exe";
        } else if (os.contains("mac")) {
            resamplerPath = "assets/Mac/macres";
            wavtoolPath = "assets/Mac/wavtool-yawu";
        } else {
            resamplerPath = "assets/linux64/macres";
            wavtoolPath = "assets/linux64/wavtool-yawu";
        }
        File resamplerFile = new File(resamplerPath);
        File wavtoolFile = new File(wavtoolPath);
        return new Engine(resampler, wavtool, resamplerFile, wavtoolFile);
    }

    @Provides
    @Singleton
    private Quantizer provideQuantizer() {
        return new Quantizer(1, 96);
    }

    @Provides
    @Singleton
    private Scaler provideScaler() {
        return new Scaler(0.2, 1.0);
    }
}
