package com.utsusynth.utsu.controller;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.utsusynth.utsu.common.StatusBar;
import com.utsusynth.utsu.common.dialog.SaveWarningDialog;
import com.utsusynth.utsu.common.dialog.SaveWarningDialog.Decision;
import com.utsusynth.utsu.common.exception.ErrorLogger;
import com.utsusynth.utsu.common.exception.FileAlreadyOpenException;
import com.utsusynth.utsu.common.i18n.Localizable;
import com.utsusynth.utsu.common.i18n.Localizer;
import com.utsusynth.utsu.common.quantize.DiscreteScaler;
import com.utsusynth.utsu.controller.song.BulkEditorController.BulkEditorType;
import com.utsusynth.utsu.files.ThemeManager;
import javafx.animation.PauseTransition;
import javafx.beans.property.BooleanProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static javafx.scene.input.KeyCombination.*;

/**
 * 'UtsuScene.fxml' Controller Class
 */
public class UtsuController implements Localizable {
    private static final ErrorLogger errorLogger = ErrorLogger.getLogger();

    // All available checkbox menu items.
    public enum CheckboxType {
        SHOW_LYRICS, SHOW_ALIASES, SHOW_PITCHBENDS,
    }

    private enum EditorType {
        SONG, VOICEBANK,
    }

    // User session data goes here.
    private final Map<String, EditorController> editors;

    // Helper classes go here.
    private final ThemeManager themeManager;
    private final Localizer localizer;
    private final DiscreteScaler scaler;
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
            ThemeManager themeManager,
            Localizer localizer,
            DiscreteScaler scaler,
            StatusBar statusBar,
            Provider<SaveWarningDialog> saveWarningProvider,
            Provider<FXMLLoader> fxmlLoaders) {
        this.themeManager = themeManager;
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

        // TODO: Resurrect Mac-specific preferences code once nsmenufx supports jlink.
        /*String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("mac")) {
            MenuToolkit macToolkit = MenuToolkit.toolkit();
            fileMenu.getItems().remove(preferencesItem);
            Menu appMenu = macToolkit.createDefaultApplicationMenu("Utsu");
            appMenu.getItems().add(2, preferencesItem);
            preferencesItem.setAccelerator(new KeyCodeCombination(KeyCode.COMMA, META_DOWN));
            appMenu.getItems().add(3, new SeparatorMenuItem());
            macToolkit.setApplicationMenu(appMenu);
        }*/

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
    private MenuItem preferencesItem; // Value injected by FXMLLoader;
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
    private MenuItem notePropertiesItem; // Value injected by FXMLLoader
    @FXML
    private Menu viewMenu; // Value injected by FXMLLoader
    @FXML
    private MenuItem zoomInHorizontallyItem; // Value injected by FXMLLoader
    @FXML
    private MenuItem zoomOutHorizontallyItem; // Value injected by FXMLLoader
    @FXML
    private MenuItem zoomInVerticallyItem; // Value injected by FXMLLoader
    @FXML
    private MenuItem zoomOutVerticallyItem; // Value injected by FXMLLoader
    @FXML
    private CheckMenuItem showLyricsItem; // Value injected by FXMLLoader
    @FXML
    private CheckMenuItem showAliasesItem; // Value injected by FXMLLoader
    @FXML
    private CheckMenuItem showPitchbendsItem; // Value injected by FXMLLoader
    @FXML
    private Menu projectMenu; // Value injected by FXMLLoader
    @FXML
    private MenuItem propertiesItem; // Value injected by FXMLLoader
    @FXML
    private Menu toolsMenu; // Value injected by FXMLLoader
    @FXML
    private Menu bulkEditorMenu; // Value injected by FXMLLoader
    @FXML
    private MenuItem portamentoEditorItem; // Value injected by FXMLLoader
    @FXML
    private MenuItem vibratoEditorItem; // Value injected by FXMLLoader
    @FXML
    private MenuItem envelopeEditorItem; // Value injected by FXMLLoader
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
        preferencesItem.setText(bundle.getString("menu.file.preferences"));
        editMenu.setText(bundle.getString("menu.edit"));
        undoItem.setText(bundle.getString("menu.edit.undo"));
        redoItem.setText(bundle.getString("menu.edit.redo"));
        cutItem.setText(bundle.getString("menu.edit.cut"));
        copyItem.setText(bundle.getString("menu.edit.copy"));
        pasteItem.setText(bundle.getString("menu.edit.paste"));
        deleteItem.setText(bundle.getString("menu.edit.delete"));
        selectAllItem.setText(bundle.getString("menu.edit.selectAll"));
        notePropertiesItem.setText(bundle.getString("menu.edit.noteProperties"));
        viewMenu.setText(bundle.getString("menu.view"));
        zoomInHorizontallyItem.setText(bundle.getString("menu.view.zoomInHorizontally"));
        zoomOutHorizontallyItem.setText(bundle.getString("menu.view.zoomOutHorizontally"));
        zoomInVerticallyItem.setText(bundle.getString("menu.view.zoomInVertically"));
        zoomOutVerticallyItem.setText(bundle.getString("menu.view.zoomOutVertically"));
        showLyricsItem.setText(bundle.getString("menu.view.showLyrics"));
        showAliasesItem.setText(bundle.getString("menu.view.showAliases"));
        showPitchbendsItem.setText(bundle.getString("menu.view.showPitchbends"));
        projectMenu.setText(bundle.getString("menu.project"));
        propertiesItem.setText(bundle.getString("menu.project.properties"));
        toolsMenu.setText(bundle.getString("menu.tools"));
        bulkEditorMenu.setText(bundle.getString("menu.tools.bulkEditor"));
        portamentoEditorItem.setText(bundle.getString("menu.tools.bulkEditor.portamento"));
        vibratoEditorItem.setText(bundle.getString("song.note.vibrato"));
        envelopeEditorItem.setText(bundle.getString("menu.tools.bulkEditor.envelope"));
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
        newSongItem.setAccelerator(new KeyCodeCombination(KeyCode.N, SHORTCUT_DOWN));
        newVoicebankItem
                .setAccelerator(new KeyCodeCombination(KeyCode.N, SHORTCUT_DOWN, SHIFT_DOWN));
        openSongItem.setAccelerator(new KeyCodeCombination(KeyCode.O, SHORTCUT_DOWN));
        openVoicebankItem
                .setAccelerator(new KeyCodeCombination(KeyCode.O, SHORTCUT_DOWN, SHIFT_DOWN));
        saveItem.setAccelerator(new KeyCodeCombination(KeyCode.S, SHORTCUT_DOWN));
        saveAsItem.setAccelerator(new KeyCodeCombination(KeyCode.S, SHORTCUT_DOWN, SHIFT_DOWN));
        exportToWavItem.setAccelerator(new KeyCodeCombination(KeyCode.W, SHORTCUT_DOWN));
        preferencesItem.setAccelerator(new KeyCodeCombination(KeyCode.COMMA, SHORTCUT_DOWN));
        undoItem.setAccelerator(new KeyCodeCombination(KeyCode.Z, SHORTCUT_DOWN));
        redoItem.setAccelerator(new KeyCodeCombination(KeyCode.Z, SHORTCUT_DOWN, SHIFT_DOWN));
        cutItem.setAccelerator(new KeyCodeCombination(KeyCode.X, SHORTCUT_DOWN));
        copyItem.setAccelerator(new KeyCodeCombination(KeyCode.C, SHORTCUT_DOWN));
        pasteItem.setAccelerator(new KeyCodeCombination(KeyCode.V, SHORTCUT_DOWN));
        deleteItem.setAccelerator(new KeyCodeCombination(KeyCode.D, SHORTCUT_DOWN));
        selectAllItem.setAccelerator(new KeyCodeCombination(KeyCode.A, SHORTCUT_DOWN));
        notePropertiesItem.setAccelerator(new KeyCodeCombination(KeyCode.E, SHORTCUT_DOWN));
        zoomInHorizontallyItem
                .setAccelerator(new KeyCodeCombination(KeyCode.EQUALS, SHORTCUT_DOWN));
        zoomInHorizontallyItem
                .setDisable(
                        scaler.getHorizontalRank() == DiscreteScaler.HORIZONTAL_SCALES.size() - 1);
        zoomOutHorizontallyItem
                .setAccelerator(new KeyCodeCombination(KeyCode.MINUS, SHORTCUT_DOWN));
        zoomOutHorizontallyItem.setDisable(scaler.getHorizontalRank() == 0);
        zoomInVerticallyItem
                .setAccelerator(new KeyCodeCombination(KeyCode.EQUALS, SHORTCUT_DOWN, SHIFT_DOWN));
        zoomInVerticallyItem
                .setDisable(scaler.getVerticalRank() == DiscreteScaler.VERTICAL_SCALES.size() - 1);
        zoomOutVerticallyItem
                .setAccelerator(new KeyCodeCombination(KeyCode.MINUS, SHORTCUT_DOWN, SHIFT_DOWN));
        zoomOutVerticallyItem.setDisable(scaler.getVerticalRank() == 0);
        propertiesItem.setAccelerator(new KeyCodeCombination(KeyCode.P, SHORTCUT_DOWN));
        portamentoEditorItem
                .setAccelerator(new KeyCodeCombination(KeyCode.P, SHORTCUT_DOWN, SHIFT_DOWN));
        vibratoEditorItem
                .setAccelerator(new KeyCodeCombination(KeyCode.V, SHORTCUT_DOWN, SHIFT_DOWN));
        envelopeEditorItem
                .setAccelerator(new KeyCodeCombination(KeyCode.E, SHORTCUT_DOWN, SHIFT_DOWN));
        helpMenu.setAccelerator(new KeyCodeCombination(KeyCode.SLASH, SHORTCUT_DOWN, SHIFT_DOWN));
    }

    /**
     * Called whenever a key is pressed, excluding text input. Can override default key press
     * behaviors. Accelerators should be used instead when overrides are not needed.
     *
     * @return true if an override behavior for this key was found, false otherwise
     */
    public boolean onKeyPressed(KeyEvent keyEvent) {
        if (new KeyCodeCombination(KeyCode.EQUALS, SHORTCUT_DOWN).match(keyEvent)) {
            zoomInH(null);
            return true;
        } else if (new KeyCodeCombination(KeyCode.MINUS, SHORTCUT_DOWN).match(keyEvent)) {
            zoomOutH(null);
            return true;
        } else if (new KeyCodeCombination(KeyCode.MINUS, SHIFT_DOWN, KeyCombination.SHORTCUT_ANY)
                .match(keyEvent)) {
            zoomOutV(null);
            return true;
        } else if (!tabs.getTabs().isEmpty()) {
            Tab curTab = tabs.getSelectionModel().getSelectedItem();
            return editors.get(curTab.getId()).onKeyPressed(keyEvent);
        }
        // No need to override default key behavior.
        return false;

    }

    /**
     * Called whenever the mouse wheel is used to scroll, Can override default scroll
     * behaviors.
     *
     * @return true if an override behavior was found, false otherwise
     */
    public boolean OnScroll(ScrollEvent scrollEvent) {
        if (!scrollEvent.isShortcutDown()) return false;
        double value = scrollEvent.getDeltaY();
        if (scrollEvent.isAltDown()) {
            if (value < 0) {
                zoomOutH(null);
            } else if (value > 0) {
                zoomInH(null);
            }
        } else {
            if (value < 0) {
                zoomOutV(null);
            } else if (value > 0) {
                zoomInV(null);
            }
        }
        return true;
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
        String fileName = editors.get(tab.getId()).getOpenFile().getName();
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
                    // Enable or disable menu items depending on which tab is selected.
                    editors.get(tabId).getMenuItems().bindProperties(
                            saveItem.disableProperty(),
                            saveAsItem.disableProperty(),
                            exportToWavItem.disableProperty(),
                            undoItem.disableProperty(),
                            redoItem.disableProperty(),
                            cutItem.disableProperty(),
                            copyItem.disableProperty(),
                            pasteItem.disableProperty(),
                            deleteItem.disableProperty(),
                            notePropertiesItem.disableProperty(),
                            propertiesItem.disableProperty(),
                            portamentoEditorItem.disableProperty(),
                            vibratoEditorItem.disableProperty(),
                            envelopeEditorItem.disableProperty());
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
            EditorController editor = loader.getController();
            editors.put(tab.getId(), editor);
            editor.openEditor(new EditorCallback() {
                @Override
                public void markChanged(boolean hasUnsavedChanges) {
                    if (hasUnsavedChanges) {
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
                }

                @Override
                public void openVoicebank(File location) {
                    Tab newTab = createEditor(EditorType.VOICEBANK);
                    try {
                        editors.get(newTab.getId()).open(location);
                        newTab.setText(location.getName());
                    } catch (FileAlreadyOpenException e) {
                        switchToExistingFile(e.getAlreadyOpenFile());
                        closeTab(newTab);
                    }
                }

                @Override
                public void openVoicebank(File location, String trueLyric) {
                    this.openVoicebank(location);
                    Tab curTab = tabs.getSelectionModel().getSelectedItem();

                    // Highlight lyric after a short pause for viewport to establish itself.
                    PauseTransition briefPause = new PauseTransition(Duration.millis(10));
                    briefPause.setOnFinished(event ->
                            editors.get(curTab.getId()).showLyricConfig(trueLyric));
                    briefPause.play();
                }

                @Override
                public BooleanProperty getCheckboxValue(CheckboxType checkboxType) {
                    switch (checkboxType) {
                        case SHOW_LYRICS:
                            return showLyricsItem.selectedProperty();
                        case SHOW_ALIASES:
                            return showAliasesItem.selectedProperty();
                        case SHOW_PITCHBENDS:
                            return showPitchbendsItem.selectedProperty();
                        default:
                            throw new IllegalArgumentException("Unknown checkbox type.");
                    }
                }
            });
            editor.refreshView();
            tab.setText(editor.getOpenFile().getName()); // Uses file name for tab name.

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
        try {
            Optional<String> songName = editors.get(newTab.getId()).open();
            if (songName.isPresent()) {
                newTab.setText(songName.get());
            } else {
                closeTab(newTab);
            }
        } catch (FileAlreadyOpenException e) {
            statusBar.setStatus("Error: Cannot have the same file open in two tabs.");
            switchToExistingFile(e.getAlreadyOpenFile());
            closeTab(newTab);
        }
    }

    @FXML
    void openVoicebank(ActionEvent event) {
        Tab newTab = createEditor(EditorType.VOICEBANK);
        try {
            Optional<String> voicebankName = editors.get(newTab.getId()).open();
            if (voicebankName.isPresent()) {
                newTab.setText(voicebankName.get());
            } else {
                closeTab(newTab);
            }
        } catch (FileAlreadyOpenException e) {
            statusBar.setStatus("Error: Cannot have the same file open in two tabs.");
            switchToExistingFile(e.getAlreadyOpenFile());
            closeTab(newTab);
        }
    }

    /**
     * If a tab is already open to this file, switch to that tab.
     */
    private void switchToExistingFile(File existingFile) {
        for (Tab tab : tabs.getTabs()) {
            String tabId = tab.getId();
            if (editors.get(tabId).getOpenFile().equals(existingFile)) {
                tabs.getSelectionModel().select(tab);
                break;
            }
        }
    }

    private void closeTab(Tab tab) {
        // Close a tab through code instead of user action.
        tab.getOnClosed().handle(null);
        tabs.getTabs().remove(tab);
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
    void openPreferences(ActionEvent event) {
        // Open preferences modal.
        InputStream fxml = getClass().getResourceAsStream("/fxml/PreferencesScene.fxml");
        FXMLLoader loader = fxmlLoaderProvider.get();
        try {
            Stage currentStage = (Stage) tabs.getScene().getWindow();
            Stage preferencesWindow = new Stage();
            preferencesWindow.setTitle(localizer.getMessage("preferences.title"));
            preferencesWindow.initModality(Modality.APPLICATION_MODAL);
            preferencesWindow.initOwner(currentStage);
            Scene scene = new Scene(loader.load(fxml));
            themeManager.applyToScene(scene);
            preferencesWindow.setScene(scene);
            // Set up an event that runs when the program is closed.
            PreferencesController controller = loader.getController();
            preferencesWindow.setOnCloseRequest(windowEvent -> {
                if (!controller.onCloseWindow()) {
                    windowEvent.consume();
                }
            });
            preferencesWindow.showAndWait();
        } catch (IOException e) {
            statusBar.setStatus("Error: Unable to open preferences.");
            errorLogger.logError(e);
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
    void openNoteProperties(ActionEvent event) {
        if (!tabs.getTabs().isEmpty()) {
            editors.get(tabs.getSelectionModel().getSelectedItem().getId()).openNoteProperties();
        }
    }

    @FXML
    void zoomInH(ActionEvent event) {
        changeHorizontalScale(scaler.getHorizontalRank() + 1);
    }

    @FXML
    void zoomOutH(ActionEvent event) {
        changeHorizontalScale(scaler.getHorizontalRank() - 1);
    }

    private void changeHorizontalScale(int newRank) {
        if (!scaler.changeHorizontalScale(scaler.getHorizontalRank(), newRank)) {
            return;
        }
        zoomOutHorizontallyItem.setDisable(newRank <= 0);
        zoomInHorizontallyItem.setDisable(newRank >= DiscreteScaler.HORIZONTAL_SCALES.size() - 1);
        for (Tab tab : tabs.getTabs()) {
            editors.get(tab.getId()).refreshView();
        }
    }

    @FXML
    void zoomInV(ActionEvent event) {
        changeVerticalScale(scaler.getVerticalRank() + 1);
    }

    @FXML
    void zoomOutV(ActionEvent event) {
        changeVerticalScale(scaler.getVerticalRank() - 1);
    }

    private void changeVerticalScale(int newRank) {
        if (!scaler.changeVerticalScale(scaler.getVerticalRank(), newRank)) {
            return;
        }
        zoomOutVerticallyItem.setDisable(newRank <= 0);
        zoomInVerticallyItem.setDisable(newRank >= DiscreteScaler.VERTICAL_SCALES.size() - 1);
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
    void openBulkPortamentoEditor(ActionEvent event) {
        if (!tabs.getTabs().isEmpty()) {
            String id = tabs.getSelectionModel().getSelectedItem().getId();
            editors.get(id).openBulkEditor(BulkEditorType.PORTAMENTO);
        }
    }

    @FXML
    void openBulkVibratoEditor(ActionEvent event) {
        if (!tabs.getTabs().isEmpty()) {
            String id = tabs.getSelectionModel().getSelectedItem().getId();
            editors.get(id).openBulkEditor(BulkEditorType.VIBRATO);
        }
    }

    @FXML
    void openBulkEnvelopeEditor(ActionEvent event) {
        if (!tabs.getTabs().isEmpty()) {
            String id = tabs.getSelectionModel().getSelectedItem().getId();
            editors.get(id).openBulkEditor(BulkEditorType.ENVELOPE);
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
