package com.utsusynth.utsu;

import java.io.File;
import java.util.Locale;
import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.utsusynth.utsu.common.StatusBar;
import com.utsusynth.utsu.common.i18n.Localizer;
import com.utsusynth.utsu.common.i18n.NativeLocale;
import com.utsusynth.utsu.common.quantize.Quantizer;
import com.utsusynth.utsu.common.quantize.Scaler;
import com.utsusynth.utsu.controller.IconManager;
import com.utsusynth.utsu.engine.Engine;
import com.utsusynth.utsu.engine.ExternalProcessRunner;
import com.utsusynth.utsu.engine.FrqGenerator;
import com.utsusynth.utsu.engine.Resampler;
import com.utsusynth.utsu.engine.Wavtool;
import javafx.fxml.FXMLLoader;

public class UtsuModule extends AbstractModule {

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
    @Singleton
    private IconManager provideIconManager() {
        return new IconManager(
                new File("assets/icons/Rewind.png"),
                new File("assets/icons/RewindPressed.png"),
                new File("assets/icons/Play.png"),
                new File("assets/icons/PlayPressed.png"),
                new File("assets/icons/Pause.png"),
                new File("assets/icons/PausePressed.png"),
                new File("assets/icons/Stop.png"),
                new File("assets/icons/StopPressed.png"));
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
                new NativeLocale(new Locale("zh", "CN")),
                new NativeLocale(new Locale("zh", "TW")),
                new NativeLocale(new Locale("pt", "BR")));
        return new Localizer(defaultLocale, allLocales);
    }

    @Provides
    private Engine provideEngine(Resampler resampler, Wavtool wavtool, StatusBar statusBar) {
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
        return new Engine(
                resampler,
                wavtool,
                statusBar,
                /* threadPoolSize= */ 10,
                resamplerFile,
                wavtoolFile);
    }

    @Provides
    @Singleton
    private FrqGenerator provideFrqGenerator(ExternalProcessRunner runner) {
        String os = System.getProperty("os.name").toLowerCase();
        String frqGeneratorPath;
        if (os.contains("win")) {
            frqGeneratorPath = "assets/win64/frq0003gen.exe";
        } else if (os.contains("mac")) {
            frqGeneratorPath = "assets/Mac/frq0003gen";
        } else {
            frqGeneratorPath = "assets/linux64/frq0003gen";
        }
        return new FrqGenerator(runner, new File(frqGeneratorPath), 256);
    }

    @Provides
    @Singleton
    private Quantizer provideQuantizer() {
        return new Quantizer(4);
    }

    @Provides
    @Singleton
    private Scaler provideScaler() {
        return new Scaler(0.2, 1.0);
    }
}
