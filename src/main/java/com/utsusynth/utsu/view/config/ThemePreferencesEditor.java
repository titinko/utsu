package com.utsusynth.utsu.view.config;

import com.google.inject.Provider;
import com.utsusynth.utsu.common.dialog.DeleteWarningDialog;
import com.utsusynth.utsu.common.dialog.SaveWarningDialog;
import com.utsusynth.utsu.common.i18n.Localizable;
import com.utsusynth.utsu.common.i18n.Localizer;
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
import javafx.stage.Stage;
import javafx.util.StringConverter;

import javax.inject.Inject;
import java.util.ResourceBundle;

public class ThemePreferencesEditor extends PreferencesEditor implements Localizable {
    private final ThemeColorPicker themeColorPicker;
    private final PreferencesManager preferencesManager;
    private final ThemeManager themeManager;
    private final Provider<SaveWarningDialog> saveWarningProvider;
    private final Provider<DeleteWarningDialog> deleteWarningProvider;
    private final Localizer localizer;

    // Session data.
    private String displayName = "Color Scheme";
    private boolean newThemePending = false;
    private boolean editPending = false;
    private BorderPane view;
    private VBox viewInternal;
    private Group themeRow;
    private HBox themeChoiceRow;
    private ChoiceBox<Theme> themeChoiceBox;
    private HBox themeEnterRow;
    private TextField themeTextField;

    @Inject
    public ThemePreferencesEditor(
            ThemeColorPicker themeColorPicker,
            PreferencesManager preferencesManager,
            ThemeManager themeManager,
            Provider<SaveWarningDialog> saveWarningProvider,
            Provider<DeleteWarningDialog> deleteWarningProvider,
            Localizer localizer) {
        this.themeColorPicker = themeColorPicker;
        this.preferencesManager = preferencesManager;
        this.themeManager = themeManager;
        this.saveWarningProvider = saveWarningProvider;
        this.deleteWarningProvider = deleteWarningProvider;
        this.localizer = localizer;
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
        themeRow = new Group();

        initializeThemeChoiceRow();
        initializeThemeEnterRow();
        themeRow.getChildren().add(themeChoiceRow);

        viewInternal = new VBox(10);
        viewInternal.getChildren().add(themeRow);
        viewInternal.getChildren().add(new Separator(Orientation.HORIZONTAL));

        localizer.localize(this);
        return viewInternal;
    }

    @Override
    public boolean onCloseEditor(Stage stage) {
        return resolveOngoingEdits(stage); // Don't bother with in-progress duplications.
    }

    @Override
    public void savePreferences() {
        preferencesManager.setTheme(themeManager.getCurrentTheme().get());
    }

    @Override
    public void revertToPreferences() {
        themeManager.getCurrentTheme().set(preferencesManager.getTheme());
    }

    @Override
    public void localize(ResourceBundle bundle) {
        themeManager.renameDefaultThemes(
                bundle.getString("preferences.colorScheme.defaultLight"),
                bundle.getString("preferences.colorScheme.defaultDark"));
        // Reload choice box to make new names appear.
        initializeThemeChoiceBox();
        themeChoiceRow.getChildren().set(0, themeChoiceBox);
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
        MenuItem editItem = new MenuItem("Edit");
        editItem.setOnAction(event -> {
            editPending = true;
            themeTextField.setText(themeChoiceBox.getValue().getName());
            themeRow.getChildren().set(0, themeEnterRow);
            viewInternal.getChildren().add(themeColorPicker.initialize(themeChoiceBox.getValue()));
        });
        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setOnAction(event -> {
            Theme themeToDelete = themeChoiceBox.getValue();
            Stage parent = (Stage) viewInternal.getScene().getWindow();
            DeleteWarningDialog.Decision decision =
                    deleteWarningProvider.get().popup(parent, themeToDelete.getName());
            if (decision != DeleteWarningDialog.Decision.DELETE) {
                return; // Give the user one last chance to back out.
            }
            themeChoiceBox.setValue(themeChoiceBox.getItems().get(0)); // Swap to default theme.
            themeManager.applyToScene(viewInternal.getScene()); // Needed for some reason.
            if (preferencesManager.getTheme().equals(themeToDelete)) {
                // If needed, save a different theme to the preferences file.
                preferencesManager.setTheme(themeChoiceBox.getValue());
                preferencesManager.saveToFile();
            }
            themeManager.deleteTheme(themeToDelete);
        });
        MenuItem importItem = new MenuItem("Import...");
        importItem.setOnAction(event -> {
            themeManager.importTheme();
            // Reload choice box to make fully-loaded theme appear.
            initializeThemeChoiceBox();
            themeChoiceRow.getChildren().set(0, themeChoiceBox);
        });
        MenuItem exportItem = new MenuItem("Export...");
        exportItem.setOnAction(event -> themeManager.exportTheme(themeChoiceBox.getValue()));
        contextMenu.getItems().addAll(
                duplicateItem,
                new SeparatorMenuItem(),
                editItem,
                deleteItem,
                new SeparatorMenuItem(),
                importItem,
                exportItem);
        contextMenu.setOnShowing(event -> {
            Theme currentTheme = themeChoiceBox.getValue();
            editItem.setDisable(ThemeManager.isDefault(currentTheme));
            deleteItem.setDisable(ThemeManager.isDefault(currentTheme));
            // Apply localization.
            duplicateItem.setText(localizer.getMessage("preferences.colorScheme.duplicate"));
            editItem.setText(localizer.getMessage("menu.edit"));
            deleteItem.setText(localizer.getMessage("menu.edit.delete"));
            importItem.setText(localizer.getMessage("preferences.colorScheme.import"));
            exportItem.setText(localizer.getMessage("preferences.colorScheme.export"));
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
                // Creating a new theme.
                newThemePending = false;
                Theme duplicateTheme = themeManager.duplicateTheme(
                        themeChoiceBox.getValue(), newThemeName);
                themeChoiceBox.setValue(duplicateTheme);
            } else if (editPending) {
                // Editing an existing theme.
                themeChoiceBox.getValue().setName(newThemeName);
                themeManager.writeThemeToFile(themeChoiceBox.getValue());
            }
            // Reload choice box to make new name appear.
            initializeThemeChoiceBox();
            themeChoiceRow.getChildren().set(0, themeChoiceBox);
        } else {
            if (editPending) {
                // Cancel edit on an existing theme.
                themeChoiceBox.getValue().getColorMap().clear(); // Triggers reload from file.
                themeManager.reloadCurrentTheme();
                themeManager.applyToScene(viewInternal.getScene());
            }
        }
        // Remove any pending changes.
        newThemePending = false;
        editPending = false;
        // Swap out for choice box.
        themeRow.getChildren().set(0, themeChoiceRow);
        // Delete color picker if present.
        if (viewInternal.getChildren().size() > 2) {
            viewInternal.getChildren().remove(2);
        }
    }

    private boolean resolveOngoingEdits(Stage parent) {
        if (editPending) {
            // If editor has unsaved changes, confirm close.
            String newName = themeTextField.getText();
            SaveWarningDialog.Decision decision = saveWarningProvider.get().popup(parent, newName);
            switch (decision) {
                case CANCEL:
                    return false;
                case CLOSE_WITHOUT_SAVING:
                    closeThemeEnterRow("");
                    return true;
                case SAVE_AND_CLOSE:
                    closeThemeEnterRow(newName);
                    return true;
            }
        }
        return true;
    }
}
