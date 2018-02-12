package com.utsusynth.utsu.model.voicebank;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * A map of lyric to LyricConfig where the values can be retrieved at any time in sorted order.
 */
public class LyricConfigMap {
    public static final String MAIN_CATEGORY = "Main";

    private final SortedMap<String, SortedSet<LyricConfig>> configSets;
    private final Map<String, LyricConfig> configMap;

    public LyricConfigMap() {
        configSets = new TreeMap<>();
        configMap = new HashMap<>();
    }

    public boolean hasLyric(String lyric) {
        return configMap.containsKey(lyric);
    }

    public LyricConfig getConfig(String lyric) {
        return configMap.get(lyric);
    }

    public Set<String> getCategories() {
        return configSets.keySet();
    }

    public Iterator<LyricConfig> getConfigs(String category) {
        if (configSets.containsKey(category)) {
            return configSets.get(category).iterator();
        }
        return new TreeSet<LyricConfig>().iterator();
    }

    /**
     * Adds a lyric config if configMap doesn't have a config for that lyric already.
     * 
     * @return whether a config was added.
     */
    public boolean addConfig(LyricConfig config) {
        if (configMap.containsKey(config.getTrueLyric())) {
            return false;
        }

        // Add category if it doesn't already exist.
        String category = getCategory(config);
        if (!configSets.containsKey(category)) {
            configSets.put(category, new TreeSet<>());
        }
        configSets.get(category).add(config);
        configMap.put(config.getTrueLyric(), config);
        return true;
    }

    /**
     * Adds a lyric config, overwriting any existing ones with the same lyric.
     */
    public void setConfig(LyricConfig config) {
        if (configMap.containsKey(config.getTrueLyric())) {
            LyricConfig oldConfig = configMap.get(config.getTrueLyric());
            String oldCategory = getCategory(oldConfig);
            if (configSets.containsKey(oldCategory)) {
                configSets.get(oldCategory).remove(oldConfig);
            }
        }

        // Add category if it doesn't already exist.
        String category = getCategory(config);
        if (!configSets.containsKey(category)) {
            configSets.put(category, new TreeSet<>());
        }
        configSets.get(category).add(config);
        configMap.put(config.getTrueLyric(), config);
    }

    public void removeConfig(String lyric) {
        if (configMap.containsKey(lyric)) {
            LyricConfig toRemove = configMap.get(lyric);
            String category = getCategory(toRemove);
            if (configSets.containsKey(category)) {
                configSets.get(category).remove(toRemove);
            }
        }
        configMap.remove(lyric);
    }

    private static String getCategory(LyricConfig config) {
        String category = new File(config.getFilename()).getParent();
        if (category == null) {
            category = MAIN_CATEGORY;
        }
        return category;
    }
}
