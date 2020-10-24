package com.utsusynth.utsu.files;

import com.utsusynth.utsu.UtsuModule.SettingsPath;
import com.utsusynth.utsu.common.exception.ErrorLogger;
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
import java.util.Optional;

public class ThemeManager {
    private static final ErrorLogger errorLogger = ErrorLogger.getLogger();

    private final File generatedCss;
    private final String templateSource;
    private final String lightThemeSource;
    private final String darkThemeSource;

    private String templateData;
    private Map<String, Color> lightTheme;
    private Map<String, Color> darkTheme;
    private Map<String, Color> currentTheme;

    @Inject
    public ThemeManager(
            @SettingsPath File settingsPath,
            String templateSource,
            String lightThemeSource,
            String darkThemeSource) {
        generatedCss = new File(settingsPath, "generated.css");
        this.templateSource = templateSource;
        this.lightThemeSource = lightThemeSource;
        this.darkThemeSource = darkThemeSource;
    }

    // Initializes class and creates default theme.
    public File initialize() throws IOException {
        templateData = IOUtils.toString(
                getClass().getResource(templateSource), StandardCharsets.UTF_8);
        lightTheme = parseTheme(
                IOUtils.toString(getClass().getResource(lightThemeSource), StandardCharsets.UTF_8));
        darkTheme = parseTheme(
                IOUtils.toString(getClass().getResource(darkThemeSource), StandardCharsets.UTF_8));
        currentTheme = darkTheme;
        applyCurrentTheme();
        return generatedCss;
    }

    public Optional<File> applyLightTheme() {
        if (currentTheme == lightTheme) {
            return Optional.of(generatedCss);
        }
        try {
            currentTheme = lightTheme;
            applyCurrentTheme();
            return Optional.of(generatedCss);
        } catch (IOException e) {
            errorLogger.logError(e);
            return Optional.empty();
        }
    }

    public Optional<File> applyDarkTheme() {
        if (currentTheme == darkTheme) {
            return Optional.of(generatedCss);
        }
        try {
            currentTheme = darkTheme;
            applyCurrentTheme();
            return Optional.of(generatedCss);
        } catch (IOException e) {
            errorLogger.logError(e);
            return Optional.empty();
        }
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

    private void applyCurrentTheme() throws IOException {
        String withCurrentTheme = findAndReplace(templateData, currentTheme);
        String withBackupTheme = findAndReplace(withCurrentTheme, lightTheme); // Backup.
        FileUtils.writeStringToFile(generatedCss, withBackupTheme, StandardCharsets.UTF_8);
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

    // Helper method
    private static String formatHexString(double val) {
        String in = Integer.toHexString((int) Math.round(val * 255));
        return in.length() == 1 ? "0" + in : in;
    }

    // Convert Color to a CSS-readable string.
    public static String toHexString(Color value) {
        return "#"
                + (formatHexString(value.getRed())
                + formatHexString(value.getGreen())
                + formatHexString(value.getBlue())
                + formatHexString(value.getOpacity()))
                .toUpperCase();
    }
}
