package com.utsusynth.utsu.engine.wavtool;

import com.google.inject.Inject;
import com.google.inject.Provider;
import javafx.util.StringConverter;

import java.io.File;

/** Converts a wavtool to and from a String. Useful for saving to file. */
public class WavtoolConverter extends StringConverter<Wavtool> {
    private final Wavtool defaultWavtool;
    private final Provider<ExternalWavtool> externalWavtoolProvider;

    @Inject
    public WavtoolConverter(
            Wavtool defaultWavtool, Provider<ExternalWavtool> externalWavtoolProvider) {
        this.defaultWavtool = defaultWavtool;
        this.externalWavtoolProvider = externalWavtoolProvider;
    }
    @Override
    public String toString(Wavtool wavtool) {
        return wavtool.toString();
    }

    @Override
    public Wavtool fromString(String str) {
        if (str.equals(defaultWavtool.toString())) {
            return defaultWavtool;
        }
        ExternalWavtool externalWavtool = externalWavtoolProvider.get();
        externalWavtool.setWavtoolPath(new File(str));
        return externalWavtool;
    }
}
