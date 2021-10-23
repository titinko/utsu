package com.utsusynth.utsu.common.data;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class PitchMapData {
    private final StringProperty pitch;
    private final StringProperty prefix;
    private final StringProperty suffix;

    public PitchMapData(String pitch, String prefix, String suffix) {
        this.pitch = new SimpleStringProperty(pitch);
        this.prefix = new SimpleStringProperty(prefix);
        this.suffix = new SimpleStringProperty(suffix);
    }

    public String getPitch() {
        return pitch.get();
    }

    public StringProperty pitchProperty() {
        return pitch;
    }

    public String getPrefix() {
        return prefix.get();
    }

    public StringProperty prefixProperty() {
        return prefix;
    }

    public String getSuffix() {
        return suffix.get();
    }

    public StringProperty suffixProperty() {
        return suffix;
    }
}
