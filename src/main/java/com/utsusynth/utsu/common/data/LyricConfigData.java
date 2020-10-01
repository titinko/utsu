package com.utsusynth.utsu.common.data;

import com.google.common.collect.ImmutableList;
import javafx.beans.property.*;

import java.io.File;
import java.util.List;

/**
 * Data about a lyric config, meant to be used in a TableView.
 */
public class LyricConfigData {
    public enum FrqStatus {
        INVALID, LOADING, VALID
    }

    private final File pathToFile;
    private final String category; // Category for nested voicebanks, defaults to "Main"
    private final StringProperty lyric;
    private final StringProperty fileName;
    private final StringProperty frqStatus;
    private final DoubleProperty offset; // Time in wav file before note starts, in ms.
    private final DoubleProperty consonant; // Time in wav file before consonant ends, in ms.
    private final DoubleProperty cutoff; // Time in wav file before note ends, in ms.
    private final DoubleProperty preutter; // Number of ms that go before note officially starts.
    private final DoubleProperty overlap; // Number of ms that overlap with previous note.
    private final BooleanProperty enabled; // Whether this config has a unique lyric.

    public LyricConfigData(
            File pathToFile,
            String category,
            String lyric,
            String fileName,
            String frqStatus,
            double offset,
            double consonant,
            double cutoff,
            double preutter,
            double overlap) {
        this.pathToFile = pathToFile;
        this.category = category;
        this.lyric = new SimpleStringProperty(lyric);
        this.fileName = new SimpleStringProperty(fileName);
        this.frqStatus = new SimpleStringProperty(frqStatus.toString());
        this.offset = new SimpleDoubleProperty(offset);
        this.consonant = new SimpleDoubleProperty(consonant);
        this.cutoff = new SimpleDoubleProperty(cutoff);
        this.preutter = new SimpleDoubleProperty(preutter);
        this.overlap = new SimpleDoubleProperty(overlap);
        this.enabled = new SimpleBooleanProperty(true);
    }

    public File getPathToFile() {
        return pathToFile;
    }

    public String getCategory() {
        return category;
    }

    public String getLyric() {
        return lyric.get();
    }

    public StringProperty lyricProperty() {
        return lyric;
    }

    public void setFrqStatus(FrqStatus status) {
        frqStatus.set(status.toString());
    }

    public StringProperty frqStatusProperty() {
        return frqStatus;
    }

    public String getFileName() {
        return fileName.get();
    }

    public StringProperty fileNameProperty() {
        return fileName;
    }

    public DoubleProperty offsetProperty() {
        return offset;
    }

    public DoubleProperty consonantProperty() {
        return consonant;
    }

    public DoubleProperty cutoffProperty() {
        return cutoff;
    }

    public DoubleProperty preutterProperty() {
        return preutter;
    }

    public DoubleProperty overlapProperty() {
        return overlap;
    }

    public BooleanProperty enabledProperty() {
        return enabled;
    }

    public double[] getConfigValues() {
        return new double[]{offset.get(), consonant.get(), cutoff.get(), preutter.get(),
                overlap.get()};
    }

    /**
     * Quick setter for config values using the same format as getConfigValues.
     */
    public void setConfigValues(double[] configValues) {
        if (configValues.length != 5) {
            return;
        }
        offset.set(configValues[0]);
        consonant.set(configValues[1]);
        cutoff.set(configValues[2]);
        preutter.set(configValues[3]);
        overlap.set(configValues[4]);
    }

    /**
     * Returns any properties that can mutate without changing lyric save/display location.
     */
    public List<Property<?>> mutableProperties() {
        return ImmutableList.of(offset, consonant, cutoff, preutter, overlap);
    }

    public LyricConfigData deepCopy() {
        return new LyricConfigData(
                pathToFile,
                category,
                lyric.get(),
                fileName.get(),
                frqStatus.get(),
                offset.get(),
                consonant.get(),
                cutoff.get(),
                preutter.get(),
                overlap.get());
    }
}
