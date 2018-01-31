package com.utsusynth.utsu.model.voicebank;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A map of lyric to LyricConfig where the values can be retrieved at any time in sorted order.
 */
public class LyricConfigMap {
    private final SortedSet<LyricConfig> configSet;
    private final Map<String, LyricConfig> configMap;

    public LyricConfigMap() {
        configSet = new TreeSet<>();
        configMap = new HashMap<>();
    }

    public boolean hasLyric(String lyric) {
        return configMap.containsKey(lyric);
    }

    public LyricConfig getConfig(String lyric) {
        return configMap.get(lyric);
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
        configSet.add(config);
        configMap.put(config.getTrueLyric(), config);
        return true;
    }

    /**
     * Adds a lyric config, overwriting any existing ones with the same lyric.
     */
    public void setConfig(LyricConfig config) {
        if (configMap.containsKey(config.getTrueLyric())) {
            configSet.remove(configMap.get(config.getTrueLyric()));
        }
        configSet.add(config);
        configMap.put(config.getTrueLyric(), config);
    }

    public void removeConfig(LyricConfig config) {
        configMap.remove(config.getTrueLyric());
        configSet.remove(config);
    }
}
