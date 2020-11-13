package com.utsusynth.utsu.controller;

import com.google.inject.Inject;
import com.utsusynth.utsu.common.i18n.Localizable;
import com.utsusynth.utsu.common.i18n.Localizer;
import com.utsusynth.utsu.files.PreferencesManager;
import com.utsusynth.utsu.view.config.EditorPreferencesEditor;
import com.utsusynth.utsu.view.config.EnginePreferencesEditor;
import com.utsusynth.utsu.view.config.PreferencesEditor;
import com.utsusynth.utsu.view.config.ThemePreferencesEditor;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.util.ResourceBundle;

/**
 * 'PreferencesScene.fxml' Controller Class
 */
public class PreferencesController implements Localizable {
    private final PreferencesManager preferencesManager;
    private final ThemePreferencesEditor themeEditor;
    private final EditorPreferencesEditor editorEditor;
    private final EnginePreferencesEditor engineEditor;
    private final Localizer localizer;

    @FXML // fx:id="root"
    private BorderPane root; // Value injected by FXMLLoader

    @FXML // fx:id="anchorLeft"
    private AnchorPane anchorLeft;

    @FXML // fx:id="anchorRight"
    private AnchorPane anchorRight; // Value injected by FXMLLoader

    @FXML // fx:id="applyButton"
    private Button applyButton; // Value injected by FXMLLoader

    @FXML // fx:id="cancelButton"
    private Button cancelButton; // Value injected by FXMLLoader

    @Inject
    public PreferencesController(
            PreferencesManager preferencesManager,
            ThemePreferencesEditor themeEditor,
            EditorPreferencesEditor editorEditor,
            EnginePreferencesEditor engineEditor,
            Localizer localizer) {
        this.preferencesManager = preferencesManager;
        this.themeEditor = themeEditor;
        this.editorEditor = editorEditor;
        this.engineEditor = engineEditor;
        this.localizer = localizer;
    }

    public void initialize() {
        // Initialize each internal preferences editor.
        themeEditor.initialize();
        editorEditor.initialize();
        engineEditor.initialize();
        // Set up table of contents.
        TreeItem<PreferencesEditor> root = new TreeItem<>(null);
        root.getChildren().add(new TreeItem<>(themeEditor));
        root.getChildren().add(new TreeItem<>(editorEditor));
        root.getChildren().add(new TreeItem<>(engineEditor));

        TreeView<PreferencesEditor> tableOfContents = new TreeView<>(root);
        tableOfContents.setPrefWidth(anchorLeft.getPrefWidth());
        tableOfContents.setShowRoot(false);
        tableOfContents.setCellFactory(preferencesEditor -> {
            TreeCell<PreferencesEditor> treeCell = new TreeCell<>() {
                @Override
                public void updateItem(PreferencesEditor item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setText(null);
                        return;
                    }
                    setText(getItem() == null ? "" : getItem().getDisplayName());
                }
            };
            treeCell.setOnMouseClicked(event -> {
                if (treeCell.getItem() != null) {
                    anchorRight.getChildren().clear();
                    anchorRight.getChildren().add(treeCell.getItem().getView());
                }
            });
            return treeCell;
        });
        anchorLeft.getChildren().add(tableOfContents);

        // Set up localization.
        localizer.localize(this);
    }

    @Override
    public void localize(ResourceBundle bundle) {
        if (root.getScene() != null) {
            Stage currentStage = (Stage) root.getScene().getWindow();
            currentStage.setTitle(bundle.getString("preferences.title"));
        }
        themeEditor.setDisplayName(bundle.getString("preferences.colorScheme"));
        editorEditor.setDisplayName(bundle.getString("preferences.editor"));
        engineEditor.setDisplayName(bundle.getString("preferences.engine"));
        applyButton.setText(bundle.getString("general.apply"));
        cancelButton.setText(bundle.getString("general.cancel"));
    }

    /* Handler for when modal is closed without saving. */
    public boolean onCloseWindow() {
        if (!themeEditor.onCloseEditor()
                || !editorEditor.onCloseEditor()
                || !engineEditor.onCloseEditor()) {
            return false;
        }
        themeEditor.revertToPreferences();
        editorEditor.revertToPreferences();
        engineEditor.revertToPreferences();
        return true;
    }

    @FXML
    void closePreferences(ActionEvent event) {
        if (!onCloseWindow()) {
            return;
        }
        Stage currentStage = (Stage) root.getScene().getWindow();
        currentStage.close();
    }

    @FXML
    void applyPreferences(ActionEvent event) {
        if (!themeEditor.onCloseEditor() || !editorEditor.onCloseEditor() || !engineEditor.onCloseEditor()) {
            return;
        }
        themeEditor.savePreferences();
        editorEditor.savePreferences();
        engineEditor.savePreferences();
        preferencesManager.saveToFile();
        Stage currentStage = (Stage) root.getScene().getWindow();
        currentStage.close();
    }
}
