package com.utsusynth.utsu;

import com.google.common.collect.ImmutableList;
import com.google.inject.*;
import com.utsusynth.utsu.common.StatusBar;
import com.utsusynth.utsu.common.i18n.Localizer;
import com.utsusynth.utsu.common.i18n.NativeLocale;
import com.utsusynth.utsu.common.quantize.Quantizer;
import com.utsusynth.utsu.common.quantize.Scaler;
import com.utsusynth.utsu.controller.common.IconManager;
import com.utsusynth.utsu.engine.*;
import com.utsusynth.utsu.files.VoicebankReader;
import com.utsusynth.utsu.model.voicebank.Voicebank;
import javafx.fxml.FXMLLoader;

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
    @interface AssetPath {
    }

    @Override
    protected void configure() {
        bind(StatusBar.class).asEagerSingleton();
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
    @AssetPath
    private File provideAssetPath() {
        if (new File("./assets").exists()) {
            return new File("./assets");
        }
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return new File("/Program Files/Utsu/app/assets");
        } else if (os.contains("mac")) {
            return new File("/Applications/Utsu.app/Contents/app/assets");
        } else {
            return new File("/opt/Utsu/app/assets");
        }
    }

    @Provides
    @Singleton
    private IconManager provideIconManager(@AssetPath File assetPath) {
        return new IconManager(
                "/icons/Rewind.png",
                "/icons/RewindPressed.png",
                "/icons/Play.png",
                "/icons/PlayPressed.png",
                "/icons/Pause.png",
                "/icons/PausePressed.png",
                "/icons/Stop.png",
                "/icons/StopPressed.png");
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
                new NativeLocale(new Locale("pt", "BR")));
        return new Localizer(defaultLocale, allLocales);
    }

    @Provides
    private Engine provideEngine(Resampler resampler, Wavtool wavtool, StatusBar statusBar, @AssetPath File assetPath) {
        String os = System.getProperty("os.name").toLowerCase();
        File resamplerPath;
        File wavtoolPath;
        if (os.contains("win")) {
            resamplerPath = new File(assetPath, "win64/macres.exe");
            wavtoolPath = new File(assetPath, "win64/wavtool-yawu.exe");
        } else if (os.contains("mac")) {
            resamplerPath = new File(assetPath, "Mac/macres");
            wavtoolPath = new File(assetPath, "Mac/wavtool-yawu");
        } else {
            resamplerPath = new File(assetPath, "linux64/macres");
            wavtoolPath = new File(assetPath, "linux64/wavtool-yawu");
        }
        return new Engine(
                resampler,
                wavtool,
                statusBar,
                /* threadPoolSize= */ 10,
                resamplerPath,
                wavtoolPath);
    }

    @Provides
    @Singleton
    private FrqGenerator provideFrqGenerator(ExternalProcessRunner runner, @AssetPath File assetPath) {
        String os = System.getProperty("os.name").toLowerCase();
        File frqGeneratorPath;
        if (os.contains("win")) {
            frqGeneratorPath = new File(assetPath, "win64/frq0003gen.exe");
        } else if (os.contains("mac")) {
            frqGeneratorPath = new File(assetPath, "Mac/frq0003gen");
        } else {
            frqGeneratorPath = new File(assetPath, "linux64/frq0003gen");
        }
        return new FrqGenerator(runner, frqGeneratorPath, 256);
    }

    @Provides
    @Singleton
    private Quantizer provideQuantizer() {
        return new Quantizer(4);
    }

    @Provides
    @Singleton
    private Scaler provideScaler() {
        return new Scaler(2, 0);
    }

    @Provides
    @Singleton
    private VoicebankReader provideVoicebankReader(Provider<Voicebank> voicebankProvider, @AssetPath File assetPath) {
        return new VoicebankReader(
                new File(assetPath, "voice/Iona_Beta/"),
                new File(assetPath, "config/lyric_conversions.txt"),
                voicebankProvider);
    }
}
