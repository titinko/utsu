package com.utsusynth.utsu;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.*;
import com.utsusynth.utsu.common.StatusBar;
import com.utsusynth.utsu.common.data.EnvelopeData;
import com.utsusynth.utsu.common.data.PitchbendData;
import com.utsusynth.utsu.common.i18n.Localizer;
import com.utsusynth.utsu.common.i18n.NativeLocale;
import com.utsusynth.utsu.common.quantize.DiscreteScaler;
import com.utsusynth.utsu.common.quantize.Quantizer;
import com.utsusynth.utsu.common.quantize.Scaler;
import com.utsusynth.utsu.controller.common.IconManager;
import com.utsusynth.utsu.engine.*;
import com.utsusynth.utsu.engine.wavtool.UtsuWavtool;
import com.utsusynth.utsu.files.*;
import com.utsusynth.utsu.files.voicebank.VoicebankReader;
import com.utsusynth.utsu.model.song.converters.ReclistConverter;
import com.utsusynth.utsu.model.song.converters.ReclistConverterMap;
import com.utsusynth.utsu.model.song.converters.jp.JpCvToJpCvvcConverter;
import com.utsusynth.utsu.model.song.converters.jp.JpCvToJpVcvConverter;
import com.utsusynth.utsu.model.song.converters.jp.JpCvvcToJpCvConverter;
import com.utsusynth.utsu.model.song.converters.jp.JpVcvToJpCvConverter;
import javafx.fxml.FXMLLoader;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import java.io.File;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Locale;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;

public class UtsuModule extends AbstractModule {
    @BindingAnnotation
    @Target({PARAMETER, METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @interface Version {
    }

    @BindingAnnotation
    @Target({PARAMETER, METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DefaultLyric {
    }

    @BindingAnnotation
    @Target({PARAMETER, METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface SettingsPath {
    }

    @BindingAnnotation
    @Target({PARAMETER, METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ReclistConverters {
    }

    @Override
    protected void configure() {
        bind(StatusBar.class).asEagerSingleton();
        bind(AssetManager.class).asEagerSingleton();
        bind(CacheManager.class).asEagerSingleton();
        bind(FileNameFixer.class).asEagerSingleton();
        bind(IconManager.class).asEagerSingleton();
        bind(VoicebankReader.class).asEagerSingleton();
        bind(ReclistConverterMap.class).asEagerSingleton();
        bind(Scaler.class).to(DiscreteScaler.class);
    }

    @Provides
    private FXMLLoader provideFXMLLoader(final Injector injector) {
        FXMLLoader loader = new FXMLLoader();
        loader.setControllerFactory(injector::getInstance);
        return loader;
    }

    @Provides
    @Version
    private String provideSettingsVersion() {
        return ("0.5");
    }

    @Provides
    @DefaultLyric
    private String provideDefaultLyric() {
        return "mi";
    }

    @Provides
    @SettingsPath
    private File provideSettingsPath(@Version String curVersion) {
        File homePath = new File(System.getProperty("user.home"));
        return new File(homePath, ".utsu/" + curVersion);
    }

    @Provides
    @ReclistConverters
    private ImmutableSet<ReclistConverter> provideReclistConverters() {
        return ImmutableSet.of(
                new JpCvToJpCvvcConverter(),
                new JpCvToJpVcvConverter(),
                new JpCvvcToJpCvConverter(),
                new JpVcvToJpCvConverter()
        );
    }

    @Provides
    @Singleton
    private ThemeManager provideThemeManager(@SettingsPath File settingsPath) {
        return new ThemeManager(
                settingsPath,
                "/css/css_template.txt",
                "/css/themes/");
    }

    @Provides
    @Singleton
    private DocumentBuilderFactory providdeDocumentBuilderFactory() {
        return DocumentBuilderFactory.newDefaultInstance();
    }

    @Provides
    @Singleton
    private PreferencesManager providePreferencesManager(
            @SettingsPath File settingsPath,
            DocumentBuilderFactory documentBuilderFactory,
            AssetManager assetManager) {
        ImmutableMap.Builder<String, String> defaultBuilder = ImmutableMap.builder();
        defaultBuilder.put("theme", ThemeManager.DEFAULT_LIGHT_THEME);
        defaultBuilder.put("autoscroll", PreferencesManager.AutoscrollMode.ENABLED_END.name());
        defaultBuilder.put(
                "autoscrollCancel", PreferencesManager.AutoscrollCancelMode.ENABLED.name());
        defaultBuilder.put(
                "playPianoNotes", PreferencesManager.PlayPianoNotesMode.ENABLED_HALF.name());
        defaultBuilder.put("guessAlias", PreferencesManager.GuessAliasMode.ENABLED.name());
        defaultBuilder.put("showVoicebankFace", "true");
        defaultBuilder.put("showVoicebankBody", "true");
        defaultBuilder.put("locale", "en");
        defaultBuilder.put("cache", PreferencesManager.CacheMode.ENABLED.name());
        defaultBuilder.put("resampler", assetManager.getResamplerFile().getAbsolutePath());
        defaultBuilder.put("wavtool", assetManager.getWavtoolFile().getAbsolutePath());
        defaultBuilder.put("voicebank", assetManager.getVoicePath().getAbsolutePath());
        defaultBuilder.put("metronome", assetManager.getMetronomeFile().getAbsolutePath());
        return new PreferencesManager(
                settingsPath,
                documentBuilderFactory,
                TransformerFactory.newDefaultInstance(),
                defaultBuilder.build()
        );
    }

    @Provides
    @Singleton
    private BulkEditorConfigManager provideBulkEditorConfigManager(
            @SettingsPath File settingsPath) {
        ImmutableList<Double> pbs = ImmutableList.of(-40.0, 0.0);
        ImmutableList<Double> pbw = ImmutableList.of(80.0);
        ImmutableList<Double> pby = ImmutableList.of();
        ImmutableList<String> pbm = ImmutableList.of();
        PitchbendData defaultPitchbend = new PitchbendData(pbs, pbw, pby, pbm);
        double[] envWidths = new double[] {200, 1, 1, 100, 1}; // Large fade in/out for visibility.
        double[] envHeights = new double[] {100, 100, 100, 100, 100};
        EnvelopeData defaultEnvelope = new EnvelopeData(envWidths, envHeights);
        return new BulkEditorConfigManager(settingsPath, defaultPitchbend, defaultEnvelope);
    }

    @Provides
    @Singleton
    private LyricEditorConfigManager provideLyricEditorConfigManager(
            @SettingsPath File settingsPath) {
        ImmutableList<String> defaultPrefixSuffixList = ImmutableList.of(
                "Custom",
                "C1-B7",
                "↑",
                "↓");
        return new LyricEditorConfigManager(settingsPath, defaultPrefixSuffixList);
    }

    @Provides
    @Singleton
    private Localizer provideLocalizer() {
        NativeLocale defaultLocale = new NativeLocale(new Locale("en"));
        ImmutableList<NativeLocale> allLocales = ImmutableList.of(
                defaultLocale,
                new NativeLocale(new Locale("ja")),
                new NativeLocale(new Locale("es")),
                new NativeLocale(new Locale("it")),
                new NativeLocale(new Locale("in")),
                new NativeLocale(new Locale("fr", "FR")),
                new NativeLocale(new Locale("zh", "CN")),
                new NativeLocale(new Locale("zh", "TW")),
                new NativeLocale(new Locale("pt", "BR")),
                new NativeLocale(new Locale("ru")),
                new NativeLocale(new Locale("hy")),
                new NativeLocale(new Locale("nl")),
                new NativeLocale(new Locale("ko")),
                new NativeLocale(new Locale("cs")));
        return new Localizer(defaultLocale, allLocales);
    }

    @Provides
    private Engine provideEngine(
            Resampler resampler,
            ExternalWavtool externalWavtool,
            UtsuWavtool utsuWavtool,
            StatusBar statusBar,
            CacheManager cacheManager,
            PreferencesManager preferencesManager) {
        return new Engine(
                resampler,
                externalWavtool,
                utsuWavtool,
                statusBar,
                /* threadPoolSize= */ 10,
                cacheManager,
                preferencesManager);
    }

    @Provides
    @Singleton
    private FrqGenerator provideFrqGenerator(
            ExternalProcessRunner runner, FileNameFixer fileNameFixer, AssetManager assetManager) {
        return new FrqGenerator(runner, fileNameFixer, assetManager, 256);
    }

    @Provides
    @Singleton
    private Quantizer provideQuantizer() {
        return new Quantizer(120);
    }

    @Provides
    @Singleton
    private DiscreteScaler provideDiscreteScaler() {
        return new DiscreteScaler(2, 0);
    }
}
