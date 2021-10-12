package com.utsusynth.utsu.controller.song;

import com.google.common.base.Function;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.utsusynth.utsu.common.RegionBounds;
import com.utsusynth.utsu.common.StatusBar;
import com.utsusynth.utsu.common.data.*;
import com.utsusynth.utsu.common.enums.FilterType;
import com.utsusynth.utsu.common.exception.ErrorLogger;
import com.utsusynth.utsu.common.exception.FileAlreadyOpenException;
import com.utsusynth.utsu.common.i18n.Localizable;
import com.utsusynth.utsu.common.i18n.Localizer;
import com.utsusynth.utsu.common.quantize.Quantizer;
import com.utsusynth.utsu.common.quantize.Scaler;
import com.utsusynth.utsu.controller.EditorCallback;
import com.utsusynth.utsu.controller.EditorController;
import com.utsusynth.utsu.controller.UtsuController.CheckboxType;
import com.utsusynth.utsu.controller.common.IconManager;
import com.utsusynth.utsu.controller.common.MenuItemManager;
import com.utsusynth.utsu.controller.common.UndoService;
import com.utsusynth.utsu.controller.song.BulkEditorController.BulkEditorType;
import com.utsusynth.utsu.controller.song.LyricEditorController.LyricEditorType;
import com.utsusynth.utsu.engine.Engine;
import com.utsusynth.utsu.engine.ExternalProcessRunner;
import com.utsusynth.utsu.files.ThemeManager;
import com.utsusynth.utsu.files.song.Ust12Reader;
import com.utsusynth.utsu.files.song.Ust12Writer;
import com.utsusynth.utsu.files.song.Ust20Reader;
import com.utsusynth.utsu.files.song.Ust20Writer;
import com.utsusynth.utsu.model.song.NoteIterator;
import com.utsusynth.utsu.model.song.SongContainer;
import com.utsusynth.utsu.view.song.Piano;
import com.utsusynth.utsu.view.song.SongCallback;
import com.utsusynth.utsu.view.song.SongEditor;
import com.utsusynth.utsu.view.song.track.TrackItemSet;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.util.*;
import java.util.stream.Collectors;

import static javafx.scene.input.KeyCombination.SHORTCUT_DOWN;

/**
 * 'SongScene.fxml' Controller Class
 */
public class SongController implements EditorController, Localizable {
    private static final ErrorLogger errorLogger = ErrorLogger.getLogger();

    // User session data goes here.
    private EditorCallback callback;

    // Helper classes go here.
    private final SongContainer song;
    private final Engine engine;
    private final SongEditor songEditor;
    private final Piano piano;
    private final Localizer localizer;
    private final Quantizer quantizer;
    private final Scaler scaler;
    private final UndoService undoService;
    private final MenuItemManager menuItemManager;
    private final StatusBar statusBar;
    private final Ust12Reader ust12Reader;
    private final Ust20Reader ust20Reader;
    private final Ust12Writer ust12Writer;
    private final Ust20Writer ust20Writer;
    private final IconManager iconManager;
    private final ThemeManager themeManager;
    private final ExternalProcessRunner processRunner;
    private final Provider<FXMLLoader> fxmlLoaderProvider;

    @FXML // fx:id="scrollPaneLeft"
    private ScrollPane scrollPaneLeft; // Value injected by FXMLLoader

    @FXML // fx:id="anchorLeft"
    private AnchorPane anchorLeft; // Value injected by FXMLLoader

    @FXML // fx:id="anchorCenter"
    private AnchorPane anchorCenter; // Value injected by FXMLLoader

    @FXML // fx:id="scrollPaneCenter"
    private ScrollPane scrollPaneCenter; // Value injected by FXMLLoader

    @FXML // fx:id="scrollPaneBottom"
    private ScrollPane scrollPaneBottom; // Value injected by FXMLLoader

    @FXML // fx:id="anchorBottom"
    private AnchorPane anchorBottom; // Value injected by FXMLLoader

    @FXML // fx:id="voicebankImage"
    private ImageView voicebankImage; // Value injected by FXMLLoader

    @FXML // fx:id="rewindIcon"
    private AnchorPane rewindIcon; // Value injected by FXMLLoader

    @FXML // fx:id="playPauseIcon"
    private AnchorPane playPauseIcon; // Value injected by FXMLLoader

    @FXML // fx:id="stopIcon"
    private AnchorPane stopIcon; // Value injected by FXMLLoader

    @FXML // fx:id="quantizeChoiceBox"
    private ChoiceBox<String> quantizeChoiceBox; // Value injected by FXMLLoader

    @Inject
    public SongController(
            SongContainer songContainer, // Inject an empty song.
            Engine engine,
            SongEditor songEditor,
            Piano piano,
            Localizer localizer,
            Quantizer quantizer,
            Scaler scaler,
            UndoService undoService,
            MenuItemManager menuItemManager,
            StatusBar statusBar,
            Ust12Reader ust12Reader,
            Ust20Reader ust20Reader,
            Ust12Writer ust12Writer,
            Ust20Writer ust20Writer,
            IconManager iconManager,
            ThemeManager themeManager,
            ExternalProcessRunner processRunner,
            Provider<FXMLLoader> fxmlLoaders) {
        this.song = songContainer;
        this.engine = engine;
        this.songEditor = songEditor;
        this.piano = piano;
        this.localizer = localizer;
        this.quantizer = quantizer;
        this.scaler = scaler;
        this.undoService = undoService;
        this.menuItemManager = menuItemManager;
        this.statusBar = statusBar;
        this.ust12Reader = ust12Reader;
        this.ust20Reader = ust20Reader;
        this.ust12Writer = ust12Writer;
        this.ust20Writer = ust20Writer;
        this.iconManager = iconManager;
        this.themeManager = themeManager;
        this.processRunner = processRunner;
        this.fxmlLoaderProvider = fxmlLoaders;
    }

    // Provide setup for other frontend song management.
    // This is called automatically when fxml loads.
    public void initialize() {
        songEditor.initialize(new SongCallback() {
            @Override
            public void addNotes(List<NoteData> toAdd) {
                onSongChange();
                song.get().addNotes(toAdd);
            }

            @Override
            public MutateResponse removeNotes(Set<Integer> positions) {
                onSongChange();
                return song.get().removeNotes(positions);
            }

            @Override
            public NoteUpdateData modifyNote(NoteData toModify) {
                onSongChange();
                return song.get().modifyNote(toModify);
            }

            @Override
            public MutateResponse standardizeNotes(int firstPos, int lastPos) {
                // Only called in response to other changes, so this does not trigger onSongChange.
                return song.get().standardizeNotes(firstPos, lastPos);
            }

            @Override
            public void recordAction(Runnable redoAction, Runnable undoAction) {
                undoService.setMostRecentAction(redoAction, undoAction);
            }

            @Override
            public void openNoteProperties(RegionBounds regionBounds) {
                openNotePropertiesEditor(regionBounds);
            }

            @Override
            public void openLyricConfig(int position) {
                NoteData noteData = song.get().getNote(position);
                Optional<String> trueLyric = noteData.getTrueLyric();
                if (trueLyric.isEmpty() || trueLyric.get().isEmpty()) {
                    String displayLyric = noteData.getLyric();
                    statusBar.setText("Error: no lyric config for \"" + displayLyric + "\"");
                    return;
                }
                callback.openVoicebank(song.get().getVoiceDir(), trueLyric.get());
            }

            @Override
            public void clearCache(int firstPos, int lastPos) {
                // Only clears cache without making changes, so does not trigger onSongChange.
                song.get().clearNoteCache(firstPos, lastPos);
            }

            @Override
            public BooleanProperty getCheckboxValue(CheckboxType checkboxType) {
                return callback.getCheckboxValue(checkboxType);
            }
        });
        scrollPaneLeft.setVvalue(0.5);
        scrollPaneCenter.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPaneCenter.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPaneBottom.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        // Context menu for voicebank icon.
        ContextMenu iconContextMenu = new ContextMenu();
        MenuItem openVoicebankItem = new MenuItem("Open Voicebank");
        openVoicebankItem.setOnAction(event -> callback.openVoicebank(song.get().getVoiceDir()));
        iconContextMenu.getItems().add(openVoicebankItem);
        iconContextMenu.setOnShowing(event -> {
            openVoicebankItem.setText(localizer.getMessage("song.openCurrentVoicebank"));
        });
        voicebankImage.setOnContextMenuRequested((event -> {
            iconContextMenu.hide();
            iconContextMenu.show(voicebankImage, event.getScreenX(), event.getScreenY());
        }));

        quantizeChoiceBox.setItems(FXCollections.observableArrayList(
                "1/4",
                "1/6",
                "1/8",
                "1/12",
                "1/16",
                "1/24",
                "1/32",
                "1/48",
                "1/64",
                "1/96",
                "1/128",
                "1/192",
                "--"));
        quantizeChoiceBox.setOnAction((action) -> {
            String quantization = quantizeChoiceBox.getValue();
            switch (quantization) {
                case "1/4":
                    quantizer.changeQuant(quantizer.getQuant(), 480);
                    break;
                case "1/6":
                    quantizer.changeQuant(quantizer.getQuant(), 320);
                    break;
                case "1/8":
                    quantizer.changeQuant(quantizer.getQuant(), 240);
                    break;
                case "1/12":
                    quantizer.changeQuant(quantizer.getQuant(), 160);
                    break;
                case "1/16":
                    quantizer.changeQuant(quantizer.getQuant(), 120);
                    break;
                case "1/24":
                    quantizer.changeQuant(quantizer.getQuant(), 80);
                    break;
                case "1/32":
                    quantizer.changeQuant(quantizer.getQuant(), 60);
                    break;
                case "1/48":
                    quantizer.changeQuant(quantizer.getQuant(), 40);
                    break;
                case "1/64":
                    quantizer.changeQuant(quantizer.getQuant(), 30);
                    break;
                case "1/96":
                    quantizer.changeQuant(quantizer.getQuant(), 20);
                    break;
                case "1/128":
                    quantizer.changeQuant(quantizer.getQuant(), 15);
                    break;
                case "1/192":
                    quantizer.changeQuant(quantizer.getQuant(), 10);
                    break;
                case "--":
                    quantizer.changeQuant(quantizer.getQuant(), 1);
            }
        });
        quantizeChoiceBox.setValue("1/16");

        iconManager.setRewindIcon(rewindIcon);
        rewindIcon.setOnMousePressed(event -> iconManager.selectIcon(rewindIcon));
        rewindIcon.setOnMouseReleased(event -> iconManager.deselectIcon(rewindIcon));
        iconManager.setPlayIcon(playPauseIcon);
        playPauseIcon.setOnMousePressed(event -> iconManager.selectIcon(playPauseIcon));
        playPauseIcon.setOnMouseReleased(event -> iconManager.deselectIcon(playPauseIcon));
        iconManager.setStopIcon(stopIcon);
        stopIcon.setOnMousePressed(event -> iconManager.selectIcon(stopIcon));
        stopIcon.setOnMouseReleased(event -> iconManager.deselectIcon(stopIcon));

        refreshView();

        // Set up enabled/disabled menu items.
        menuItemManager.initializeSong(
                undoService.canUndoProperty(),
                undoService.canRedoProperty(),
                songEditor.isAnythingSelectedProperty(),
                songEditor.clipboardFilledProperty());

        // Do scrolling after a short pause for viewport to establish itself.
        PauseTransition briefPause = new PauseTransition(Duration.millis(50));
        briefPause.setOnFinished(event -> {
            songEditor.scrollToPosition(0);
        });
        briefPause.play();

        // Set up localization.
        localizer.localize(this);
    }

    @FXML
    private Label quantizationLabel; // Value injected by FXMLLoader

    @Override
    public void localize(ResourceBundle bundle) {
        quantizationLabel.setText(bundle.getString("song.quantization"));
    }

    @Override
    public void refreshView() {
        // Set song image.
        try {
            Image image = new Image("file:" + song.get().getVoicebank().getImagePath());
            voicebankImage.setImage(image);
        } catch (Exception e) {
            System.out.println("Exception while loading voicebank image.");
            errorLogger.logWarning(e);
        }

        anchorLeft.getChildren().clear();
        anchorLeft.getChildren().add(piano.initPiano());

        // Cross-editor bindings.
        ListView<TrackItemSet> noteTrack = songEditor.createNewTrack(song.get().getNotes());
        noteTrack.prefWidthProperty().bind(scrollPaneCenter.widthProperty());
        noteTrack.prefHeightProperty().bind(scrollPaneCenter.heightProperty());
        ListView<TrackItemSet> dynamicsTrack = songEditor.getDynamicsElement();
        dynamicsTrack.prefWidthProperty().bind(
                scrollPaneBottom.widthProperty().subtract(Quantizer.SCROLL_BAR_WIDTH));

        // Scrollbar bindings, after scrollbars are generated.
        PauseTransition briefPause = new PauseTransition(Duration.millis(20));
        briefPause.setOnFinished(event -> {
            for (Node node : noteTrack.lookupAll(".scroll-bar")) {
                if (!(node instanceof ScrollBar)) {
                    continue;
                }
                ScrollBar scrollBar = (ScrollBar) node;
                if (scrollBar.getOrientation() == Orientation.VERTICAL) {
                    // TODO: Call this only once.
                    scrollBar.setPrefWidth(Quantizer.SCROLL_BAR_WIDTH);
                    scrollBar.setValue(scrollPaneLeft.getVvalue() * scrollBar.getMax());
                    scrollPaneLeft.vvalueProperty().addListener((obs, oldValue, newValue) -> {
                        if (!oldValue.equals(newValue)) {
                            scrollBar.setValue(newValue.doubleValue() * scrollBar.getMax());
                        }
                    });
                    scrollBar.valueProperty().addListener((obs, oldValue, newValue) -> {
                        if (!oldValue.equals(newValue)) {
                            scrollPaneLeft.setVvalue(newValue.doubleValue() / scrollBar.getMax());
                        }
                    });
                } else if (scrollBar.getOrientation() == Orientation.HORIZONTAL) {
                    scrollBar.setPrefHeight(Quantizer.SCROLL_BAR_WIDTH);
                    for (Node dynamicsNode : dynamicsTrack.lookupAll(".scroll-bar")) {
                        if (!(dynamicsNode instanceof ScrollBar)) {
                            continue;
                        }
                        ScrollBar dynamicsScrollBar = (ScrollBar) dynamicsNode;
                        if (dynamicsScrollBar.getOrientation() == Orientation.HORIZONTAL) {
                            dynamicsScrollBar.valueProperty().bindBidirectional(
                                    scrollBar.valueProperty());
                        }
                    }
                }
            }
        });
        briefPause.play();

        // Reloads current song.
        anchorCenter.getChildren().clear();
        anchorCenter.getChildren().add(noteTrack);
        anchorBottom.getChildren().clear();
        anchorBottom.getChildren().add(dynamicsTrack);
    }

    @Override
    public void openEditor(EditorCallback callback) {
        this.callback = callback;
    }

    @Override
    public void closeEditor() {
        // Stop any ongoing playback.
        engine.stopPlayback();
        // Clear any remaining cache files.
        song.get().clearAllCacheValues();
        // Remove this song from local memory.
        song.removeSong();
    }

    @Override
    public File getOpenFile() {
        return song.getLocation();
    }

    @Override
    public MenuItemManager getMenuItems() {
        return menuItemManager;
    }

    @Override
    public boolean onKeyPressed(KeyEvent keyEvent) {
        if (new KeyCodeCombination(KeyCode.SPACE).match(keyEvent)) {
            // In the pause case, flicker will be overridden before it can happen.
            flickerIcon(playPauseIcon);
            playOrPause();
            return true;
        } else if (new KeyCodeCombination(KeyCode.SPACE, SHORTCUT_DOWN).match(keyEvent)) {
            flickerIcon(playPauseIcon);
            stopPlayback();
            startPlayback();
            return true;
        } else if (new KeyCodeCombination(KeyCode.V).match(keyEvent)
                || new KeyCodeCombination(KeyCode.W).match(keyEvent)) {
            flickerIcon(rewindIcon);
            rewindPlayback(); // Rewind button's event handler.
            return true;
        } else if (new KeyCodeCombination(KeyCode.B).match(keyEvent)) {
            flickerIcon(stopIcon);
            stopPlayback(); // Stop button's event handler.
            return true;
        } else if (new KeyCodeCombination(KeyCode.BACK_SPACE).match(keyEvent)) {
            songEditor.deleteSelected();
            return true;
        } else if (new KeyCodeCombination(KeyCode.ENTER).match(keyEvent)) {
            Optional<Integer> focusNote = songEditor.getFocusNote();
            if (focusNote.isPresent()) {
                if (!songEditor.getVisibleTrack().contains(focusNote.get())) {
                    songEditor.scrollToPosition(focusNote.get());
                }
                songEditor.openLyricInput(focusNote.get());
            }
            return true;
        } else if (new KeyCodeCombination(KeyCode.TAB).match(keyEvent)
                || new KeyCodeCombination(KeyCode.RIGHT).match(keyEvent)) {
            Optional<Integer> focusNote = songEditor.getFocusNote();
            if (focusNote.isPresent()) {
                Optional<Integer> newFocus = song.get().getNextNote(focusNote.get());
                if (newFocus.isPresent()) {
                    if (!songEditor.getVisibleTrack().contains(newFocus.get())) {
                        songEditor.scrollToPosition(newFocus.get());
                    }
                    songEditor.focusOnNote(newFocus.get());
                }
            } else if (song.get().getNoteIterator().hasNext()) {
                int positionMs = song.get().getNoteIterator().next().getDelta();
                if (!songEditor.getVisibleTrack().contains(positionMs)) {
                    songEditor.scrollToPosition(positionMs);
                }
                songEditor.focusOnNote(positionMs);
            }
            return true;
        } else if (new KeyCodeCombination(KeyCode.LEFT).match(keyEvent)
                || new KeyCodeCombination(KeyCode.TAB, KeyCombination.SHIFT_DOWN).match(keyEvent)) {
            Optional<Integer> focusNote = songEditor.getFocusNote();
            if (focusNote.isPresent()) {
                Optional<Integer> newFocus = song.get().getPrevNote(focusNote.get());
                if (newFocus.isPresent()) {
                    if (!songEditor.getVisibleTrack().contains(newFocus.get())) {
                        songEditor.scrollToPosition(newFocus.get());
                    }
                    songEditor.focusOnNote(newFocus.get());
                }
            } else if (song.get().getNoteIterator().hasNext()) {
                int positionMs = song.get().getNoteIterator().next().getDelta();
                if (!songEditor.getVisibleTrack().contains(positionMs)) {
                    songEditor.scrollToPosition(positionMs);
                }
                songEditor.focusOnNote(positionMs);
            }
            return true;
        } else {
            // No need to override default key behavior.
            return false;
        }
    }

    /**
     * Quickly selects an icon, then changes it back.
     */
    private void flickerIcon(AnchorPane icon) {
        iconManager.selectIcon(icon);
        PauseTransition briefPause = new PauseTransition(Duration.millis(120));
        briefPause.setOnFinished(event -> {
            iconManager.deselectIcon(icon);
        });
        briefPause.play();
    }

    @Override
    public Optional<String> open() throws FileAlreadyOpenException {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select UST File");
        fc.getExtensionFilters().addAll(
                new ExtensionFilter("UST files", "*.ust"),
                new ExtensionFilter("All files", "*.*"));
        File file = fc.showOpenDialog(null);
        if (file != null) {
            open(file);
            return Optional.of(file.getName());
        }
        return Optional.empty();
    }

    @Override
    public void open(File file) throws FileAlreadyOpenException {
        song.setLocation(file);
        statusBar.setText("Opening " + file.getName() + "...");
        new Thread(() -> {
            try {
                String saveFormat; // Format to save this song in the future.
                String charset = "UTF-8";
                CharsetDecoder utf8Decoder = StandardCharsets.UTF_8.newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT);
                try {
                    utf8Decoder.decode(ByteBuffer.wrap(FileUtils.readFileToByteArray(file)));
                } catch (MalformedInputException | UnmappableCharacterException e) {
                    charset = "SJIS";
                }
                String content = FileUtils.readFileToString(file, charset);
                if (content.contains("UST Version1.2")) {
                    song.setSong(ust12Reader.loadSong(content));
                    saveFormat = "UST 1.2 (Shift JIS)";
                } else if (content.contains("UST Version2.0")) {
                    song.setSong(ust20Reader.loadSong(content));
                    saveFormat =
                            "UST 2.0 " + (charset.equals("UTF-8") ? "(UTF-8)" : "(Shift JIS)");
                } else {
                    // If no version found, assume UST 1.2 for now.
                    song.setSong(ust12Reader.loadSong(content));
                    saveFormat = "UST 1.2 (Shift JIS)";
                }
                undoService.clearActions();
                song.setSaveFormat(saveFormat);
                Platform.runLater(() -> {
                    refreshView();
                    callback.markChanged(false);
                    menuItemManager.disableSave();
                    statusBar.setText("Opened " + file.getName());
                    // Do scrolling after a short pause for viewport to establish itself.
                    PauseTransition briefPause = new PauseTransition(Duration.millis(10));
                    briefPause.setOnFinished(event -> songEditor.scrollToPosition(0));
                    briefPause.play();
                });
            } catch (Exception e) {
                Platform.runLater(
                        () -> statusBar.setText("Error: Unable to open " + file.getName()));
                errorLogger.logError(e);
            }
        }).start();
    }

    @Override
    public Optional<String> save() {
        if (song.hasPermanentLocation()) {
            String saveFormat = song.getSaveFormat();
            String charset = saveFormat.contains("Shift JIS") ? "SJIS" : "UTF-8";
            File saveLocation = song.getLocation();
            statusBar.setText("Saving...");
            new Thread(() -> {
                try (PrintStream ps = new PrintStream(saveLocation, charset)) {
                    if (saveFormat.contains("UST 1.2")) {
                        ust12Writer.writeSong(song.get(), ps);
                    } else {
                        ust20Writer.writeSong(song.get(), ps, charset);
                    }
                    ps.flush();
                    ps.close();
                    // Report results to UI.
                    Platform.runLater(() -> {
                        callback.markChanged(false);
                        menuItemManager.disableSave();
                        statusBar.setText("Saved changes to " + saveLocation.getName());
                    });
                } catch (Exception e) {
                    Platform.runLater(
                            () -> statusBar
                                    .setText("Error: Unable to save " + saveLocation.getName()));
                    errorLogger.logError(e);
                }
            }).start();
            return Optional.empty();
        } else {
            // Default to "Save As" if no permanent location found.
            return saveAs();
        }
    }

    @Override
    public Optional<String> saveAs() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select UST File");
        if (System.getProperty("os.name").toLowerCase().contains("mac")) {
            fc.getExtensionFilters().addAll(
                    new ExtensionFilter("UST 2.0 (UTF-8)", "*.ust"),
                    new ExtensionFilter("UST 2.0 (Shift JIS)", "*.ust"),
                    new ExtensionFilter("UST 1.2 (Shift JIS)", "*.ust"));
        } else {
            // For now, default to 1.2 format for Windows and Linux users.
            fc.getExtensionFilters().addAll(
                    new ExtensionFilter("UST 1.2 (Shift JIS)", "*.ust"),
                    new ExtensionFilter("UST 2.0 (UTF-8)", "*.ust"),
                    new ExtensionFilter("UST 2.0 (Shift JIS)", "*.ust"));
        }
        File file = fc.showSaveDialog(null);
        if (file != null) {
            statusBar.setText("Saving...");
            try {
                song.setLocation(file);
            } catch (FileAlreadyOpenException e) {
                statusBar.setText("Error: Cannot have the same file open in two tabs.");
                return Optional.empty();
            }
            ExtensionFilter chosenFormat = fc.getSelectedExtensionFilter();
            String charset = chosenFormat.getDescription().contains("Shift JIS") ? "SJIS" : "UTF-8";
            new Thread(() -> {
                try (PrintStream ps = new PrintStream(file, charset)) {
                    if (chosenFormat.getDescription().contains("UST 1.2")) {
                        ust12Writer.writeSong(song.get(), ps);
                    } else {
                        ust20Writer.writeSong(song.get(), ps, charset);
                    }
                    ps.flush();
                    ps.close();
                    // Report results to UI.
                    song.setSaveFormat(chosenFormat.getDescription());
                    Platform.runLater(() -> {
                        callback.markChanged(false);
                        menuItemManager.disableSave();
                        statusBar.setText("Saved as " + file.getName());
                    });
                } catch (Exception e) {
                    Platform.runLater(
                            () -> statusBar
                                    .setText("Error: Unable to save as " + file.getName()));
                    errorLogger.logError(e);
                }
            }).start();
            // File name may have changed, so just return new file name.
            return Optional.of(file.getName());
        }
        return Optional.empty();
    }

    /**
     * Called whenever a Song is changed.
     */
    private void onSongChange() {
        song.get().clearCache(); // Invalidate rendered song cache.
        if (callback != null) {
            callback.markChanged(true);
        }
        if (song.hasPermanentLocation()) {
            menuItemManager.enableSave();
        } else {
            menuItemManager.disableSave();
        }
    }

    /**
     * Allows users to alter properties of individual notes and note regions.
     */
    private void openNotePropertiesEditor(RegionBounds regionBounds) {
        // Open note properties modal.
        Stage currentStage = (Stage) anchorCenter.getScene().getWindow();
        if (!currentStage.isFocused()) {
            return; // Only one modal at a time!
        }
        InputStream fxml = getClass().getResourceAsStream("/fxml/NotePropertiesScene.fxml");
        FXMLLoader loader = fxmlLoaderProvider.get();
        try {
            Stage propertiesWindow = new Stage();
            propertiesWindow.setTitle(localizer.getMessage("menu.edit.noteProperties"));
            propertiesWindow.initModality(Modality.APPLICATION_MODAL);
            propertiesWindow.initOwner(currentStage);
            BorderPane notePropertiesPane = loader.load(fxml);
            NotePropertiesController controller = loader.getController();
            controller.setData(song, regionBounds, (oldData, newData) -> {
                Runnable redoAction = () -> {
                    NoteIterator notes = song.get().getNoteIterator(regionBounds);
                    Iterator<NoteConfigData> newDataIterator = newData.iterator();
                    while (notes.hasNext() && newDataIterator.hasNext()) {
                        notes.next().setConfigData(newDataIterator.next());
                    }
                    onSongChange();
                    songEditor.selectRegion(regionBounds);
                    songEditor.refreshSelected();
                };
                Runnable undoAction = () -> {
                    NoteIterator notes = song.get().getNoteIterator(regionBounds);
                    Iterator<NoteConfigData> oldDataIterator = oldData.iterator();
                    while (notes.hasNext() && oldDataIterator.hasNext()) {
                        notes.next().setConfigData(oldDataIterator.next());
                    }
                    onSongChange();
                    songEditor.selectRegion(regionBounds);
                    songEditor.refreshSelected();
                };
                // Apply changes and save redo/undo for these changes.
                redoAction.run();
                undoService.setMostRecentAction(redoAction, undoAction);
            });
            Scene scene = new Scene(notePropertiesPane);
            themeManager.applyToScene(scene);
            propertiesWindow.setScene(scene);
            propertiesWindow.showAndWait();
        } catch (IOException e) {
            statusBar.setText("Error: Unable to open note properties editor.");
            errorLogger.logError(e);
        }
    }

    @FXML
    void rewindPlayback() {
        engine.stopPlayback();
        songEditor.stopPlayback();
        songEditor.selectRegion(RegionBounds.INVALID);
        songEditor.scrollToPosition(0); // Scroll to start of song.
        // TODO: Stop scrollbar's existing acceleration.
    }

    @FXML
    void playOrPause() {
        // Do nothing if play icon is currently disabled.
        if (playPauseIcon.isDisabled()) {
            return;
        }

        // Get current playback status to decide what to do.
        switch (engine.getStatus()) {
            case PLAYING:
                pausePlayback();
                break;
            case PAUSED:
                resumePlayback();
                break;
            case STOPPED:
                startPlayback();
                break;
        }
    }

    private void startPlayback() {
        // If there is no track selected, play the whole song instead.
        RegionBounds regionToPlay = songEditor.getPlayableTrack();

        Function<Duration, Void> startPlaybackFn = duration -> {
            songEditor.startPlayback(regionToPlay, duration);
            return null;
        };
        Runnable endPlaybackFn = () -> {
            iconManager.setPlayIcon(playPauseIcon);
        };

        // Disable the play button while rendering.
        playPauseIcon.setDisable(true);

        statusBar.setText("Rendering...");
        new Thread(() ->
        {
            if (engine.startPlayback(song.get(), regionToPlay, startPlaybackFn, endPlaybackFn)) {
                Platform.runLater(() -> {
                    iconManager.setPauseIcon(playPauseIcon);
                    statusBar.setText("Render complete.");
                });
            } else {
                Platform.runLater(() -> statusBar.setText("Render produced no output."));
            }
            playPauseIcon.setDisable(false);
        }).start();
    }

    private void pausePlayback() {
        engine.pausePlayback();
        songEditor.pausePlayback();
        iconManager.setPlayIcon(playPauseIcon);
    }

    private void resumePlayback() {
        engine.resumePlayback();
        songEditor.resumePlayback();
        iconManager.setPauseIcon(playPauseIcon);
    }

    @FXML
    void stopPlayback() {
        engine.stopPlayback();
        songEditor.stopPlayback();
    }

    @Override
    public void exportToWav() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select WAV File");
        fc.getExtensionFilters().addAll(new ExtensionFilter(".wav files", "*.wav"));
        File file = fc.showSaveDialog(null);
        if (file != null) {
            statusBar.setText("Exporting...");
            new Thread(() -> {
                if (engine.renderWav(song.get(), file)) {
                    Platform.runLater(
                            () -> statusBar.setText("Exported to file: " + file.getName()));
                } else {
                    Platform.runLater(() -> statusBar.setText("Export produced no output."));
                }
            }).start();
        }
    }

    @Override
    public void openProperties() {
        // Open song properties modal.
        Stage currentStage = (Stage) anchorCenter.getScene().getWindow();
        if (!currentStage.isFocused()) {
            return; // Only one modal at a time!
        }
        InputStream fxml = getClass().getResourceAsStream("/fxml/SongPropertiesScene.fxml");
        FXMLLoader loader = fxmlLoaderProvider.get();
        try {
            Stage propertiesWindow = new Stage();
            propertiesWindow.setTitle(localizer.getMessage("menu.project.properties"));
            propertiesWindow.initModality(Modality.APPLICATION_MODAL);
            propertiesWindow.initOwner(currentStage);
            BorderPane propertiesPane = loader.load(fxml);
            SongPropertiesController controller = loader.getController();
            controller.setData(song, engine, shouldClearCache -> {
                // Should only be called after song changes are applied.
                if (shouldClearCache) {
                    song.get().clearAllCacheValues();
                }
                Platform.runLater(() -> {
                    onSongChange();
                    refreshView();
                    statusBar.setText("Property changes applied.");
                });
                return null;
            });
            Scene scene = new Scene(propertiesPane);
            themeManager.applyToScene(scene);
            propertiesWindow.setScene(scene);
            propertiesWindow.showAndWait();
        } catch (IOException e) {
            statusBar.setText("Error: Unable to open note properties editor.");
            errorLogger.logError(e);
        }
    }

    @Override
    public void undo() {
        undoService.undo();
    }

    @Override
    public void redo() {
        undoService.redo();
    }

    @Override
    public void cut() {
        songEditor.copySelected();
        songEditor.deleteSelected();
    }

    @Override
    public void copy() {
        songEditor.copySelected();
    }

    @Override
    public void paste() {
        songEditor.pasteSelected();
    }

    @Override
    public void delete() {
        songEditor.deleteSelected();
    }

    @Override
    public void selectAll() {
        songEditor.selectAll();
    }

    @Override
    public void openNoteProperties() {
        if (!songEditor.getSelectedTrack().equals(RegionBounds.INVALID)) {
            openNotePropertiesEditor(songEditor.getSelectedTrack());
        }
    }

    @Override
    public void showLyricConfig(String trueLyric) {
        // Lyric configs can only be shown on voicebank editor.
    }

    @Override
    public void openBulkEditor(BulkEditorType editorType) {
        // Open bulk editor modal.
        Stage currentStage = (Stage) anchorCenter.getScene().getWindow();
        if (!currentStage.isFocused()) {
            return; // Only one modal at a time!
        }
        InputStream fxml = getClass().getResourceAsStream("/fxml/BulkEditorScene.fxml");
        FXMLLoader loader = fxmlLoaderProvider.get();
        try {
            Stage editorWindow = new Stage();
            editorWindow.setTitle(localizer.getMessage("menu.tools.bulkEditor"));
            editorWindow.initModality(Modality.APPLICATION_MODAL);
            editorWindow.initOwner(currentStage);
            BorderPane editorPane = loader.load(fxml);
            BulkEditorController controller = loader.getController();
            controller.openEditor(
                    editorType,
                    songEditor.getSelectedTrack(),
                    editorWindow,
                    new BulkEditorCallback() {
                        @Override
                        public void updatePortamento(
                                PitchbendData newPortamento,
                                RegionBounds regionToUpdate,
                                List<FilterType> filters) {
                            Function<NoteData, NoteData> transformNote = noteData -> {
                                if (noteData == null) {
                                    return noteData;
                                }
                                if (noteData.getPitchbend().isPresent()) {
                                    PitchbendData newPitchbend =
                                            newPortamento.withVibrato(Optional.of(
                                                    noteData.getPitchbend().get().getVibrato()));
                                    return noteData.withPitchbend(newPitchbend.deepcopy());
                                } else {
                                    return noteData;
                                }
                            };
                            modifyNotes(
                                    song.get().getNotes(regionToUpdate, filters), transformNote);
                        }

                        @Override
                        public void updateVibrato(
                                PitchbendData newVibrato,
                                RegionBounds regionToUpdate,
                                List<FilterType> filters) {
                            Function<NoteData, NoteData> transformNote = noteData -> {
                                if (noteData == null) {
                                    return noteData;
                                }
                                if (noteData.getPitchbend().isPresent()) {
                                    PitchbendData newPitchbend =
                                            noteData.getPitchbend().get().withVibrato(
                                                    Optional.of(newVibrato.getVibrato()));
                                    return noteData.withPitchbend(newPitchbend.deepcopy());
                                } else {
                                    return noteData;
                                }
                            };
                            modifyNotes(
                                    song.get().getNotes(regionToUpdate, filters), transformNote);
                        }

                        @Override
                        public void updateEnvelope(
                                EnvelopeData newEnvelope,
                                RegionBounds regionToUpdate,
                                List<FilterType> filters) {
                            Function<NoteData, NoteData> transformNote = noteData -> {
                                if (noteData == null) {
                                    return noteData;
                                }
                                return noteData.withEnvelope(newEnvelope.deepcopy());
                            };
                            modifyNotes(
                                    song.get().getNotes(regionToUpdate, filters), transformNote);
                        }
                    });
            Scene scene = new Scene(editorPane);
            themeManager.applyToScene(scene);
            editorWindow.setScene(scene);
            editorWindow.showAndWait();
        } catch (IOException e) {
            statusBar.setText("Error: Unable to open bulk editor.");
            errorLogger.logError(e);
        }
    }

    @Override
    public void openLyricEditor(LyricEditorType editorType) {
        // Open lyric editor modal.
        Stage currentStage = (Stage) anchorCenter.getScene().getWindow();
        if (!currentStage.isFocused()) {
            return; // Only one modal at a time!
        }
        InputStream fxml = getClass().getResourceAsStream("/fxml/LyricEditorScene.fxml");
        FXMLLoader loader = fxmlLoaderProvider.get();
        try {
            Stage editorWindow = new Stage();
            editorWindow.setTitle(localizer.getMessage("menu.tools.lyricEditor"));
            editorWindow.initModality(Modality.APPLICATION_MODAL);
            editorWindow.initOwner(currentStage);
            BorderPane editorPane = loader.load(fxml);
            LyricEditorController controller = loader.getController();
            controller.openEditor(
                    editorType,
                    songEditor.getSelectedTrack(),
                    editorWindow,
                    new LyricEditorCallback() {
                        @Override
                        public void insertLyrics(
                                List<String> newLyrics, RegionBounds regionToUpdate) {
                            // blah
                        }

                        @Override
                        public void addPrefix(String prefixToAdd, RegionBounds regionToUpdate) {
                            // blah
                        }

                        @Override
                        public void removePrefix(
                                String prefixToRemove, RegionBounds regionToUpdate) {
                            // blah
                        }

                        @Override
                        public void addSuffix(String suffixToAdd, RegionBounds regionToUpdate) {
                            // blah
                        }

                        @Override
                        public void removeSuffix(
                                String suffixToRemove, RegionBounds regionToUpdate) {
                            // blah
                        }
                    });
            Scene scene = new Scene(editorPane);
            themeManager.applyToScene(scene);
            editorWindow.setScene(scene);
            editorWindow.showAndWait();
        } catch (IOException e) {
            statusBar.setText("Error: Unable to open bulk editor.");
            errorLogger.logError(e);
        }
    }

    private void modifyNotes(List<NoteData> oldNotes, Function<NoteData, NoteData> transform) {
        List<NoteData> newNotes = oldNotes.stream().map(transform).collect(Collectors.toList());
        if (newNotes.isEmpty()) {
            return;
        }
        Runnable redoAction = () -> {
            int minMs = Integer.MAX_VALUE;
            int maxMs = 0;
            for (NoteData noteData : newNotes) {
                song.get().modifyNote(noteData);
                if (minMs > noteData.getPosition()) {
                    minMs = noteData.getPosition();
                }
                if (maxMs < noteData.getPosition() + noteData.getDuration()) {
                    maxMs = noteData.getPosition() + noteData.getDuration();
                }
            }
            onSongChange();
            songEditor.selectRegion(new RegionBounds(minMs, maxMs));
            songEditor.refreshSelected();
        };
        Runnable undoAction = () -> {
            int minMs = Integer.MAX_VALUE;
            int maxMs = 0;
            for (NoteData noteData : oldNotes) {
                song.get().modifyNote(noteData);
                if (minMs > noteData.getPosition()) {
                    minMs = noteData.getPosition();
                }
                if (maxMs < noteData.getPosition()) {
                    maxMs = noteData.getPosition() + noteData.getDuration();
                }
            }
            onSongChange();
            songEditor.selectRegion(new RegionBounds(minMs, maxMs));
            songEditor.refreshSelected();
        };
        // Apply changes and save redo/undo for these changes.
        redoAction.run();
        undoService.setMostRecentAction(redoAction, undoAction);
    }

    @Override
    public Optional<File> openPlugin() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select executable file");
        fc.getExtensionFilters().addAll(
                new ExtensionFilter("Executables", "*", "*.exe"), // TODO: Support .jar
                new ExtensionFilter("OSX Executables", "*.out", "*.app"),
                new ExtensionFilter("All Files", "*.*"));
        File file = fc.showOpenDialog(null);
        if (file != null) {
            invokePlugin(file);
            return Optional.of(file);
        }
        return Optional.empty();
    }

    @Override
    public void invokePlugin(File plugin) {
        if (plugin != null) {
            try {
                // Plugin input. Only give Shift-JIS UST 1.2 files to plugins for now.
                File pluginFile = File.createTempFile("plugin", ".ust");
                System.out.println("Plugin input: " + pluginFile.getAbsolutePath());
                pluginFile.deleteOnExit();
                PrintStream ps = new PrintStream(pluginFile, "SJIS");
                String[] headers =
                        ust12Writer.writeToPlugin(song.get(), songEditor.getSelectedTrack(), ps);
                ps.flush();
                ps.close();
                System.out.println(headers[0] + " " + headers[1]);

                // Write pre-plugin song to a string.
                ByteArrayOutputStream songBytes = new ByteArrayOutputStream();
                ps = new PrintStream(songBytes, true, "SJIS");
                ust12Writer.writeSong(song.get(), ps);
                ps.close();
                String songString = songBytes.toString("SJIS");

                // Attempt to run plugin.
                processRunner.runProcess(
                        new File(plugin.getAbsolutePath()).getParent(),
                        plugin.getAbsolutePath(),
                        pluginFile.getAbsolutePath());

                // Read song from plugin output.
                String output = FileUtils.readFileToString(pluginFile, "SJIS");
                song.setSong(ust12Reader.readFromPlugin(headers, songString, output));
                onSongChange();
                refreshView();

            } catch (IOException e) {
                // TODO: Handle this.
                errorLogger.logError(e);
            }
        }
    }
}
