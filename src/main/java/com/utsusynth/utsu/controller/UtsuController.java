package com.utsusynth.utsu.controller;

import static javafx.scene.input.KeyCombination.CONTROL_DOWN;
import static javafx.scene.input.KeyCombination.SHIFT_DOWN;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.utsusynth.utsu.common.exception.ErrorLogger;
import com.utsusynth.utsu.common.i18n.Localizable;
import com.utsusynth.utsu.common.i18n.Localizer;
import com.utsusynth.utsu.common.quantize.Scaler;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.layout.BorderPane;

/**
 * 'UtsuScene.fxml' Controller Class
 */
public class UtsuController implements Localizable {
    private static final ErrorLogger errorLogger = ErrorLogger.getLogger();

    private enum EditorType {
        SONG, VOICEBANK,
    }

    // User session data goes here.
    private TabPane tabs;
    private final Map<String, EditorController> editors;

    // Helper classes go here.
    private final Localizer localizer;
    private final Scaler scaler;
    private final Provider<FXMLLoader> fxmlLoaderProvider;

    @FXML
    private BorderPane rootPane;

    @Inject
    public UtsuController(Localizer localizer, Scaler scaler, Provider<FXMLLoader> fxmlLoaders) {
        this.localizer = localizer;
        this.scaler = scaler;
        this.fxmlLoaderProvider = fxmlLoaders;

        this.editors = new HashMap<>();
    }

    // Provide setup for other controllers.
    // This is called automatically.
    public void initialize() {
        // Initialize tabs.
        tabs = new TabPane();
        rootPane.setCenter(tabs);

        // Create an empty song editor.
        createEditor(EditorType.SONG);

        // Set up localization.
        localizer.localize(this);
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
    private Menu editMenu; // Value injected by FXMLLoader
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
        saveItem.setText(bundle.getString("menu.file.saveFile"));
        saveItem.setAccelerator(new KeyCodeCombination(KeyCode.S, CONTROL_DOWN));
        saveAsItem.setText(bundle.getString("menu.file.saveFileAs"));
        saveAsItem.setAccelerator(new KeyCodeCombination(KeyCode.S, CONTROL_DOWN, SHIFT_DOWN));
        editMenu.setText(bundle.getString("menu.edit"));
        viewMenu.setText(bundle.getString("menu.view"));
        zoomInItem.setText(bundle.getString("menu.view.zoomIn"));
        zoomInItem.setAccelerator(new KeyCodeCombination(KeyCode.EQUALS, CONTROL_DOWN));
        zoomOutItem.setText(bundle.getString("menu.view.zoomOut"));
        zoomOutItem.setAccelerator(new KeyCodeCombination(KeyCode.MINUS, CONTROL_DOWN));
        projectMenu.setText(bundle.getString("menu.project"));
        propertiesItem.setText(bundle.getString("menu.project.properties"));
        helpMenu.setText(bundle.getString("menu.help"));
        helpMenu.setAccelerator(new KeyCodeCombination(KeyCode.SLASH, CONTROL_DOWN, SHIFT_DOWN));
        aboutItem.setText(bundle.getString("menu.help.about"));

        // Force the menu to refresh.
        fileMenu.setVisible(false);
        fileMenu.setVisible(true);
    }

    /**
     * Called whenever Utsu is closed.
     * 
     * @return true if window should be closed, false otherwise
     */
    public boolean onCloseWindow() {
        // TODO: Replace with a save dialog. Also, add this to localizer.
        Alert alert = new Alert(
                AlertType.CONFIRMATION,
                "Are you sure you want to exit Utsu?  Any unsaved changes will be lost.");
        Optional<ButtonType> result = Optional.fromJavaUtil(alert.showAndWait());
        if (result.isPresent() && result.get() == ButtonType.OK) {
            return true;
        }
        return false;
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
            Tab tab = new Tab("*Untitled", loader.load(fxml));
            String tabId = "tab" + new Date().getTime(); // Use current timestamp for id.
            tab.setId(tabId);
            tab.setOnSelectionChanged(event -> {
                if (tab.isSelected()) {
                    if (type == EditorType.SONG) {
                        propertiesItem.setDisable(false);
                    } else {
                        propertiesItem.setDisable(true);
                    }
                }
            });
            tab.setOnClosed(event -> {
                this.editors.remove(tabId);
            });
            // Add and select new tab.
            tabs.getTabs().add(tab);
            tabs.getSelectionModel().select(tab);

            EditorController editor = (EditorController) loader.getController();
            editors.put(tab.getId(), editor);
            editor.openEditor(new EditorCallback() {
                @Override
                public void enableSave(boolean enabled) {
                    saveItem.setDisable(!enabled);
                    // Adds a handy * to indicate unsaved files.
                    if (enabled && !tab.getText().startsWith("*")) {
                        tab.setText("*" + tab.getText());
                    }
                }
            });
            editor.refreshView();
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
        String newName = editors.get(newTab.getId()).openFile();
        newTab.setText(newName);
    }

    @FXML
    void openVoicebank(ActionEvent event) {
        Tab newTab = createEditor(EditorType.VOICEBANK);
        String newName = editors.get(newTab.getId()).openFile();
        newTab.setText(newName);
    }

    @FXML
    void saveFile(ActionEvent event) {
        if (!tabs.getTabs().isEmpty()) {
            Tab curTab = tabs.getSelectionModel().getSelectedItem();
            String newName = editors.get(curTab.getId()).saveFile();
            curTab.setText(newName);
        }
    }

    @FXML
    void saveFileAs(ActionEvent event) {
        if (!tabs.getTabs().isEmpty()) {
            Tab curTab = tabs.getSelectionModel().getSelectedItem();
            String newName = editors.get(curTab.getId()).saveFileAs();
            curTab.setText(newName);
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
}
