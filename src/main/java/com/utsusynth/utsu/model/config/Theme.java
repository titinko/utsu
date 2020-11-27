package com.utsusynth.utsu.model.config;

import com.google.common.collect.ImmutableMap;
import javafx.scene.paint.Color;

import java.util.Map;

public class Theme {
    private final String id;

    private String name;
    private Map<String, Color> colorMap;

    public Theme(String id) {
        this.id = id;
        this.name = id;
        this.colorMap = ImmutableMap.of();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Color> getColorMap() {
        return colorMap;
    }

    public void setColorMap(Map<String, Color> colorMap) {
        this.colorMap = colorMap;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Theme)) {
            return false;
        }
        Theme otherTheme = (Theme) other;
        return otherTheme.id.equals(this.id);
    }
}