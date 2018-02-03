package com.utsusynth.utsu.common.data;

import java.util.List;
import com.google.common.collect.ImmutableList;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class LyricConfigData {
    private final StringProperty lyric;
    private final StringProperty fileName;
    private final DoubleProperty offset; // Time in wav file before note starts, in ms.
    private final DoubleProperty consonant; // Time in wav file before consonant ends, in ms.
    private final DoubleProperty cutoff; // Time in wav file before note ends, in ms.
    private final DoubleProperty preutter; // Number of ms that go before note officially starts.
    private final DoubleProperty overlap; // Number of ms that overlap with previous note.

    public LyricConfigData(
            String lyric,
            String fileName,
            double offset,
            double consonant,
            double cutoff,
            double preutter,
            double overlap) {
        this.lyric = new SimpleStringProperty(lyric);
        this.fileName = new SimpleStringProperty(fileName);
        this.offset = new SimpleDoubleProperty(offset);
        this.consonant = new SimpleDoubleProperty(consonant);
        this.cutoff = new SimpleDoubleProperty(cutoff);
        this.preutter = new SimpleDoubleProperty(preutter);
        this.overlap = new SimpleDoubleProperty(overlap);
    }

    public String getLyric() {
        return lyric.get();
    }

    public StringProperty lyricProperty() {
        return lyric;
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

    public double[] getConfigValues() {
        return new double[] {offset.get(), consonant.get(), cutoff.get(), preutter.get(),
                overlap.get()};
    }

    /** Returns any properties that can mutate without changing lyric save/display location. */
    public List<Property<?>> mutableProperties() {
        return ImmutableList.of(offset, consonant, cutoff, preutter, overlap);
    }
}
