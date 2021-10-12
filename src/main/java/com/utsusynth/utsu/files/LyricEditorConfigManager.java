package com.utsusynth.utsu.files;

import com.utsusynth.utsu.UtsuModule.SettingsPath;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class LyricEditorConfigManager {
    private final File configPath;
    private final File prefixSuffixConfigPath;
    private final List<String> defaultPrefixSuffixItems;

    public LyricEditorConfigManager(
            @SettingsPath File settingsPath, List<String> defaultPrefixSuffixItems) {
        configPath = new File(settingsPath, "config");
        prefixSuffixConfigPath = new File(configPath, "prefix_suffix_config.txt");
        this.defaultPrefixSuffixItems = defaultPrefixSuffixItems;
    }

    /** Returns number of provided prefix/suffix choices. These cannot be deleted. */
    public int getNumDefaultPrefixSuffix() {
        return defaultPrefixSuffixItems.size();
    }

    /** Returns an in-code representation of the prefix/suffix config file. */
    public ObservableList<String> getPrefixSuffixConfig() {
        ObservableList<String> prefixSuffixConfig = loadPrefixSuffix();
        prefixSuffixConfig.addListener((ListChangeListener<? super String>) change -> {
            while (change.next()) {
                if (change.wasAdded() || change.wasRemoved()) {
                    writePrefixSuffix(prefixSuffixConfig);
                }
            }
        });
        return prefixSuffixConfig;
    }

    private ObservableList<String> loadPrefixSuffix() {
        ObservableList<String> prefixSuffixList = FXCollections.observableArrayList();
        prefixSuffixList.addAll(defaultPrefixSuffixItems);
        if (!prefixSuffixConfigPath.canRead()) {
            return prefixSuffixList;
        }
        try {
            for (String line
                    : FileUtils.readLines(prefixSuffixConfigPath, StandardCharsets.UTF_8)) {
                // Do not trim as whitespace might be intentional.
                if (line.isEmpty()) {
                    System.out.println("Error: Empty prefix/suffix option saved.");
                    continue;
                }
                prefixSuffixList.add(line);
            }
        } catch (IOException e) {
            System.out.println("Error: Unable to read envelope config.");
        }
        return prefixSuffixList;
    }

    private void writePrefixSuffix(List<String> prefixSuffixList) {
        // Initialize config file.
        if (!configPath.exists() && !configPath.mkdirs()) {
            System.out.println("Error: Failed to create config path.");
            return;
        }
        int startPoint = getNumDefaultPrefixSuffix();
        try (PrintStream ps = new PrintStream(prefixSuffixConfigPath)) {
            // Only write non-default data to file.
            for (int i = startPoint; i < prefixSuffixList.size(); i++) {
                ps.println(prefixSuffixList.get(i));
            }
            ps.flush();
        } catch (FileNotFoundException e) {
            System.out.println("Error: Failed to create config file.");
        }
    }
}
