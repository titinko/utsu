package com.utsusynth.utsu.controller;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.MalformedInputException;
import java.nio.charset.UnmappableCharacterException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.utsusynth.utsu.common.RegionBounds;
import com.utsusynth.utsu.common.StatusBar;
import com.utsusynth.utsu.common.UndoService;
import com.utsusynth.utsu.common.data.MutateResponse;
import com.utsusynth.utsu.common.data.NoteData;
import com.utsusynth.utsu.common.data.NoteUpdateData;
import com.utsusynth.utsu.common.exception.ErrorLogger;
import com.utsusynth.utsu.common.exception.FileAlreadyOpenException;
import com.utsusynth.utsu.common.i18n.Localizable;
import com.utsusynth.utsu.common.i18n.Localizer;
import com.utsusynth.utsu.common.i18n.NativeLocale;
import com.utsusynth.utsu.common.quantize.Quantizer;
import com.utsusynth.utsu.common.quantize.Scaler;
import com.utsusynth.utsu.controller.IconManager.IconType;
import com.utsusynth.utsu.engine.Engine;
import com.utsusynth.utsu.engine.Engine.PlaybackStatus;
import com.utsusynth.utsu.engine.ExternalProcessRunner;
import com.utsusynth.utsu.files.Ust12Reader;
import com.utsusynth.utsu.files.Ust12Writer;
import com.utsusynth.utsu.files.Ust20Reader;
import com.utsusynth.utsu.files.Ust20Writer;
import com.utsusynth.utsu.model.song.SongContainer;
import com.utsusynth.utsu.view.song.Piano;
import com.utsusynth.utsu.view.song.SongCallback;
import com.utsusynth.utsu.view.song.SongEditor;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

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
    private final StatusBar statusBar;
    private final Ust12Reader ust12Reader;
    private final Ust20Reader ust20Reader;
    private final Ust12Writer ust12Writer;
    private final Ust20Writer ust20Writer;
    private final IconManager iconManager;
    private final ExternalProcessRunner processRunner;
    private final Provider<FXMLLoader> fxmlLoaderProvider;

    @FXML // fx:id="scrollPaneLeft"
    private ScrollPane scrollPaneLeft; // Value injected by FXMLLoader

    @FXML // fx:id="anchorLeft"
    private AnchorPane anchorLeft; // Value injected by FXMLLoader

    @FXML // fx:id="anchorCenter"
    private AnchorPane anchorCenter; // Value injected by FXMLLoader

    @FXML // fx:id="scrollPaneRight"
    private ScrollPane scrollPaneCenter; // Value injected by FXMLLoader

    @FXML // fx:id="anchorRight"
    private AnchorPane anchorRight; // Value injected by FXMLLoader

    @FXML // fx:id="scrollPaneBottom"
    private ScrollPane scrollPaneBottom; // Value injected by FXMLLoader

    @FXML // fx:id="anchorBottom"
    private AnchorPane anchorBottom; // Value injected by FXMLLoader

    @FXML // fx:id="voicebankImage"
    private ImageView voicebankImage; // Value injected by FXMLLoader

    @FXML // fx:id="rewindIcon"
    private ImageView rewindIcon; // Value injected by FXMLLoader

    @FXML // fx:id="playPauseIcon"
    private ImageView playPauseIcon; // Value injected by FXMLLoader

    @FXML // fx:id="stopIcon"
    private ImageView stopIcon; // Value injected by FXMLLoader

    @FXML // fx:id="quantizeChoiceBox"
    private ChoiceBox<String> quantizeChoiceBox; // Value injected by FXMLLoader

    @FXML // fx:id="languageChoiceBox"
    private ChoiceBox<NativeLocale> languageChoiceBox; // Value injected by FXMLLoader

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
            StatusBar statusBar,
            Ust12Reader ust12Reader,
            Ust20Reader ust20Reader,
            Ust12Writer ust12Writer,
            Ust20Writer ust20Writer,
            IconManager iconManager,
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
        this.statusBar = statusBar;
        this.ust12Reader = ust12Reader;
        this.ust20Reader = ust20Reader;
        this.ust12Writer = ust12Writer;
        this.ust20Writer = ust20Writer;
        this.iconManager = iconManager;
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
            public void openNoteProperties(RegionBounds regionBounds) {
                openNotePropertiesEditor(regionBounds);
            }
        });
        anchorCenter.widthProperty().addListener((obs, oldWidthNum, newWidthNum) -> {
            // Scrollbar should still be at its old location.
            double oldWidth = oldWidthNum.doubleValue() - scrollPaneCenter.getWidth();
            double newWidth = newWidthNum.doubleValue() - scrollPaneCenter.getWidth();
            if (oldWidth > 0 && newWidth > 0) {
                scrollPaneCenter.setHvalue(scrollPaneCenter.getHvalue() * newWidth / oldWidth);
            }
        });
        scrollPaneLeft.vvalueProperty().bindBidirectional(scrollPaneCenter.vvalueProperty());
        scrollPaneCenter.hvalueProperty().bindBidirectional(scrollPaneBottom.hvalueProperty());
        scrollPaneCenter.hvalueProperty().addListener(event -> {
            double hvalue = scrollPaneCenter.getHvalue();
            double margin = scrollPaneCenter.getViewportBounds().getWidth();
            songEditor.selectivelyShowRegion(hvalue, margin);
        });
        scrollPaneCenter.viewportBoundsProperty().addListener((event, oldValue, newValue) -> {
            if (oldValue.getWidth() != newValue.getWidth()) {
                double hvalue = scrollPaneCenter.getHvalue();
                double margin = scrollPaneCenter.getViewportBounds().getWidth();
                songEditor.selectivelyShowRegion(hvalue, margin);
            }
        });

        quantizeChoiceBox
                .setItems(FXCollections.observableArrayList("1/4", "1/8", "1/16", "1/32", "1/64"));
        quantizeChoiceBox.setOnAction((action) -> {
            String quantization = quantizeChoiceBox.getValue();
            if (quantization.equals("1/4")) {
                quantizer.changeQuant(quantizer.getQuant(), 1);
            } else if (quantization.equals("1/8")) {
                quantizer.changeQuant(quantizer.getQuant(), 2);
            } else if (quantization.equals("1/16")) {
                quantizer.changeQuant(quantizer.getQuant(), 4);
            } else if (quantization.equals("1/32")) {
                quantizer.changeQuant(quantizer.getQuant(), 8);
            } else if (quantization.equals("1/64")) {
                quantizer.changeQuant(quantizer.getQuant(), 16);
            }
        });
        quantizeChoiceBox.setValue("1/16");

        rewindIcon.setImage(iconManager.getImage(IconType.REWIND_NORMAL));
        rewindIcon.setOnMousePressed(
                event -> rewindIcon.setImage(iconManager.getImage(IconType.REWIND_PRESSED)));
        rewindIcon.setOnMouseReleased(
                event -> rewindIcon.setImage(iconManager.getImage(IconType.REWIND_NORMAL)));
        playPauseIcon.setImage(iconManager.getImage(IconType.PLAY_NORMAL));
        playPauseIcon.setOnMousePressed(event -> {
            if (engine.getStatus() == PlaybackStatus.PLAYING) {
                playPauseIcon.setImage(iconManager.getImage(IconType.PAUSE_PRESSED));
            } else {
                playPauseIcon.setImage(iconManager.getImage(IconType.PLAY_PRESSED));
            }
        });
        playPauseIcon.setOnMouseReleased(event -> {
            if (engine.getStatus() == PlaybackStatus.PLAYING) {
                playPauseIcon.setImage(iconManager.getImage(IconType.PAUSE_NORMAL));
            } else {
                playPauseIcon.setImage(iconManager.getImage(IconType.PLAY_NORMAL));
            }
        });
        stopIcon.setImage(iconManager.getImage(IconType.STOP_NORMAL));
        stopIcon.setOnMousePressed(
                event -> stopIcon.setImage(iconManager.getImage(IconType.STOP_PRESSED)));
        stopIcon.setOnMouseReleased(
                event -> stopIcon.setImage(iconManager.getImage(IconType.STOP_NORMAL)));

        languageChoiceBox.setItems(FXCollections.observableArrayList(localizer.getAllLocales()));
        languageChoiceBox
                .setOnAction((action) -> localizer.setLocale(languageChoiceBox.getValue()));
        languageChoiceBox.setValue(localizer.getCurrentLocale());

        refreshView();
        scrollToPosition(0);

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

        anchorLeft.getChildren().add(piano.initPiano());

        // Reloads current song.
        anchorCenter.getChildren().clear();
        anchorCenter.getChildren().add(songEditor.createNewTrack(song.get().getNotes()));
        anchorCenter.getChildren().add(songEditor.getNotesElement());
        anchorCenter.getChildren().add(songEditor.getPitchbendsElement());
        anchorCenter.getChildren().add(songEditor.getPlaybackElement());
        anchorCenter.getChildren().add(songEditor.getSelectionElement());
        anchorBottom.getChildren().clear();
        anchorBottom.getChildren().add(songEditor.getDynamicsElement());
        anchorBottom.getChildren().add(songEditor.getEnvelopesElement());
    }

    @Override
    public void openEditor(EditorCallback callback) {
        this.callback = callback;
    }

    @Override
    public void closeEditor() {
        // Remove this song from local memory.
        song.removeSong();
    }

    @Override
    public String getFileName() {
        return song.getLocation().getName();
    }

    @Override
    public boolean hasPermanentLocation() {
        return song.hasPermanentLocation();
    }

    @Override
    public boolean onKeyPressed(KeyEvent keyEvent) {
        if (new KeyCodeCombination(KeyCode.SPACE).match(keyEvent)) {
            // In the pause case, flicker will be overridden before it can happen.
            flickerImage(playPauseIcon, iconManager.getImage(IconType.PLAY_PRESSED));
            playOrPause();
            return true;
        } else if (new KeyCodeCombination(KeyCode.V).match(keyEvent)) {
            flickerImage(rewindIcon, iconManager.getImage(IconType.REWIND_PRESSED));
            rewindPlayback(); // Rewind button's event handler.
            return true;
        } else if (new KeyCodeCombination(KeyCode.B).match(keyEvent)) {
            flickerImage(stopIcon, iconManager.getImage(IconType.STOP_PRESSED));
            stopPlayback(); // Stop button's event handler.
            return true;
        } else if (new KeyCodeCombination(KeyCode.BACK_SPACE).match(keyEvent)) {
            songEditor.deleteSelected();
            return true;
        } else if (new KeyCodeCombination(KeyCode.ENTER).match(keyEvent)) {
            Optional<Integer> focusNote = songEditor.getFocusNote();
            if (focusNote.isPresent()) {
                scrollToPosition(focusNote.get());
                songEditor.openLyricInput(focusNote.get());
            }
            return true;
        } else if (new KeyCodeCombination(KeyCode.TAB).match(keyEvent)) {
            Optional<Integer> focusNote = songEditor.getFocusNote();
            if (focusNote.isPresent()) {
                Optional<Integer> newFocus = song.get().getNextNote(focusNote.get());
                if (newFocus.isPresent()) {
                    scrollToPosition(newFocus.get());
                    songEditor.focusOnNote(newFocus.get());
                }
            } else if (song.get().getNoteIterator().hasNext()) {
                songEditor.focusOnNote(song.get().getNoteIterator().next().getDelta());
            }
            return true;
        } else {
            // No need to override default key behavior.
            return false;
        }
    }

    /** Quickly sets an icon to a different image, then changes it back. */
    private void flickerImage(ImageView icon, Image flickerImage) {
        Image defaultImage = icon.getImage();
        icon.setImage(flickerImage);
        PauseTransition briefPause = new PauseTransition(Duration.millis(120));
        briefPause.setOnFinished(event -> {
            // Do nothing if an external source has already changed the image.
            if (icon.getImage().equals(flickerImage)) {
                icon.setImage(defaultImage);
            }
        });
        briefPause.play();
    }

    private void scrollToPosition(int positionMs) {
        // This is only roughly accurate.
        scrollPaneCenter.setHvalue(scaler.scalePos(positionMs) / songEditor.getWidthX());
    }

    @Override
    public Optional<String> open() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select UST File");
        fc.getExtensionFilters().addAll(
                new ExtensionFilter("UST files", "*.ust"),
                new ExtensionFilter("All files", "*.*"));
        File file = fc.showOpenDialog(null);
        if (file != null) {
            statusBar.setStatus("Opening " + file.getName() + "...");
            try {
                song.setLocation(file);
            } catch (FileAlreadyOpenException e) {
                statusBar.setStatus("Error: Cannot have the same file open in two tabs.");
                return Optional.absent();
            }
            new Thread(() -> {
                try {
                    String saveFormat; // Format to save this song in the future.
                    String charset = "UTF-8";
                    CharsetDecoder utf8Decoder = Charset.forName("UTF-8").newDecoder()
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
                        callback.enableSave(false);
                        statusBar.setStatus("Opened " + file.getName());
                    });
                } catch (Exception e) {
                    Platform.runLater(
                            () -> statusBar.setStatus("Error: Unable to open " + file.getName()));
                    errorLogger.logError(e);
                }
            }).start();
            return Optional.of(file.getName());
        }
        return Optional.absent();
    }

    @Override
    public Optional<String> save() {
        if (song.hasPermanentLocation()) {
            String saveFormat = song.getSaveFormat();
            String charset = saveFormat.contains("Shift JIS") ? "SJIS" : "UTF-8";
            File saveLocation = song.getLocation();
            statusBar.setStatus("Saving...");
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
                        callback.enableSave(false);
                        statusBar.setStatus("Saved changes to " + saveLocation.getName());
                    });
                } catch (Exception e) {
                    Platform.runLater(
                            () -> statusBar
                                    .setStatus("Error: Unable to save " + saveLocation.getName()));
                    errorLogger.logError(e);
                }
            }).start();
            return Optional.absent();
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
            statusBar.setStatus("Saving...");
            try {
                song.setLocation(file);
            } catch (FileAlreadyOpenException e) {
                statusBar.setStatus("Error: Cannot have the same file open in two tabs.");
                return Optional.absent();
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
                        callback.enableSave(false);
                        statusBar.setStatus("Saved as " + file.getName());
                    });
                } catch (Exception e) {
                    Platform.runLater(
                            () -> statusBar
                                    .setStatus("Error: Unable to save as " + file.getName()));
                    errorLogger.logError(e);
                }
            }).start();
            // File name may have changed, so just return new file name.
            return Optional.of(file.getName());
        }
        return Optional.absent();
    }

    /** Called whenever a Song is changed. */
    private void onSongChange() {
        if (callback == null) {
            return;
        }
        if (song.hasPermanentLocation()) {
            callback.enableSave(true);
        } else {
            callback.enableSave(false);
            callback.markChanged(); // Don't enable save, but enable asterisk.
        }
    }

    /** Allows users to alter properties of individual notes and note regions. */
    private void openNotePropertiesEditor(RegionBounds regionBounds) {
        // Open note properties modal.
        InputStream fxml = getClass().getResourceAsStream("/fxml/NotePropertiesScene.fxml");
        FXMLLoader loader = fxmlLoaderProvider.get();
        try {
            Stage currentStage = (Stage) anchorCenter.getScene().getWindow();
            Stage propertiesWindow = new Stage();
            propertiesWindow.setTitle("Note Properties");
            propertiesWindow.initModality(Modality.APPLICATION_MODAL);
            propertiesWindow.initOwner(currentStage);
            BorderPane notePropertiesPane = loader.load(fxml);
            NotePropertiesController controller = (NotePropertiesController) loader.getController();
            controller.setData(song, regionBounds, () -> {
                onSongChange();
                songEditor.selectRegion(regionBounds);
                songEditor.refreshSelected();
            });
            propertiesWindow.setScene(new Scene(notePropertiesPane));
            propertiesWindow.showAndWait();
        } catch (IOException e) {
            statusBar.setStatus("Error: Unable to open note properties editor.");
            errorLogger.logError(e);
        }
    }

    @FXML
    void rewindPlayback() {
        engine.stopPlayback();
        songEditor.stopPlayback();
        songEditor.selectRegion(RegionBounds.INVALID);
        scrollToPosition(0); // Scroll to start of song.
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

        Function<Duration, Void> startPlaybackFn =
                duration -> songEditor.startPlayback(regionToPlay, duration);
        Runnable endPlaybackFn = () -> {
            playPauseIcon.setImage(iconManager.getImage(IconType.PLAY_NORMAL));
        };

        // Disable the play button while rendering.
        playPauseIcon.setDisable(true);

        statusBar.setStatus("Rendering...");
        new Thread(() -> {
            if (engine.startPlayback(song.get(), regionToPlay, startPlaybackFn, endPlaybackFn)) {
                playPauseIcon.setImage(iconManager.getImage(IconType.PAUSE_NORMAL));
                Platform.runLater(() -> statusBar.setStatus("Render complete."));
            } else {
                Platform.runLater(() -> statusBar.setStatus("Render produced no output."));
            }
            playPauseIcon.setDisable(false);
        }).start();
    }

    private void pausePlayback() {
        engine.pausePlayback();
        songEditor.pausePlayback();
        playPauseIcon.setImage(iconManager.getImage(IconType.PLAY_NORMAL));
    }

    private void resumePlayback() {
        engine.resumePlayback();
        songEditor.resumePlayback();
        playPauseIcon.setImage(iconManager.getImage(IconType.PAUSE_NORMAL));
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
            statusBar.setStatus("Exporting...");
            new Thread(() -> {
                if (engine.renderWav(song.get(), file)) {
                    Platform.runLater(
                            () -> statusBar.setStatus("Exported to file: " + file.getName()));
                } else {
                    Platform.runLater(() -> statusBar.setStatus("Export produced no output."));
                }
            }).start();
        }
    }

    @Override
    public void openProperties() {
        // Open song properties modal.
        InputStream fxml = getClass().getResourceAsStream("/fxml/SongPropertiesScene.fxml");
        FXMLLoader loader = fxmlLoaderProvider.get();
        try {
            Stage currentStage = (Stage) anchorCenter.getScene().getWindow();
            Stage propertiesWindow = new Stage();
            propertiesWindow.setTitle("Song Properties");
            propertiesWindow.initModality(Modality.APPLICATION_MODAL);
            propertiesWindow.initOwner(currentStage);
            BorderPane propertiesPane = loader.load(fxml);
            SongPropertiesController controller = (SongPropertiesController) loader.getController();
            controller.setData(song, engine, () -> {
                onSongChange();
                refreshView();
            });
            propertiesWindow.setScene(new Scene(propertiesPane));
            propertiesWindow.showAndWait();
        } catch (IOException e) {
            statusBar.setStatus("Error: Unable to open note properties editor.");
            errorLogger.logError(e);
        }
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
        return Optional.absent();
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
