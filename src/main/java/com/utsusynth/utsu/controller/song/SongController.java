package com.utsusynth.utsu.controller.song;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.utsusynth.utsu.common.dialog.ChooseTrackDialog;
import com.utsusynth.utsu.common.utils.RegionBounds;
import com.utsusynth.utsu.common.StatusBar;
import com.utsusynth.utsu.common.data.*;
import com.utsusynth.utsu.common.enums.FilterType;
import com.utsusynth.utsu.common.exception.ErrorLogger;
import com.utsusynth.utsu.common.exception.FileAlreadyOpenException;
import com.utsusynth.utsu.common.i18n.Localizable;
import com.utsusynth.utsu.common.i18n.Localizer;
import com.utsusynth.utsu.common.quantize.Quantizer;
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
import com.utsusynth.utsu.files.PreferencesManager;
import com.utsusynth.utsu.files.ThemeManager;
import com.utsusynth.utsu.files.song.*;
import com.utsusynth.utsu.files.voicebank.VoicebankReader;
import com.utsusynth.utsu.model.song.NoteIterator;
import com.utsusynth.utsu.model.song.SongContainer;
import com.utsusynth.utsu.model.song.converters.ReclistConverter;
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
import javafx.scene.input.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.shape.Rectangle;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.*;
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
    private final UndoService undoService;
    private final MenuItemManager menuItemManager;
    private final StatusBar statusBar;
    private final SongReaderManager songReaderManager;
    private final Ust12Writer ust12Writer;
    private final Ust20Writer ust20Writer;
    private final VoicebankReader voicebankReader;
    private final IconManager iconManager;
    private final ThemeManager themeManager;
    private final PreferencesManager preferencesManager;
    private final ExternalProcessRunner processRunner;
    private final Provider<ChooseTrackDialog> chooseTrackProvider;
    private final Provider<FXMLLoader> fxmlLoaderProvider;

    @FXML // fx:id="scrollPaneLeft"
    private ScrollPane scrollPaneLeft; // Value injected by FXMLLoader

    @FXML // fx:id="anchorLeft"
    private AnchorPane anchorLeft; // Value injected by FXMLLoader

    @FXML // fx:id="anchorCenter"
    private AnchorPane anchorCenter; // Value injected by FXMLLoader

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
            UndoService undoService,
            MenuItemManager menuItemManager,
            StatusBar statusBar,
            SongReaderManager songReaderManager,
            Ust12Writer ust12Writer,
            Ust20Writer ust20Writer,
            VoicebankReader voicebankReader,
            IconManager iconManager,
            ThemeManager themeManager,
            PreferencesManager preferencesManager,
            ExternalProcessRunner processRunner,
            Provider<ChooseTrackDialog> chooseTrackProvider,
            Provider<FXMLLoader> fxmlLoaders) {
        this.song = songContainer;
        this.engine = engine;
        this.songEditor = songEditor;
        this.piano = piano;
        this.localizer = localizer;
        this.quantizer = quantizer;
        this.undoService = undoService;
        this.menuItemManager = menuItemManager;
        this.statusBar = statusBar;
        this.songReaderManager = songReaderManager;
        this.ust12Writer = ust12Writer;
        this.ust20Writer = ust20Writer;
        this.voicebankReader = voicebankReader;
        this.iconManager = iconManager;
        this.themeManager = themeManager;
        this.preferencesManager = preferencesManager;
        this.processRunner = processRunner;
        this.chooseTrackProvider = chooseTrackProvider;
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

            @Override
            public AnchorPane getLyricPane() {
                return anchorCenter;
            }

            @Override
            public List<String> getVoicebankPrefixes() {
                return song.get().getVoicebank().getPrefixes();
            }

            @Override
            public List<String> getVoicebankSuffixes() {
                return song.get().getVoicebank().getSuffixes();
            }
        });
        scrollPaneLeft.setVvalue(0.5);

        // Context menu for voicebank icon.
        ContextMenu iconContextMenu = new ContextMenu();
        MenuItem openVoicebankItem = new MenuItem("Open Voicebank");
        openVoicebankItem.setOnAction(event -> callback.openVoicebank(song.get().getVoiceDir()));
        MenuItem changeVoicebankItem = new MenuItem("Change...");
        changeVoicebankItem.setOnAction(event -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Select voicebank");
            File file = dc.showDialog(null);
            if (file != null
                    && !file.getAbsolutePath().equals(song.get().getVoiceDir().getAbsolutePath())) {
                new Thread(() -> {
                    song.setSong(song.get().toBuilder().setVoiceDirectory(file).build());
                    String newName = song.get().getVoicebank().getName(); // Loads voicebank.
                    song.get().clearAllCacheValues();
                    Platform.runLater(() -> {
                        onSongChange();
                        refreshView();
                        statusBar.setText("Changed voicebank to " + newName + ".");
                    });
                }).start();
            }
        });
        iconContextMenu.getItems().addAll(openVoicebankItem, changeVoicebankItem);
        iconContextMenu.setOnShowing(event -> {
            openVoicebankItem.setText(localizer.getMessage("song.openCurrentVoicebank"));
            changeVoicebankItem.setText(localizer.getMessage("properties.change"));
        });
        voicebankImage.setOnMouseClicked(event -> {
            iconContextMenu.hide();
            iconContextMenu.show(voicebankImage, event.getScreenX(), event.getScreenY());
        });

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
        // Load voicebank images.
        ImageView voicebankPortrait = new ImageView();
        try {
            // Voicebank image on the upper left.
            Image image = new Image("file:" + song.get().getVoicebank().getImagePath());
            voicebankImage.setImage(image);
            voicebankImage.visibleProperty().bind(preferencesManager.getShowVoicebankFace());

            // Full-size character portrait.
            Image portrait = new Image("file:" + song.get().getVoicebank().getPortraitPath());
            voicebankPortrait.setImage(portrait);
            voicebankPortrait.setOpacity(song.get().getVoicebank().getPortraitOpacity());
            voicebankPortrait.setPreserveRatio(true);
            voicebankPortrait.setFitHeight(800);
            voicebankPortrait.setMouseTransparent(true);
            voicebankPortrait.setSmooth(true);
            Rectangle clip = new Rectangle();
            clip.widthProperty().bind(
                    anchorCenter.widthProperty().subtract(Quantizer.SCROLL_BAR_WIDTH));
            clip.heightProperty().bind(
                    anchorCenter.heightProperty().subtract(Quantizer.SCROLL_BAR_WIDTH));
            voicebankPortrait.setClip(clip);
            voicebankPortrait.visibleProperty().bind(preferencesManager.getShowVoicebankBody());
        } catch (Exception e) {
            System.out.println("Exception while loading voicebank images.");
            errorLogger.logWarning(e);
        }

        anchorLeft.getChildren().clear();
        anchorLeft.getChildren().add(piano.initPiano());

        // Cross-editor bindings.
        ListView<TrackItemSet> noteTrack = songEditor.createNewTrack(song.get().getNotes());
        noteTrack.prefWidthProperty().bind(anchorCenter.widthProperty());
        noteTrack.prefHeightProperty().bind(anchorCenter.heightProperty());
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
        anchorCenter.getChildren().addAll(noteTrack, voicebankPortrait);
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
    public void onThemeChanged() {
        // No manual theme handling needed for songs right now.
    }

    @Override
    public Optional<String> open(String... fileType) throws FileAlreadyOpenException {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select File");
        StringBuilder allFileTypes = new StringBuilder();
        for (int i = 0; i < fileType.length; i++) {
            allFileTypes.append(fileType[i]);
            if (i < fileType.length - 1) {
                allFileTypes.append(", ");
            }
        }
        allFileTypes.append(" files");
        fc.getExtensionFilters().addAll(
                new ExtensionFilter(allFileTypes.toString(), fileType),
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
                SongReader songReader = songReaderManager.getSongReader(file);
                String saveFormat = songReader.getSaveFormat(file);
                // Read song now if it's only one track.
                int numTracks = songReader.getNumTracks(file);
                if (numTracks <= 1) {
                    song.setSong(songReader.loadSong(file, 1));
                }
                // Determine if there's more than one track.
                Platform.runLater(() -> {
                    // Get num tracks.
                    if (numTracks > 1) {
                        Stage parent = (Stage) anchorCenter.getScene().getWindow();
                        ImmutableList<Integer> trackNums =
                                chooseTrackProvider.get().popup(parent, numTracks);
                        if (trackNums.isEmpty()) {
                            return; // Cancel process if no tracks are selected.
                        }
                        song.setSong(songReader.loadSong(file, trackNums.get(0)));
                        for (int i = 1; i < trackNums.size(); i++) {
                            callback.openSongTrack(file, songReader, trackNums.get(i));
                        }
                    }
                    undoService.clearActions();
                    // If no save format, mark as unsaved file with no permanent location.
                    if (saveFormat.isEmpty()) {
                        song.reset();
                        callback.markChanged(true);
                    } else {
                        song.setSaveFormat(songReader.getSaveFormat(file));
                        callback.markChanged(false);
                    }
                    // Update view.
                    refreshView();
                    menuItemManager.disableSave();
                    statusBar.setText("Opened " + file.getName());
                    // Do scrolling after a short pause for viewport to establish itself.
                    PauseTransition briefPause = new PauseTransition(Duration.millis(10));
                    briefPause.setOnFinished(event -> songEditor.scrollToPosition(0));
                    briefPause.play();
                });
            } catch (Exception e) {
                statusBar.setTextAsync("Error: Unable to open " + file.getName());
                errorLogger.logError(e);
            }
        }).start();
    }

    /** Opens a file with some values pre-calculated. Opens silently without changing status bar. */
    public void openSubTrack(
            File file, SongReader songReader, int trackNum) throws FileAlreadyOpenException {
        new Thread(() -> {
            try {
                song.setSong(songReader.loadSong(file, trackNum));
                String saveFormat = songReader.getSaveFormat(file);
                undoService.clearActions();
                if (!saveFormat.isEmpty()) {
                    song.setSaveFormat(saveFormat);
                }
                Platform.runLater(() -> {
                    // Update view.
                    refreshView();
                    callback.markChanged(true);
                    menuItemManager.disableSave();
                    // Do scrolling after a short pause for viewport to establish itself.
                    PauseTransition briefPause = new PauseTransition(Duration.millis(10));
                    briefPause.setOnFinished(event -> songEditor.scrollToPosition(0));
                    briefPause.play();
                });
            } catch (Exception e) {
                statusBar.setTextAsync("Error: Unable to open " + file.getName());
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
                   statusBar.setTextAsync("Error: Unable to save " + saveLocation.getName());
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
                    statusBar.setTextAsync("Error: Unable to save as " + file.getName());
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
                statusBar.setTextAsync("Render produced no output.");
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
                    statusBar.setTextAsync("Exported to file: " + file.getName());
                } else {
                    statusBar.setTextAsync("Export produced no output.");
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

    /**
     * Can modify a set of notes, but cannot modify lyric, position, or duration. For those fields
     * use updateNotes instead.
     */
    private void modifyNotes(List<NoteData> oldNotes, Function<NoteData, NoteData> transform) {
        List<NoteData> newNotes = oldNotes.stream().map(transform).collect(Collectors.toList());
        if (newNotes.isEmpty()) {
            return;
        }
        Runnable redoAction = () -> {
            for (NoteData noteData : newNotes) {
                song.get().modifyNote(noteData);
            }
            onSongChange();
            songEditor.selectRegion(getRegionBounds(newNotes));
            songEditor.refreshSelected();
        };
        Runnable undoAction = () -> {
            for (NoteData noteData : oldNotes) {
                song.get().modifyNote(noteData);
            }
            onSongChange();
            songEditor.selectRegion(getRegionBounds(oldNotes));
            songEditor.refreshSelected();
        };
        // Apply changes and save redo/undo for these changes.
        redoAction.run();
        undoService.setMostRecentAction(redoAction, undoAction);
    }

    /** Utility method to get the region surrounding a series of notes. */
    private static RegionBounds getRegionBounds(List<NoteData> notes) {
        int minMs = Integer.MAX_VALUE;
        int maxMs = 0;
        for (NoteData noteData : notes) {
            if (minMs > noteData.getPosition()) {
                minMs = noteData.getPosition();
            }
            if (maxMs < noteData.getPosition()) {
                maxMs = noteData.getPosition() + noteData.getDuration();
            }
        }
        return new RegionBounds(minMs, maxMs);
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
                                String[] newLyrics, RegionBounds regionToUpdate) {
                            List<NoteData> notesToChange = new ArrayList<>();
                            List<NoteData> newNotes = new ArrayList<>();
                            int lyricIndex = 0;
                            for (NoteData noteData : song.get().getNotes(regionToUpdate)) {
                                if (lyricIndex >= newLyrics.length) {
                                    break;
                                }
                                notesToChange.add(noteData);
                                newNotes.add(noteData.withNewLyric(newLyrics[lyricIndex]));
                                lyricIndex++;
                            }
                            updateNotes(notesToChange, newNotes);
                        }

                        @Override
                        public void transformLyric(
                                Function<NoteData, NoteData> transform,
                                RegionBounds regionToUpdate) {
                            List<NoteData> oldNotes = song.get().getNotes(regionToUpdate);
                            List<NoteData> newNotes = oldNotes.stream()
                                    .map(transform)
                                    .collect(Collectors.toList());
                            updateNotes(oldNotes, newNotes);
                        }

                        @Override
                        public void convertReclist(
                                List<ReclistConverter> path,
                                boolean usePresampConfig,
                                RegionBounds regionToUpdate) {
                            // Override with default presamp config if needed.
                            VoicebankData voicebankData = usePresampConfig
                                    ? song.get().getVoicebank().getReadonlyData()
                                    : song.get().getVoicebank().getReadonlyData().withPresampConfig(
                                            voicebankReader.getDefaultPresampConfig().getReader());
                            statusBar.setText("Converting...");
                            StringBuilder result = new StringBuilder("Converted: ");
                            new Thread(() -> {
                                for (ReclistConverter converter : path) {
                                    List<NoteContextData> oldNotes =
                                            song.get().getNotesInContext(regionToUpdate);
                                    List<NoteData> newNotes = converter.apply(oldNotes, voicebankData);
                                    updateNotes(song.get().getNotes(regionToUpdate), newNotes);
                                    result
                                            .append(converter.getFrom())
                                            .append("->")
                                            .append(converter.getTo())
                                            .append(",");
                                }
                                if (!path.isEmpty()) {
                                    result.deleteCharAt(result.length() - 1); // Delete last comma.
                                    Platform.runLater(() -> {
                                        onSongChange();
                                        refreshView();
                                        statusBar.setText(result.toString());
                                    });
                                }
                            }).start();
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

    /**
     * Modify anything about a set of notes, including lyric, positions, and quantity of notes.
     * @param oldNotes: List of old notes.
     * @param newNotes: List of new notes.
     */
    private void updateNotes(List<NoteData> oldNotes, List<NoteData> newNotes) {
        if (newNotes.isEmpty()) {
            return;
        }
        // To avoid errors, sort both old and new notes.
        Comparator<NoteData> noteComparator = Comparator.comparingInt(NoteData::getPosition);
        List<NoteData> sortedOldNotes =
                oldNotes.stream().sorted(noteComparator).collect(Collectors.toList());
        List<NoteData> sortedNewNotes =
                newNotes.stream().sorted(noteComparator).collect(Collectors.toList());

        Runnable redoAction = () -> {
            MutateResponse removeResponse = song.get().removeNotes(
                    sortedOldNotes.stream().map(NoteData::getPosition).collect(Collectors.toSet()));
            song.get().addNotes(sortedNewNotes);

            // Standardize all impacted notes.
            int newMinPos = sortedNewNotes.get(0).getPosition();
            int minPos = removeResponse.getPrev()
                    .map(prev -> Math.min(prev.getPosition(), newMinPos)).orElse(newMinPos);
            int newMaxPos = sortedNewNotes.get(sortedNewNotes.size() - 1).getPosition();
            int maxPos = removeResponse.getNext()
                    .map(next -> Math.max(next.getPosition(), newMaxPos)).orElse(newMaxPos);
            song.get().standardizeNotes(minPos, maxPos);

            // If run from a separate thread, leave refreshing the view for later.
            if (Platform.isFxApplicationThread()) {
                onSongChange();
                refreshView();
            }
        };
        Runnable undoAction = () -> {
            MutateResponse removeResponse = song.get().removeNotes(
                    sortedNewNotes.stream().map(NoteData::getPosition).collect(Collectors.toSet()));
            song.get().addNotes(sortedOldNotes);

            // Standardize all impacted notes.
            int newMinPos = sortedOldNotes.get(0).getPosition();
            int minPos = removeResponse.getPrev()
                    .map(prev -> Math.min(prev.getPosition(), newMinPos)).orElse(newMinPos);
            int newMaxPos = sortedOldNotes.get(sortedOldNotes.size() - 1).getPosition();
            int maxPos = removeResponse.getNext()
                    .map(next -> Math.max(next.getPosition(), newMaxPos)).orElse(newMaxPos);
            song.get().standardizeNotes(minPos, maxPos);

            onSongChange();
            if (Platform.isFxApplicationThread()) {
                onSongChange();
                refreshView();
            }
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
                // String output = FileUtils.readFileToString(pluginFile, "SJIS");
                // song.setSong(ust12Reader.readFromPlugin(headers, songString, output));
                onSongChange();
                refreshView();

            } catch (IOException e) {
                // TODO: Handle this.
                errorLogger.logError(e);
            }
        }
    }

    public void toggleMetronome(MouseEvent mouseEvent) {

    }
}
