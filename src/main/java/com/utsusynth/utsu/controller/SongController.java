package com.utsusynth.utsu.controller;

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
import com.google.inject.Inject;
import com.google.inject.Provider;
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
import com.utsusynth.utsu.engine.Engine;
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
    private final Provider<FXMLLoader> fxmlLoaderProvider;

    @FXML // fx:id="scrollPaneLeft"
    private ScrollPane scrollPaneLeft; // Value injected by FXMLLoader

    @FXML // fx:id="anchorLeft"
    private AnchorPane anchorLeft; // Value injected by FXMLLoader

    @FXML // fx:id="scrollPaneRight"
    private ScrollPane scrollPaneRight; // Value injected by FXMLLoader

    @FXML // fx:id="anchorRight"
    private AnchorPane anchorRight; // Value injected by FXMLLoader

    @FXML // fx:id="scrollPaneBottom"
    private ScrollPane scrollPaneBottom; // Value injected by FXMLLoader

    @FXML // fx:id="anchorBottom"
    private AnchorPane anchorBottom; // Value injected by FXMLLoader

    @FXML // fx:id="voicebankImage"
    private ImageView voicebankImage; // Value injected by FXMLLoader

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
        this.fxmlLoaderProvider = fxmlLoaders;
    }

    // Provide setup for other frontend song management.
    // This is called automatically when fxml loads.
    public void initialize() {
        DoubleProperty scrollbarTracker = new SimpleDoubleProperty();
        scrollbarTracker.bind(scrollPaneRight.hvalueProperty());
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
            public Mode getCurrentMode() {
                return currentMode;
            }

            @Override
            public void adjustScrollbar(double oldWidth, double newWidth) {
                // Note down what scrollbar position will be next time anchorRight's width changes.
                double scrollPosition =
                        scrollPaneRight.getHvalue() * (oldWidth - scrollPaneRight.getWidth());
                scrollbarTracker.unbind();
                scrollbarTracker.set(scrollPosition / (newWidth - scrollPaneRight.getWidth()));
            }
        });
        anchorRight.widthProperty().addListener(event -> {
            // Sync up the scrollbar's position with where the editor thinks it should be.
            if (!scrollbarTracker.isBound()) {
                scrollPaneRight.setHvalue(scrollbarTracker.get());
                scrollbarTracker.bind(scrollPaneRight.hvalueProperty());
            }
        });
        scrollPaneLeft.vvalueProperty().bindBidirectional(scrollPaneRight.vvalueProperty());
        scrollPaneRight.hvalueProperty().bindBidirectional(scrollPaneBottom.hvalueProperty());
        scrollPaneRight.hvalueProperty().addListener(event -> {
            double hvalue = scrollPaneRight.getHvalue();
            double margin = scrollPaneRight.getViewportBounds().getWidth();
            songEditor.selectivelyShowRegion(hvalue, margin);
        });
        scrollPaneRight.viewportBoundsProperty().addListener((event, oldValue, newValue) -> {
            if (oldValue.getWidth() != newValue.getWidth()) {
                double hvalue = scrollPaneRight.getHvalue();
                double margin = scrollPaneRight.getViewportBounds().getWidth();
                songEditor.selectivelyShowRegion(hvalue, margin);
            }
        });

        modeChoiceBox.setItems(FXCollections.observableArrayList(Mode.ADD, Mode.EDIT, Mode.DELETE));
        modeChoiceBox.setOnAction((action) -> {
            currentMode = modeChoiceBox.getValue();
        });
        modeChoiceBox.setValue(Mode.ADD);
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
        quantizeChoiceBox.setValue("1/4");

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
    private Button renderButton; // Value injected by FXMLLoader
    @FXML
    private Button exportWavButton; // Value injected by FXMLLoader

    @Override
    public void localize(ResourceBundle bundle) {
        modeLabel.setText(bundle.getString("song.mode"));
        quantizationLabel.setText(bundle.getString("song.quantization"));
        renderButton.setText(bundle.getString("song.render"));
        exportWavButton.setText(bundle.getString("song.exportWav"));
    }

    @Override
    public void refreshView() {
        // Set song image.
        Image image = new Image("file:" + song.get().getVoicebank().getImagePath());
        voicebankImage.setImage(image);

        anchorLeft.getChildren().add(piano.initPiano());

        // Reloads current
        anchorRight.getChildren().clear();
        anchorRight.getChildren().add(songEditor.createNewTrack(song.get().getNotes()));
        anchorRight.getChildren().add(songEditor.getNotesElement());
        anchorRight.getChildren().add(songEditor.getPitchbendsElement());
        anchorRight.getChildren().add(songEditor.getPlaybackElement());
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

    @FXML
    void renderSong(ActionEvent event) {
        double tempo = song.get().getTempo();
        Function<Duration, Void> playbackFn =
                (duration) -> songEditor.startPlayback(duration, tempo);

        // Disable the render button while rendering.
        renderButton.setDisable(true);
        new Thread(() -> {
            engine.playSong(song.get(), playbackFn, songEditor.getSelectedTrack());
            renderButton.setDisable(false);
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
        // Open properties modal.
        InputStream fxml = getClass().getResourceAsStream("/fxml/PropertiesScene.fxml");
        FXMLLoader loader = fxmlLoaderProvider.get();
        try {
            Stage currentStage = (Stage) anchorRight.getScene().getWindow();
            Stage propertiesWindow = new Stage();
            propertiesWindow.initModality(Modality.APPLICATION_MODAL);
            propertiesWindow.initOwner(currentStage);
            BorderPane propertiesPane = loader.load(fxml);
            PropertiesController controller = (PropertiesController) loader.getController();
            controller.setSongContainer(song);
            propertiesWindow.setScene(new Scene(propertiesPane));
            propertiesWindow.showAndWait();
        } catch (IOException e) {
            // TODO Handle this.
            errorLogger.logError(e);
        }
        refreshView();
    }
}
