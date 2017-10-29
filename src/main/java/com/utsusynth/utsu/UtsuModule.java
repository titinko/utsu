package com.utsusynth.utsu;

import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.utsusynth.utsu.common.i18n.Localizer;
import com.utsusynth.utsu.common.i18n.NativeLocale;
import com.utsusynth.utsu.common.quantize.Quantizer;
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
import java.io.File;
import java.util.Locale;
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
	private TrackLyric provideLyric() {
		return new TrackLyric("mi");
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
		String path = "/Users/emmabreen/Documents/Playground/C++/";
		File resamplerPath = new File(path + "tn_fnds/macres");
		File wavtoolPath = new File(path + "wavtool-yawu/build/wavtool-yawu");
		return new Engine(resampler, wavtool, resamplerPath, wavtoolPath);
	}

	@Provides
	@Singleton
	private Quantizer provideQuantizer() {
		return new Quantizer(1);
	}
}
