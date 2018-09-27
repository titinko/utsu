package com.utsusynth.utsu.controller;

import static javafx.scene.input.KeyCombination.SHIFT_DOWN;
import static javafx.scene.input.KeyCombination.SHORTCUT_DOWN;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.utsusynth.utsu.common.StatusBar;
import com.utsusynth.utsu.common.dialog.SaveWarningDialog;
import com.utsusynth.utsu.common.dialog.SaveWarningDialog.Decision;
import com.utsusynth.utsu.common.exception.ErrorLogger;
import com.utsusynth.utsu.common.i18n.Localizable;
import com.utsusynth.utsu.common.i18n.Localizer;
import com.utsusynth.utsu.common.quantize.Scaler;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

/**
 * 'UtsuScene.fxml' Controller Class
 */
public class UtsuController implements Localizable {
    private static final ErrorLogger errorLogger = ErrorLogger.getLogger();

    private enum EditorType {
        SONG, VOICEBANK,
    }

    // User session data goes here.
    private final Map<String, EditorController> editors;

    // Helper classes go here.
    private final Localizer localizer;
    private final Scaler scaler;
    private final StatusBar statusBar;
    private final Provider<SaveWarningDialog> saveWarningProvider;
    private final Provider<FXMLLoader> fxmlLoaderProvider;

    @FXML
    private TabPane tabs;
    @FXML
    private Label statusLabel;
    @FXML
    private ProgressBar loadingBar;

    @Inject
    public UtsuController(
            Localizer localizer,
            Scaler scaler,
            StatusBar statusBar,
            Provider<SaveWarningDialog> saveWarningProvider,
            Provider<FXMLLoader> fxmlLoaders) {
        this.localizer = localizer;
        this.scaler = scaler;
        this.statusBar = statusBar;
        this.saveWarningProvider = saveWarningProvider;
        this.fxmlLoaderProvider = fxmlLoaders;

        this.editors = new HashMap<>();
    }

    // Provide setup for other controllers.
    // This is called automatically.
    public void initialize() {
        // Create an empty song editor.
        createEditor(EditorType.SONG);

        // Set up localization.
        localizer.localize(this);

        // Create keyboard shortcuts.
        createMenuKeyboardShortcuts();

        // Set up status bar.
        statusBar.initialize(statusLabel.textProperty(), loadingBar.progressProperty());
        loadingBar.progressProperty().addListener(event -> {
            if (loadingBar.getProgress() <= 0 || loadingBar.getProgress() >= 1) {
                loadingBar.setVisible(false);
            } else {
                loadingBar.setVisible(true);
            }
        });
    }

    @FXML
    private Menu fileMenu; // Value injected by FXMLLoader
    @FXML
    private Menu newMenu; // Value injected by FXMLLoader
    @FXML
    private MenuItem newSongItem; // Value injected by FXMLLoader
    @FXML
    private MenuItem newVoicebankItem; // Value injected by FXMLLoader
    @FXML
    private MenuItem openSongItem; // Value injected by FXMLLoader
    @FXML
    private MenuItem openVoicebankItem; // Value injected by FXMLLoader
    @FXML
    private MenuItem saveItem; // Value injected by FXMLLoader
    @FXML
    private MenuItem saveAsItem; // Value injected by FXMLLoader
    @FXML
    private MenuItem exportToWavItem; // Value injected by FXMLLoader;
    @FXML
    private Menu editMenu; // Value injected by FXMLLoader
    @FXML
    private MenuItem undoItem; // Value injected by FXMLLoader
    @FXML
    private MenuItem redoItem; // Value injected by FXMLLoader
    @FXML
    private MenuItem cutItem; // Value injected by FXMLLoader
    @FXML
    private MenuItem copyItem; // Value injected by FXMLLoader
    @FXML
    private MenuItem pasteItem; // Value injected by FXMLLoader
    @FXML
    private MenuItem deleteItem; // Value injected by FXMLLoader
    @FXML
    private MenuItem selectAllItem; // Value injected by FXMLLoader
    @FXML
    private Menu viewMenu; // Value injected by FXMLLoader
    @FXML
    private MenuItem zoomInItem; // Value injected by FXMLLoader
    @FXML
    private MenuItem zoomOutItem; // Value injected by FXMLLoader
    @FXML
    private Menu projectMenu; // Value injected by FXMLLoader
    @FXML
    private MenuItem propertiesItem; // Value injected by FXMLLoader
    @FXML
    private Menu pluginsMenu; // Value injected by FXMLLoader
    @FXML
    private MenuItem openPluginItem; // Value injected by FXMLLoader
    @FXML
    private Menu recentPluginsMenu; // Value injected by FXMLLoader
    @FXML
    private Menu helpMenu; // Value injected by FXMLLoader
    @FXML
    private MenuItem aboutItem; // Value injected by FXMLLoader

    @Override
    public void localize(ResourceBundle bundle) {
        fileMenu.setText(bundle.getString("menu.file"));
        newMenu.setText(bundle.getString("menu.file.new"));
        newSongItem.setText(bundle.getString("menu.file.new.song"));
        newVoicebankItem.setText(bundle.getString("menu.file.new.voicebank"));
        openSongItem.setText(bundle.getString("menu.file.openSong"));
        openVoicebankItem.setText(bundle.getString("menu.file.openVoicebank"));
        saveItem.setText(bundle.getString("general.save"));
        saveAsItem.setText(bundle.getString("menu.file.saveFileAs"));
        exportToWavItem.setText(bundle.getString("menu.file.exportWav"));
        editMenu.setText(bundle.getString("menu.edit"));
        undoItem.setText(bundle.getString("menu.edit.undo"));
        redoItem.setText(bundle.getString("menu.edit.redo"));
        cutItem.setText(bundle.getString("menu.edit.cut"));
        copyItem.setText(bundle.getString("menu.edit.copy"));
        pasteItem.setText(bundle.getString("menu.edit.paste"));
        deleteItem.setText(bundle.getString("menu.edit.delete"));
        selectAllItem.setText(bundle.getString("menu.edit.selectAll"));
        viewMenu.setText(bundle.getString("menu.view"));
        zoomInItem.setText(bundle.getString("menu.view.zoomIn"));
        zoomOutItem.setText(bundle.getString("menu.view.zoomOut"));
        projectMenu.setText(bundle.getString("menu.project"));
        propertiesItem.setText(bundle.getString("menu.project.properties"));
        pluginsMenu.setText(bundle.getString("menu.plugins"));
        openPluginItem.setText(bundle.getString("menu.plugins.openPlugin"));
        recentPluginsMenu.setText(bundle.getString("menu.plugins.recentPlugins"));
        helpMenu.setText(bundle.getString("menu.help"));
        aboutItem.setText(bundle.getString("menu.help.about"));

        // Force the menu to refresh.
        fileMenu.setVisible(false);
        fileMenu.setVisible(true);
    }

    private void createMenuKeyboardShortcuts() {
        saveItem.setAccelerator(new KeyCodeCombination(KeyCode.S, SHORTCUT_DOWN));
        saveAsItem.setAccelerator(new KeyCodeCombination(KeyCode.S, SHORTCUT_DOWN, SHIFT_DOWN));
        exportToWavItem.setAccelerator(new KeyCodeCombination(KeyCode.W, SHORTCUT_DOWN));
        undoItem.setAccelerator(new KeyCodeCombination(KeyCode.Z, SHORTCUT_DOWN));
        redoItem.setAccelerator(new KeyCodeCombination(KeyCode.Z, SHORTCUT_DOWN, SHIFT_DOWN));
        cutItem.setAccelerator(new KeyCodeCombination(KeyCode.X, SHORTCUT_DOWN));
        copyItem.setAccelerator(new KeyCodeCombination(KeyCode.C, SHORTCUT_DOWN));
        pasteItem.setAccelerator(new KeyCodeCombination(KeyCode.V, SHORTCUT_DOWN));
        deleteItem.setAccelerator(new KeyCodeCombination(KeyCode.D, SHORTCUT_DOWN));
        selectAllItem.setAccelerator(new KeyCodeCombination(KeyCode.A, SHORTCUT_DOWN));
        zoomInItem.setAccelerator(new KeyCodeCombination(KeyCode.EQUALS, SHORTCUT_DOWN));
        zoomOutItem.setAccelerator(new KeyCodeCombination(KeyCode.MINUS, SHORTCUT_DOWN));
        propertiesItem.setAccelerator(new KeyCodeCombination(KeyCode.P, SHORTCUT_DOWN));
        helpMenu.setAccelerator(new KeyCodeCombination(KeyCode.SLASH, SHORTCUT_DOWN, SHIFT_DOWN));
    }

    /**
     * Called whenever a key is pressed, excluding text input. Can override default key press
     * behaviors. Accelerators should be used instead when overrides are not needed.
     * 
     * @return true if an override behavior for this key was found, false otherwise
     */
    public boolean onKeyPressed(KeyEvent keyEvent) {
        if (!tabs.getTabs().isEmpty()) {
            Tab curTab = tabs.getSelectionModel().getSelectedItem();
            return editors.get(curTab.getId()).onKeyPressed(keyEvent);
        }
        return false;
    }

    /**
     * Called whenever Utsu is closed.
     * 
     * @return true if window should be closed, false otherwise
     */
    public boolean onCloseWindow() {
        for (Tab tab : tabs.getTabs()) {
            if (!onCloseTab(tab)) {
                return false;
            }
        }
        return true;
    }

    private boolean onCloseTab(Tab tab) {
        String fileName = editors.get(tab.getId()).getFileName();
        if (fileName.length() < tab.getText().length()) {
            // If tab has unsaved changes, confirm close.
            Stage parent = (Stage) tabs.getScene().getWindow();
            Decision decision = saveWarningProvider.get().popup(parent, fileName);
            switch (decision) {
                case CANCEL:
                    return false;
                case CLOSE_WITHOUT_SAVING:
                    return true;
                case SAVE_AND_CLOSE:
                    editors.get(tab.getId()).save();
                    return true;
            }
        }
        return true;
    }

    @FXML
    void newSong(ActionEvent event) {
        createEditor(EditorType.SONG);
    }

    @FXML
    void newVoicebank(ActionEvent event) {
        // TODO
    }

    private Tab createEditor(EditorType type) {
        try {
            // Open song editor.
            String fxmlString =
                    type == EditorType.SONG ? "/fxml/SongScene.fxml" : "/fxml/VoicebankScene.fxml";
            InputStream fxml = getClass().getResourceAsStream(fxmlString);
            FXMLLoader loader = fxmlLoaderProvider.get();

            // Create tab.
            Tab tab = new Tab("Untitled", loader.load(fxml));
            String tabId = "tab" + new Date().getTime(); // Use current timestamp for id.
            tab.setId(tabId);
            tab.setOnSelectionChanged(event -> {
                if (tab.isSelected()) {
                    // Enable saveItem if tab can be saved.
                    EditorController editor = editors.get(tabId);
                    boolean fileChanged = editor.getFileName().length() < tab.getText().length();
                    saveItem.setDisable(!fileChanged || !editor.hasPermanentLocation());
                    if (type == EditorType.SONG) {
                        saveAsItem.setDisable(false);
                        exportToWavItem.setDisable(false);
                        cutItem.setDisable(false);
                        copyItem.setDisable(false);
                        pasteItem.setDisable(false);
                        deleteItem.setDisable(false);
                        propertiesItem.setDisable(false);
                        recentPluginsMenu.setDisable(recentPluginsMenu.getItems().isEmpty());
                    } else if (type == EditorType.VOICEBANK) {
                        saveAsItem.setDisable(true);
                        exportToWavItem.setDisable(true);
                        cutItem.setDisable(true);
                        copyItem.setDisable(true);
                        pasteItem.setDisable(true);
                        deleteItem.setDisable(true);
                        propertiesItem.setDisable(true);
                        recentPluginsMenu.setDisable(true);
                    }
                }
            });
            tab.setOnCloseRequest(event -> {
                if (!onCloseTab(tab)) {
                    event.consume();
                }
            });
            tab.setOnClosed(event -> {
                editors.get(tabId).closeEditor();
                editors.remove(tabId);
            });
            EditorController editor = (EditorController) loader.getController();
            editors.put(tab.getId(), editor);
            editor.openEditor(new EditorCallback() {
                @Override
                public void markChanged() {
                    // Adds a handy * to indicate unsaved changes.
                    if (!tab.getText().startsWith("*")) {
                        tab.setText("*" + tab.getText());
                    }
                }

                @Override
                public void enableSave(boolean enabled) {
                    if (enabled) {
                        // Adds a handy * to indicate unsaved changes.
                        if (!tab.getText().startsWith("*")) {
                            tab.setText("*" + tab.getText());
                        }
                    } else {
                        // Remove the * if present.
                        if (tab.getText().startsWith("*")) {
                            tab.setText(tab.getText().substring(1));
                        }
                    }
                    saveItem.setDisable(!enabled);
                }
            });
            editor.refreshView();
            tab.setText(editor.getFileName()); // Uses file name for tab name.

            // Add and select new tab.
            tabs.getTabs().add(tab);
            tabs.getSelectionModel().select(tab);
            return tab;
        } catch (IOException e) {
            // TODO Handle this
            errorLogger.logError(e);
        }
        return null;
    }

    @FXML
    void openSong(ActionEvent event) {
        Tab newTab = createEditor(EditorType.SONG);
        Optional<String> songName = editors.get(newTab.getId()).open();
        if (songName.isPresent()) {
            newTab.setText(songName.get());
        }
    }

    @FXML
    void openVoicebank(ActionEvent event) {
        Tab newTab = createEditor(EditorType.VOICEBANK);
        Optional<String> voicebankName = editors.get(newTab.getId()).open();
        if (voicebankName.isPresent()) {
            newTab.setText(voicebankName.get());
        }
    }

    @FXML
    void saveFile(ActionEvent event) {
        if (!tabs.getTabs().isEmpty()) {
            Tab curTab = tabs.getSelectionModel().getSelectedItem();
            Optional<String> filename = editors.get(curTab.getId()).save();
            if (filename.isPresent()) {
                curTab.setText(filename.get());
            }
        }
    }

    @FXML
    void saveFileAs(ActionEvent event) {
        if (!tabs.getTabs().isEmpty()) {
            Tab curTab = tabs.getSelectionModel().getSelectedItem();
            Optional<String> filename = editors.get(curTab.getId()).saveAs();
            if (filename.isPresent()) {
                curTab.setText(filename.get());
            }
        }
    }

    @FXML
    void exportToWav(ActionEvent event) {
        if (!tabs.getTabs().isEmpty()) {
            editors.get(tabs.getSelectionModel().getSelectedItem().getId()).exportToWav();
        }
    }

    @FXML
    void undo(ActionEvent event) {
        if (!tabs.getTabs().isEmpty()) {
            editors.get(tabs.getSelectionModel().getSelectedItem().getId()).undo();
        }
    }

    @FXML
    void redo(ActionEvent event) {
        if (!tabs.getTabs().isEmpty()) {
            editors.get(tabs.getSelectionModel().getSelectedItem().getId()).redo();
        }
    }

    @FXML
    void cut(ActionEvent event) {
        if (!tabs.getTabs().isEmpty()) {
            editors.get(tabs.getSelectionModel().getSelectedItem().getId()).cut();
        }
    }

    @FXML
    void copy(ActionEvent event) {
        if (!tabs.getTabs().isEmpty()) {
            editors.get(tabs.getSelectionModel().getSelectedItem().getId()).copy();
        }
    }

    @FXML
    void paste(ActionEvent event) {
        if (!tabs.getTabs().isEmpty()) {
            editors.get(tabs.getSelectionModel().getSelectedItem().getId()).paste();
        }
    }

    @FXML
    void delete(ActionEvent event) {
        if (!tabs.getTabs().isEmpty()) {
            editors.get(tabs.getSelectionModel().getSelectedItem().getId()).delete();
        }
    }

    @FXML
    void selectAll(ActionEvent event) {
        if (!tabs.getTabs().isEmpty()) {
            editors.get(tabs.getSelectionModel().getSelectedItem().getId()).selectAll();
        }
    }

    @FXML
    void zoomIn(ActionEvent event) {
        double newScale = scaler.getHorizontalScale() + Scaler.HORIZONTAL_SCALE_INDREMENT;
        scaler.changeHorizontalScale(scaler.getHorizontalScale(), newScale);
        if (newScale >= Scaler.MAX_HORIZONTAL_SCALE) {
            zoomInItem.setDisable(true);
        }
        if (newScale > Scaler.MIN_HORIZONTAL_SCALE) {
            zoomOutItem.setDisable(false);
        }
        for (Tab tab : tabs.getTabs()) {
            editors.get(tab.getId()).refreshView();
        }
    }

    @FXML
    void zoomOut(ActionEvent event) {
        double newScale = scaler.getHorizontalScale() - Scaler.HORIZONTAL_SCALE_INDREMENT;
        scaler.changeHorizontalScale(scaler.getHorizontalScale(), newScale);
        if (newScale <= Scaler.MIN_HORIZONTAL_SCALE) {
            zoomOutItem.setDisable(true);
        }
        if (newScale < Scaler.MAX_HORIZONTAL_SCALE) {
            zoomInItem.setDisable(false);
        }
        for (Tab tab : tabs.getTabs()) {
            editors.get(tab.getId()).refreshView();
        }
    }

    @FXML
    void openProperties(ActionEvent event) {
        if (!tabs.getTabs().isEmpty()) {
            editors.get(tabs.getSelectionModel().getSelectedItem().getId()).openProperties();
        }
    }

    @FXML
    void openPlugin(ActionEvent event) {
        if (!tabs.getTabs().isEmpty()) {
            Tab curTab = tabs.getSelectionModel().getSelectedItem();
            Optional<File> plugin = editors.get(curTab.getId()).openPlugin();
            if (plugin.isPresent()) {
                String name = plugin.get().getName();
                // Clean up existing shortcuts if necessary.
                recentPluginsMenu.getItems().removeIf(item -> item.getText().equals(name));
                if (recentPluginsMenu.getItems().size() > 10) {
                    recentPluginsMenu.getItems().remove(9, recentPluginsMenu.getItems().size());
                }
                // Add shortcut to this plugin for the rest of current session.
                MenuItem newItem = new MenuItem(plugin.get().getName());
                newItem.setOnAction(invocation -> {
                    Tab newCurTab = tabs.getSelectionModel().getSelectedItem();
                    editors.get(newCurTab.getId()).invokePlugin(plugin.get());
                });
                recentPluginsMenu.getItems().add(0, newItem);
                recentPluginsMenu.setDisable(false);
            }
        }
    }
}
