package com.utsusynth.utsu.controller;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.MalformedInputException;
import java.nio.charset.UnmappableCharacterException;
import java.util.ResourceBundle;
import org.apache.commons.io.FileUtils;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.utsusynth.utsu.common.RegionBounds;
import com.utsusynth.utsu.common.UndoService;
import com.utsusynth.utsu.common.data.AddResponse;
import com.utsusynth.utsu.common.data.NoteData;
import com.utsusynth.utsu.common.data.RemoveResponse;
import com.utsusynth.utsu.common.exception.ErrorLogger;
import com.utsusynth.utsu.common.exception.FileAlreadyOpenException;
import com.utsusynth.utsu.common.exception.NoteAlreadyExistsException;
import com.utsusynth.utsu.common.i18n.Localizable;
import com.utsusynth.utsu.common.i18n.Localizer;
import com.utsusynth.utsu.common.i18n.NativeLocale;
import com.utsusynth.utsu.common.quantize.Quantizer;
import com.utsusynth.utsu.controller.IconManager.IconState;
import com.utsusynth.utsu.engine.Engine;
import com.utsusynth.utsu.engine.ExternalProcessRunner;
import com.utsusynth.utsu.files.Ust12Reader;
import com.utsusynth.utsu.files.Ust12Writer;
import com.utsusynth.utsu.files.Ust20Reader;
import com.utsusynth.utsu.files.Ust20Writer;
import com.utsusynth.utsu.model.song.SongContainer;
import com.utsusynth.utsu.view.song.Piano;
import com.utsusynth.utsu.view.song.SongCallback;
import com.utsusynth.utsu.view.song.SongEditor;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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

    public enum Mode {
        ADD, EDIT, DELETE,
    }

    // User session data goes here.
    private Mode currentMode;
    private EditorCallback callback;

    // Helper classes go here.
    private final SongContainer song;
    private final Engine engine;
    private final SongEditor songEditor;
    private final Piano piano;
    private final Localizer localizer;
    private final Quantizer quantizer;
    private final UndoService undoService;
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

    @FXML // fx:id="playIcon"
    private ImageView playIcon; // Value injected by FXMLLoader

    @FXML // fx:id="pauseIcon"
    private ImageView pauseIcon; // Value injected by FXMLLoader

    @FXML // fx:id="stopIcon"
    private ImageView stopIcon; // Value injected by FXMLLoader

    @FXML // fx:id="modeChoiceBox"
    private ChoiceBox<Mode> modeChoiceBox; // Value injected by FXMLLoader

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
            UndoService undoService,
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
        this.undoService = undoService;
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
        DoubleProperty scrollbarTracker = new SimpleDoubleProperty();
        scrollbarTracker.bind(scrollPaneCenter.hvalueProperty());
        songEditor.initialize(new SongCallback() {
            @Override
            public AddResponse addNote(NoteData toAdd) throws NoteAlreadyExistsException {
                onSongChange();
                return song.get().addNote(toAdd);
            }

            @Override
            public RemoveResponse removeNote(int position) {
                onSongChange();
                return song.get().removeNote(position);
            }

            @Override
            public void modifyNote(NoteData toModify) {
                onSongChange();
                song.get().modifyNote(toModify);
            }

            @Override
            public void openNoteProperties(RegionBounds regionBounds) {
                openNotePropertiesEditor(regionBounds);
            }

            @Override
            public Mode getCurrentMode() {
                return currentMode;
            }

            @Override
            public void adjustScrollbar(double oldWidth, double newWidth) {
                // Note down what scrollbar position will be next time anchorCenter's width changes.
                double scrollPosition =
                        scrollPaneCenter.getHvalue() * (oldWidth - scrollPaneCenter.getWidth());
                scrollbarTracker.unbind();
                scrollbarTracker.set(scrollPosition / (newWidth - scrollPaneCenter.getWidth()));
            }
        });
        anchorCenter.widthProperty().addListener(event -> {
            // Sync up the scrollbar's position with where the editor thinks it should be.
            if (!scrollbarTracker.isBound()) {
                scrollPaneCenter.setHvalue(scrollbarTracker.get());
                scrollbarTracker.bind(scrollPaneCenter.hvalueProperty());
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

        modeChoiceBox.setItems(FXCollections.observableArrayList(Mode.ADD, Mode.EDIT, Mode.DELETE));
        modeChoiceBox.setOnAction((action) -> {
            currentMode = modeChoiceBox.getValue();
        });
        modeChoiceBox.setValue(Mode.EDIT);
        quantizeChoiceBox.setItems(FXCollections.observableArrayList("1/4", "1/8", "1/16", "1/32"));
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
            }
        });
        quantizeChoiceBox.setValue("1/16");

        rewindIcon.setImage(iconManager.getRewindImage(IconState.NORMAL));
        rewindIcon.setOnMousePressed(
                event -> rewindIcon.setImage(iconManager.getRewindImage(IconState.PRESSED)));
        rewindIcon.setOnMouseReleased(
                event -> rewindIcon.setImage(iconManager.getRewindImage(IconState.NORMAL)));
        playIcon.setImage(iconManager.getPlayImage(IconState.NORMAL));
        playIcon.setOnMousePressed(
                event -> playIcon.setImage(iconManager.getPlayImage(IconState.PRESSED)));
        playIcon.setOnMouseReleased(
                event -> playIcon.setImage(iconManager.getPlayImage(IconState.NORMAL)));
        pauseIcon.setImage(iconManager.getPauseImage(IconState.NORMAL));
        pauseIcon.setOnMousePressed(
                event -> pauseIcon.setImage(iconManager.getPauseImage(IconState.PRESSED)));
        pauseIcon.setOnMouseReleased(
                event -> pauseIcon.setImage(iconManager.getPauseImage(IconState.NORMAL)));
        stopIcon.setImage(iconManager.getStopImage(IconState.NORMAL));
        stopIcon.setOnMousePressed(
                event -> stopIcon.setImage(iconManager.getStopImage(IconState.PRESSED)));
        stopIcon.setOnMouseReleased(
                event -> stopIcon.setImage(iconManager.getStopImage(IconState.NORMAL)));

        languageChoiceBox.setItems(FXCollections.observableArrayList(localizer.getAllLocales()));
        languageChoiceBox
                .setOnAction((action) -> localizer.setLocale(languageChoiceBox.getValue()));
        languageChoiceBox.setValue(localizer.getCurrentLocale());

        refreshView();

        // Set up localization.
        localizer.localize(this);
    }

    @FXML
    private Label modeLabel; // Value injected by FXMLLoader
    @FXML
    private Label quantizationLabel; // Value injected by FXMLLoader
    @FXML
    private Button exportWavButton; // Value injected by FXMLLoader

    @Override
    public void localize(ResourceBundle bundle) {
        modeLabel.setText(bundle.getString("song.mode"));
        quantizationLabel.setText(bundle.getString("song.quantization"));
        exportWavButton.setText(bundle.getString("song.exportWav"));
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

        // Reloads current
        anchorCenter.getChildren().clear();
        anchorCenter.getChildren().add(songEditor.createNewTrack(song.get().getNotes()));
        anchorCenter.getChildren().add(songEditor.getNotesElement());
        anchorCenter.getChildren().add(songEditor.getPitchbendsElement());
        anchorCenter.getChildren().add(songEditor.getPlaybackElement());
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
    public void open() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select UST File");
        fc.getExtensionFilters().addAll(
                new ExtensionFilter("UST files", "*.ust"),
                new ExtensionFilter("All files", "*.*"));
        File file = fc.showOpenDialog(null);
        if (file != null) {
            try {
                song.setLocation(file);
            } catch (FileAlreadyOpenException e) {
                // TODO: Show alert to user.
                System.out.println("Error: Cannot open the same file in two tabs.");
                return;
            }
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
                    saveFormat = "UST 2.0 " + (charset.equals("UTF-8") ? "(UTF-8)" : "(Shift JIS)");
                } else {
                    // If no version found, assume UST 1.2 for now.
                    song.setSong(ust12Reader.loadSong(content));
                    saveFormat = "UST 1.2 (Shift JIS)";
                }
                undoService.clearActions();
                callback.enableSave(false);
                song.setSaveFormat(saveFormat);
                refreshView();
            } catch (IOException e) {
                // TODO Handle this better.
                errorLogger.logError(e);
            }
        }
    }

    @Override
    public void save() {
        callback.enableSave(false);
        if (song.hasPermanentLocation()) {
            String saveFormat = song.getSaveFormat();
            String charset = "UTF-8";
            if (saveFormat.contains("Shift JIS")) {
                charset = "SJIS";
            }
            File saveLocation = song.getLocation();
            try (PrintStream ps = new PrintStream(saveLocation, charset)) {
                if (saveFormat.contains("UST 1.2")) {
                    ust12Writer.writeSong(song.get(), ps);
                } else {
                    ust20Writer.writeSong(song.get(), ps, charset);
                }
                ps.flush();
                ps.close();
            } catch (FileNotFoundException | UnsupportedEncodingException e) {
                // TODO: Handle this.
                errorLogger.logError(e);
            }
        } else {
            // Default to "Save As" if no permanent location found.
            saveAs();
        }
    }

    @Override
    public void saveAs() {
        callback.enableSave(false);
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
            try {
                song.setLocation(file);
            } catch (FileAlreadyOpenException e) {
                // TODO: Show alert to user.
                System.out.println("Error: Cannot open the same file in two tabs.");
                return;
            }
            ExtensionFilter chosenFormat = fc.getSelectedExtensionFilter();
            String charset = "UTF-8";
            if (chosenFormat.getDescription().contains("Shift JIS")) {
                charset = "SJIS";
            }
            try (PrintStream ps = new PrintStream(file, charset)) {
                if (chosenFormat.getDescription().contains("UST 1.2")) {
                    ust12Writer.writeSong(song.get(), ps);
                } else {
                    ust20Writer.writeSong(song.get(), ps, charset);
                }
                ps.flush();
                ps.close();
            } catch (FileNotFoundException | UnsupportedEncodingException e) {
                // TODO: Handle this better.
                errorLogger.logError(e);
            }
            song.setSaveFormat(chosenFormat.getDescription());
        }
    }

    /** Called whenever a Song is changed. */
    private void onSongChange() {
        if (callback == null) {
            return;
        }
        callback.markChanged();
        if (song.hasPermanentLocation()) {
            callback.enableSave(true);
        } else {
            callback.enableSave(false);
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
                refreshView();
                songEditor.selectRegion(regionBounds);
            });
            propertiesWindow.setScene(new Scene(notePropertiesPane));
            propertiesWindow.showAndWait();
        } catch (IOException e) {
            // TODO Handle this.
            errorLogger.logError(e);
        }
    }

    @FXML
    void playSelection() {
        // If there is no track selected, play the whole song instead.
        RegionBounds selectedRegion = songEditor.getSelectedTrack();
        RegionBounds regionToPlay =
                selectedRegion.equals(RegionBounds.INVALID) ? RegionBounds.WHOLE_SONG
                        : selectedRegion;

        double tempo = song.get().getTempo();
        Function<Duration, Void> playbackFn =
                (duration) -> songEditor.startPlayback(selectedRegion, duration, tempo);

        // Disable the play button while rendering.
        playIcon.setDisable(true);
        playIcon.setImage(iconManager.getPlayImage(IconState.DISABLED));

        new Thread(() -> {
            engine.playSong(song.get(), playbackFn, regionToPlay);
            playIcon.setDisable(false);
            playIcon.setImage(iconManager.getPlayImage(IconState.NORMAL));
        }).start();
    }

    @FXML
    void exportSongAsWav(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select WAV File");
        fc.getExtensionFilters().addAll(new ExtensionFilter(".wav files", "*.wav"));
        File file = fc.showSaveDialog(null);
        if (file != null) {
            engine.renderWav(song.get(), file);
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
            controller.setData(song, () -> {
                onSongChange();
                refreshView();
            });
            propertiesWindow.setScene(new Scene(propertiesPane));
            propertiesWindow.showAndWait();
        } catch (IOException e) {
            // TODO Handle this.
            errorLogger.logError(e);
        }
    }

    @Override
    public void selectAll() {
        // Selects all notes.
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
