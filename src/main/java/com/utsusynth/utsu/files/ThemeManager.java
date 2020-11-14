package com.utsusynth.utsu.files;

import com.utsusynth.utsu.UtsuModule.SettingsPath;
import com.utsusynth.utsu.common.exception.ErrorLogger;
import com.utsusynth.utsu.common.utils.RoundUtils;
import com.utsusynth.utsu.model.config.Theme;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.ChoiceBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public class ThemeManager {
    private static final ErrorLogger errorLogger = ErrorLogger.getLogger();

    public static final String DEFAULT_LIGHT_THEME = "light_theme.txt";
    public static final String DEFAULT_DARK_THEME = "dark_theme.txt";

    private final File generatedCss;
    private final File themesPath;
    private final String templateSource;
    private final String defaultThemeSource;
    private final String defaultThemeId;
    private final ObservableList<Theme> themes;
    private final ObjectProperty<Theme> currentTheme;

    private String templateData;
    private Scene primaryScene;

    public ThemeManager(
            @SettingsPath File settingsPath,
            String templateSource,
            String defaultThemeSource) {
        File cssSettingsPath = new File(settingsPath, "css");
        generatedCss = new File(cssSettingsPath, "generated.css");
        themesPath = new File(cssSettingsPath, "themes");
        this.templateSource = templateSource;
        this.defaultThemeSource = defaultThemeSource;
        defaultThemeId = DEFAULT_LIGHT_THEME;
        themes = FXCollections.observableArrayList();
        currentTheme = new SimpleObjectProperty<>(null);
    }

    // Initializes class and creates default theme.
    public void initialize(Scene scene, String chosenThemeId) throws IOException {
        // Set up template.
        templateData = IOUtils.toString(
                getClass().getResource(templateSource), StandardCharsets.UTF_8);
        primaryScene = scene;

        // Load default themes.
        loadThemeIfNeeded(DEFAULT_LIGHT_THEME);
        loadThemeIfNeeded(DEFAULT_DARK_THEME);

        // Read in list of themes from file.
        if (!themesPath.exists() && !themesPath.mkdirs()) {
            throw new IOException("Error: Failed to create themes path.");
        }
        Iterator<File> themeFiles =
                FileUtils.iterateFiles(themesPath, new SuffixFileFilter(".txt"), null);
        while (themeFiles.hasNext()) {
            try {
                loadThemeIfNeeded(themeFiles.next().getName());
            } catch (IOException e) {
                errorLogger.logWarning(e); // Swallow exceptions reading optional theme files.
            }
        }

        if (hasTheme(chosenThemeId)) {
            currentTheme.set(findTheme(chosenThemeId));
        } else {
            currentTheme.set(findTheme(defaultThemeId));
        }
        applyCurrentTheme(); // Generate css file from theme and template data.
        applyToScene(scene); // Apply generated css to scene.
        // Set this up to be automatic when theme changes again.
        currentTheme.addListener((oldTheme, newTheme, obs) -> {
            if (oldTheme == null || !oldTheme.getValue().getId().equals(newTheme.getId())) {
                reloadCurrentTheme();
            }
        });
    }

    /* Reloads the primary scene using the current theme. Useful when a theme is mutated. */
    public void reloadCurrentTheme() {
        try {
            applyCurrentTheme(); // Creates new generated CSS file.
        } catch (Exception e) {
            errorLogger.logError(e);
            return;
        }
        applyToScene(primaryScene); // Adds new generated CSS file to this scene.
    }

    public void applyToScene(Scene scene) {
        String css = "file:///" + generatedCss.getAbsolutePath().replace("\\", "/");
        if (scene == null) {
            System.out.println("Warning: tried to write theme to a null scene.");
            return;
        }
        if (scene.getStylesheets().size() > 0) {
            scene.getStylesheets().set(0, css);
        } else {
            scene.getStylesheets().add(css);
        }
    }

    public Theme duplicateTheme(Theme themeToCopy, String newName) {
        String newThemeId = UUID.randomUUID() + "_theme.txt";
        Theme newTheme = new Theme(newThemeId);
        newTheme.setName(newName);
        Map<String, Color> shallowCopy = new HashMap<>();
        for (Map.Entry<String, Color> entry : themeToCopy.getColorMap().entrySet()) {
            shallowCopy.put(entry.getKey(), entry.getValue());
        }
        newTheme.setColorMap(shallowCopy);
        writeThemeToFile(newTheme);
        return newTheme;
    }

    public void deleteTheme(Theme themeToDelete) {
        if (isDefault(themeToDelete)) {
            return; // Don't try to delete the default themes.
        }
        themes.remove(themeToDelete);
        try {
            File fileToDelete = new File(themesPath, themeToDelete.getId());
            Files.delete(fileToDelete.toPath());
        } catch (IOException e) {
            errorLogger.logError(e);
        }
    }

    public void populateChoiceBox(ChoiceBox<Theme> choiceBox) {
        choiceBox.setItems(themes);
    }

    public ObjectProperty<Theme> getCurrentTheme() {
        return currentTheme;
    }

    public static boolean isDefault(Theme theme) {
        return theme.getId().equals(DEFAULT_LIGHT_THEME)
                || theme.getId().equals(DEFAULT_DARK_THEME);
    }

    public void writeThemeToFile(Theme toWrite) {
        File writeToMe = new File(themesPath, toWrite.getId());
        try (PrintStream ps = new PrintStream(writeToMe)) {
            ps.println("NAME=" + toWrite.getName());
            for (String key : toWrite.getColorMap().keySet()) {
                ps.println(key + "=" + toHexString(toWrite.getColorMap().get(key)));
            }
            ps.flush();
        } catch (FileNotFoundException e) {
            // TODO: Handle this.
            errorLogger.logError(e);
        }
    }

    public void importTheme() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Color Scheme File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("TXT files", "*.txt"),
                new FileChooser.ExtensionFilter("All files", "*.*"));
        File file = fileChooser.showOpenDialog(null);
        if (file == null) {
            return;
        }
        String newThemeId = UUID.randomUUID() + "_theme.txt";
        try {
            FileUtils.copyFile(file, new File(themesPath, newThemeId));
            loadThemeIfNeeded(newThemeId);
        } catch (IOException e) {
            // TODO: Handle this.
            errorLogger.logError(e);
        }
        currentTheme.setValue(findTheme(newThemeId));
    }

    public void exportTheme(Theme themeToExport) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Create Color Scheme File");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("TXT", "*.txt"));
        File file = fileChooser.showSaveDialog(null);
        if (file == null) {
            return;
        }
        String themeId = themeToExport.getId();
        try {
            FileUtils.copyFile(new File(themesPath, themeId), file);
        } catch (IOException e) {
            // TODO: Handle this.
            errorLogger.logError(e);
        }
    }

    private boolean hasTheme(String id) {
        return findTheme(id) != null;
    }

    private Theme findTheme(String id) {
        for (Theme theme : themes) {
            if (theme.getId().equals(id)) {
                return theme;
            }
        }
        return null;
    }

    private void applyCurrentTheme() throws IOException {
        Theme loadedTheme = loadThemeIfNeeded(currentTheme.get().getId());
        String withCurrentTheme = findAndReplace(templateData, loadedTheme);
        Theme defaultTheme = loadThemeIfNeeded(defaultThemeId);
        String withBackupTheme = findAndReplace(withCurrentTheme, defaultTheme);
        FileUtils.writeStringToFile(generatedCss, withBackupTheme, StandardCharsets.UTF_8);
    }

    private Theme loadThemeIfNeeded(String themeId) throws IOException {
        Theme theme;
        if (hasTheme(themeId)) {
            theme = findTheme(themeId);
        } else {
            theme = new Theme(themeId);
            themes.add(theme);
        }
        assert theme != null;
        if (theme.getColorMap().isEmpty()) {
            if (isDefault(theme)) {
                // Special case for default themes.
                String source = defaultThemeSource + theme.getId();
                parseTheme(
                        theme,
                        IOUtils.toString(getClass().getResource(source), StandardCharsets.UTF_8));
            } else {
                parseTheme(theme, readConfigFile(new File(themesPath, themeId)));
            }
        }
        return theme;
    }

    private static void parseTheme(Theme theme, String themeData) {
        Map<String, Color> themeMap = new HashMap<>();
        for (String themeLine : themeData.split("\n")) {
            if (!themeLine.contains("=")) {
                continue; // Assume this is not a config line.
            }
            String[] themeMapping = themeLine.trim().split("=");
            if (themeMapping.length != 2) {
                continue; // Assume this is not a config line.
            }
            if (themeMapping[0].equals("NAME")) {
                theme.setName(themeMapping[1]); // Special case for theme name.
                continue;
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
        theme.setColorMap(themeMap);
    }

    private static String findAndReplace(String input, Theme theme) {
        Map<String, Color> colorMap = theme.getColorMap();
        ArrayList<String> keyList = new ArrayList<>(colorMap.size());
        ArrayList<String> valueList = new ArrayList<>(colorMap.size());
        for (String key : colorMap.keySet()) {
            keyList.add("$[" + key + "]");
            valueList.add(toHexString(colorMap.get(key)));
        }
        String[] keys = keyList.toArray(new String[colorMap.size()]);
        String[] values = valueList.toArray(new String[colorMap.size()]);
        return StringUtils.replaceEach(input, keys, values);
    }

    // Removes floating point values before converting to hex.
    private static String formatHexString(double value) {
        String in = Integer.toHexString(RoundUtils.round(value * 255));
        return in.length() == 1 ? "0" + in : in;
    }

    // Convert Color to a CSS-readable string.
    private static String toHexString(Color color) {
        return "#"
                + (formatHexString(color.getRed())
                + formatHexString(color.getGreen())
                + formatHexString(color.getBlue())
                + formatHexString(color.getOpacity()))
                .toUpperCase();
    }

    private static String readConfigFile(File file) {
        if (!file.canRead() || !file.isFile()) {
            // This is often okay.
            return "";
        }
        try {
            byte[] bytes = FileUtils.readFileToByteArray(file);
            String charset = "UTF-8";
            CharsetDecoder utf8Decoder =
                    StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                            .onUnmappableCharacter(CodingErrorAction.REPORT);
            try {
                utf8Decoder.decode(ByteBuffer.wrap(bytes));
            } catch (CharacterCodingException e) {
                charset = "SJIS";
            }
            return new String(bytes, charset);
        } catch (IOException e) {
            errorLogger.logError(e);
        }
        return "";
    }
}
