package com.utsusynth.utsu.view.config;

import com.utsusynth.utsu.files.PreferencesManager;
import com.utsusynth.utsu.files.ThemeManager;
import com.utsusynth.utsu.model.config.Theme;
import javafx.collections.FXCollections;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import javax.inject.Inject;

import static com.utsusynth.utsu.files.ThemeManager.DEFAULT_DARK_THEME;
import static com.utsusynth.utsu.files.ThemeManager.DEFAULT_LIGHT_THEME;

public class ThemePreferencesEditor extends PreferencesEditor {
    private final PreferencesManager preferencesManager;
    private final ThemeManager themeManager;

    private String displayName = "Color Scheme";
    private BorderPane view;

    @Inject
    public ThemePreferencesEditor(
            PreferencesManager preferencesManager, ThemeManager themeManager) {
        this.preferencesManager = preferencesManager;
        this.themeManager = themeManager;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    protected void setDisplayNameInternal(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public BorderPane getView() {
        return view;
    }

    @Override
    protected void setViewInternal(BorderPane view) {
        this.view = view;
    }

    @Override
    protected Node initializeInternal() {
        VBox vBox = new VBox();
        vBox.setSpacing(10);

        HBox themeChoiceRow = new HBox();
        themeChoiceRow.setSpacing(10);
        ChoiceBox<Theme> themeChoiceBox = new ChoiceBox<>();
        themeChoiceBox.setItems(FXCollections.observableArrayList(themeManager.getThemes()));
        themeChoiceBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Theme theme) {
                return theme.getId().equals(DEFAULT_LIGHT_THEME) ? "Light Theme" : "Dark Theme";
            }

            @Override
            public Theme fromString(String displayName) {
                return displayName.equals("Light Theme")
                        ? new Theme(DEFAULT_LIGHT_THEME) : new Theme(DEFAULT_DARK_THEME);
            }
        });
        themeChoiceBox.valueProperty().bindBidirectional(themeManager.getCurrentTheme());
        themeChoiceBox.valueProperty().addListener(
                obs -> themeManager.applyToScene(vBox.getScene()));
        Button themeChoiceSettingsButton = new Button("⚙");
        themeChoiceRow.getChildren().addAll(themeChoiceBox, themeChoiceSettingsButton);

        vBox.getChildren().add(themeChoiceRow);
        vBox.getChildren().add(new Separator(Orientation.HORIZONTAL));
        vBox.getChildren().add(new ColorPicker());
        return vBox;
    }

    @Override
    public void savePreferences() {
        preferencesManager.setTheme(themeManager.getCurrentTheme().get());
    }

    @Override
    public void revertToPreferences() {
        themeManager.getCurrentTheme().set(preferencesManager.getTheme());
    }
}
