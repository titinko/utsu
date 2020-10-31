package com.utsusynth.utsu.model.config;

import static com.utsusynth.utsu.files.ThemeManager.DEFAULT_LIGHT_THEME;

import java.util.HashMap;

public class UserPreferences {
    HashMap<String, String> preferences = new HashMap<>();

    public String getTheme() {
        if (preferences.containsKey("theme")) {
            return preferences.get("theme");
        }
        return DEFAULT_LIGHT_THEME;
    }

    public void setTheme(String newTheme) {
        preferences.put("Theme", newTheme);
    }
}
