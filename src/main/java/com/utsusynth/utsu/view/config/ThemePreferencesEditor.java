package com.utsusynth.utsu.view.config;

import com.utsusynth.utsu.files.PreferencesManager;
import com.utsusynth.utsu.files.ThemeManager;
import com.utsusynth.utsu.model.config.Theme;
import javafx.collections.FXCollections;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import javax.inject.Inject;

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
        VBox vBox = new VBox(10);

        HBox themeChoiceRow = new HBox(10);
        ChoiceBox<Theme> themeChoiceBox = new ChoiceBox<>();
        themeChoiceBox.setPrefWidth(150);
        themeChoiceBox.setItems(FXCollections.observableArrayList(themeManager.getThemes()));
        themeChoiceBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Theme theme) {
                return theme.getName().isEmpty() ? theme.getId() : theme.getName();
            }

            @Override
            public Theme fromString(String displayName) {
                return null; // Never used.
            }
        });
        themeChoiceBox.valueProperty().bindBidirectional(themeManager.getCurrentTheme());
        themeChoiceBox.valueProperty().addListener(
                obs -> themeManager.applyToScene(vBox.getScene()));

        Label themeSettingsBox = new Label(" ⚙˯ ");
        themeSettingsBox.getStyleClass().add("theme-settings");
        themeSettingsBox.setOnMouseEntered(event -> {
            themeSettingsBox.getStyleClass().add("highlighted");
        });
        themeSettingsBox.setOnMouseExited(event -> {
            themeSettingsBox.getStyleClass().remove("highlighted");
        });
        ContextMenu contextMenu = new ContextMenu();
        MenuItem renameItem = new MenuItem("Rename");
        MenuItem editItem = new MenuItem("Edit");
        MenuItem duplicateItem = new MenuItem("Duplicate");
        MenuItem importItem = new MenuItem("Import");
        MenuItem exportItem = new MenuItem("Export");
        contextMenu.getItems().addAll(renameItem, editItem, duplicateItem, importItem, exportItem);
        contextMenu.setOnShowing(event -> {
            // Apply localization.
        });
        themeSettingsBox.setOnMouseClicked(event -> {
            contextMenu.hide();
            contextMenu.show(themeChoiceRow, event.getScreenX(), event.getScreenY());
        });

        themeChoiceRow.getChildren().addAll(themeChoiceBox, themeSettingsBox);

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
