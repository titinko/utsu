package com.utsusynth.utsu.files;

import com.utsusynth.utsu.UtsuModule.SettingsPath;
import com.utsusynth.utsu.common.exception.ErrorLogger;
import com.utsusynth.utsu.common.utils.RoundUtils;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ThemeManager {
    private static final ErrorLogger errorLogger = ErrorLogger.getLogger();

    public static final String DEFAULT_LIGHT_THEME = "light_theme.txt";
    public static final String DEFAULT_DARK_THEME = "dark_theme.txt";

    private final File generatedCss;
    private final String templateSource;
    private final String defaultThemeSource;
    private final StringProperty currentTheme;

    private String templateData;
    private Map<String, Color> defaultThemeMap;

    @Inject
    public ThemeManager(
            @SettingsPath File settingsPath,
            String templateSource,
            String defaultThemeSource) {
        generatedCss = new File(settingsPath, "generated.css");
        this.templateSource = templateSource;
        this.defaultThemeSource = defaultThemeSource;
        currentTheme = new SimpleStringProperty(DEFAULT_LIGHT_THEME);
    }

    // Initializes class and creates default theme.
    public void initialize(Scene scene) throws IOException {
        templateData = IOUtils.toString(
                getClass().getResource(templateSource), StandardCharsets.UTF_8);
        String lightThemeSource = defaultThemeSource + DEFAULT_LIGHT_THEME;
        defaultThemeMap = parseTheme(
                IOUtils.toString(getClass().getResource(lightThemeSource), StandardCharsets.UTF_8));
        currentTheme.set(DEFAULT_LIGHT_THEME);
        applyCurrentTheme();
        String css = "file:///" + generatedCss.getAbsolutePath().replace("\\", "/");
        scene.getStylesheets().add(css);
        addSceneListener(scene);
    }

    public StringProperty getCurrentTheme() {
        return currentTheme;
    }

    private void addSceneListener(Scene scene) {
        currentTheme.addListener((oldTheme, newTheme, obs) -> {
            if (!oldTheme.equals(newTheme)) {
                try {
                    applyCurrentTheme();
                } catch (Exception e) {
                    errorLogger.logError(e);
                    return;
                }
                String css = "file:///" + generatedCss.getAbsolutePath().replace("\\", "/");
                if (scene.getStylesheets().size() > 0) {
                    scene.getStylesheets().set(0, css);
                } else {
                    scene.getStylesheets().add(css);
                }
            }
        });
    }

    private void applyCurrentTheme() throws IOException {
        Map<String, Color> currentThemeMap;
        if (currentTheme.get().equals(DEFAULT_LIGHT_THEME)
                || currentTheme.get().equals(DEFAULT_DARK_THEME)) {
            String source = defaultThemeSource + currentTheme.get();
            currentThemeMap = parseTheme(
                    IOUtils.toString(getClass().getResource(source), StandardCharsets.UTF_8));
        } else {
            // TODO: Parse custom themes here.
            currentThemeMap = new HashMap<>();
        }
        String withCurrentTheme = findAndReplace(templateData, currentThemeMap);
        String withBackupTheme = findAndReplace(withCurrentTheme, defaultThemeMap); // Backup.
        FileUtils.writeStringToFile(generatedCss, withBackupTheme, StandardCharsets.UTF_8);
    }

    private Map<String, Color> parseTheme(String themeData) {
        Map<String, Color> themeMap = new HashMap<>();
        for (String themeLine : themeData.split("\n")) {
            if (!themeLine.contains("=")) {
                continue; // Assume this is not a config line.
            }
            String[] themeMapping = themeLine.trim().split("=");
            if (themeMapping.length != 2) {
                continue; // Assume this is not a config line.
            }
            if (themeMap.containsKey(themeMapping[0])) {
                System.out.println(
                        "Warning: Same mapping appeared twice in a theme: " + themeMapping[0]);
                continue;
            }
            try {
                Color color = Color.web(themeMapping[1]);
                themeMap.put(themeMapping[0], color);
            } catch (Exception e) {
                System.out.println("Warning: Could not parse theme color value.");
                errorLogger.logWarning(e);
            }
        }
        return themeMap;
    }

    private static String findAndReplace(String input, Map<String, Color> theme) {
        ArrayList<String> keyList = new ArrayList<>(theme.size());
        ArrayList<String> valueList = new ArrayList<>(theme.size());
        for (String key : theme.keySet()) {
            keyList.add("$[" + key + "]");
            valueList.add(toHexString(theme.get(key)));
        }
        String[] keys = keyList.toArray(new String[theme.size()]);
        String[] values = valueList.toArray(new String[theme.size()]);
        return StringUtils.replaceEach(input, keys, values);
    }

    // Removes floating point values before converting to hex.
    private static String formatHexString(double value) {
        String in = Integer.toHexString(RoundUtils.round(value * 255));
        return in.length() == 1 ? "0" + in : in;
    }

    // Convert Color to a CSS-readable string.
    public static String toHexString(Color color) {
        return "#"
                + (formatHexString(color.getRed())
                + formatHexString(color.getGreen())
                + formatHexString(color.getBlue())
                + formatHexString(color.getOpacity()))
                .toUpperCase();
    }
}
