package com.utsusynth.utsu.view.config;

import com.utsusynth.utsu.files.PreferencesManager;
import com.utsusynth.utsu.files.ThemeManager;
import com.utsusynth.utsu.model.config.Theme;
import javafx.geometry.Orientation;
import javafx.scene.Group;
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

    // Session data.
    private String displayName = "Color Scheme";
    private boolean newThemePending = false;
    private BorderPane view;
    private VBox viewInternal;
    private Group themeRow;
    private HBox themeChoiceRow;
    private ChoiceBox<Theme> themeChoiceBox;
    private HBox themeEnterRow;
    private TextField themeTextField;

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
        viewInternal = new VBox(10);
        themeRow = new Group();

        initializeThemeChoiceRow();
        initializeThemeEnterRow();
        themeRow.getChildren().add(themeChoiceRow);
        viewInternal.getChildren().add(themeRow);
        viewInternal.getChildren().add(new Separator(Orientation.HORIZONTAL));
        viewInternal.getChildren().add(new ColorPicker());
        return viewInternal;
    }

    @Override
    public void savePreferences() {
        preferencesManager.setTheme(themeManager.getCurrentTheme().get());
    }

    @Override
    public void revertToPreferences() {
        themeManager.getCurrentTheme().set(preferencesManager.getTheme());
    }

    private void initializeThemeChoiceRow() {
        themeChoiceRow = new HBox(10);
        initializeThemeChoiceBox();

        Label themeSettingsBox = new Label(" ⚙˯ ");
        themeSettingsBox.getStyleClass().add("theme-settings");
        themeSettingsBox.setOnMouseEntered(event -> {
            themeSettingsBox.getStyleClass().add("highlighted");
        });
        themeSettingsBox.setOnMouseExited(event -> {
            themeSettingsBox.getStyleClass().remove("highlighted");
        });
        ContextMenu contextMenu = new ContextMenu();
        MenuItem duplicateItem = new MenuItem("Duplicate");
        duplicateItem.setOnAction(event -> {
            newThemePending = true;
            themeTextField.setText(themeChoiceBox.getValue().getName());
            themeRow.getChildren().set(0, themeEnterRow);
        });
        MenuItem renameItem = new MenuItem("Rename");
        renameItem.setOnAction(event -> {
            themeTextField.setText(themeChoiceBox.getValue().getName());
            themeRow.getChildren().set(0, themeEnterRow);
        });
        MenuItem editItem = new MenuItem("Edit");
        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setOnAction(event -> {
            themeManager.deleteTheme(themeChoiceBox.getValue());
            themeChoiceBox.setValue(themeChoiceBox.getItems().get(0));
        });
        MenuItem importItem = new MenuItem("Import...");
        MenuItem exportItem = new MenuItem("Export...");
        contextMenu.getItems().addAll(
                duplicateItem,
                new SeparatorMenuItem(),
                renameItem,
                editItem,
                deleteItem,
                new SeparatorMenuItem(),
                importItem,
                exportItem);
        contextMenu.setOnShowing(event -> {
            Theme currentTheme = themeChoiceBox.getValue();
            renameItem.setDisable(ThemeManager.isDefault(currentTheme));
            editItem.setDisable(ThemeManager.isDefault(currentTheme));
            deleteItem.setDisable(ThemeManager.isDefault(currentTheme));
            // Apply localization.
        });
        themeSettingsBox.setOnMouseClicked(event -> {
            contextMenu.hide();
            contextMenu.show(themeChoiceRow, event.getScreenX(), event.getScreenY());
        });
        themeChoiceRow.getChildren().addAll(themeChoiceBox, themeSettingsBox);
    }

    private void initializeThemeChoiceBox() {
        themeChoiceBox = new ChoiceBox<>();
        themeManager.populateChoiceBox(themeChoiceBox);
        themeChoiceBox.setPrefWidth(150);
        themeChoiceBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Theme theme) {
                return theme.getName();
            }

            @Override
            public Theme fromString(String displayName) {
                return null; // Never used.
            }
        });
        themeChoiceBox.valueProperty().bindBidirectional(themeManager.getCurrentTheme());
        themeChoiceBox.valueProperty().addListener(
                obs -> themeManager.applyToScene(viewInternal.getScene()));
    }

    private void initializeThemeEnterRow() {
        themeEnterRow = new HBox(10);
        themeTextField = new TextField();
        themeTextField.setPrefWidth(150);
        themeTextField.setOnAction(event -> closeThemeEnterRow(themeTextField.getText()));
        Button cancelButton = new Button("Cancel");
        cancelButton.setCancelButton(true);
        cancelButton.setOnAction(event -> closeThemeEnterRow(""));
        Button applyButton = new Button("Apply");
        applyButton.setDefaultButton(true);
        applyButton.setOnAction(event -> closeThemeEnterRow(themeTextField.getText()));
        themeEnterRow.getChildren().addAll(themeTextField, cancelButton, applyButton);
    }

    private void closeThemeEnterRow(String newThemeName) {
        if (!themeRow.getChildren().contains(themeEnterRow)) {
            return; // Prevent accidental double activations.
        }
        if (!newThemeName.isEmpty()) {
            if (newThemePending) {
                newThemePending = false;
                Theme duplicateTheme = themeManager.duplicateTheme(
                        themeChoiceBox.getValue(), newThemeName);
                themeChoiceBox.setValue(duplicateTheme);
            } else {
                // Regenerate choice box with new name.
                themeChoiceBox.getValue().setName(newThemeName);
            }
            initializeThemeChoiceBox();
            themeChoiceRow.getChildren().set(0, themeChoiceBox);
        }
        // Swap out for choice box.
        themeRow.getChildren().set(0, themeChoiceRow);
    }
}
