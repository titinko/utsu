package com.utsusynth.utsu;

import static javafx.scene.input.KeyCombination.CONTROL_DOWN;
import static javafx.scene.input.KeyCombination.SHIFT_DOWN;
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
import com.utsusynth.utsu.common.UndoService;
import com.utsusynth.utsu.common.data.AddResponse;
import com.utsusynth.utsu.common.data.NoteData;
import com.utsusynth.utsu.common.data.RemoveResponse;
import com.utsusynth.utsu.common.exception.ErrorLogger;
import com.utsusynth.utsu.common.exception.NoteAlreadyExistsException;
import com.utsusynth.utsu.common.i18n.Localizable;
import com.utsusynth.utsu.common.i18n.Localizer;
import com.utsusynth.utsu.common.i18n.NativeLocale;
import com.utsusynth.utsu.common.quantize.Quantizer;
import com.utsusynth.utsu.common.quantize.Scaler;
import com.utsusynth.utsu.engine.Engine;
import com.utsusynth.utsu.files.Ust12Reader;
import com.utsusynth.utsu.files.Ust12Writer;
import com.utsusynth.utsu.files.Ust20Reader;
import com.utsusynth.utsu.files.Ust20Writer;
import com.utsusynth.utsu.model.SongManager;
import com.utsusynth.utsu.view.Piano;
import com.utsusynth.utsu.view.Track;
import com.utsusynth.utsu.view.ViewCallback;
import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.shape.Line;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * 'UtsuScene.fxml' Controller Class
 */
public class UtsuController implements Localizable {
    private static final ErrorLogger errorLogger = ErrorLogger.getLogger();

    public enum Mode {
        ADD, EDIT, DELETE,
    }

    // User session data goes here.
    private Mode currentMode;

    // Helper classes go here.
    private final SongManager songManager;
    private final Engine engine;
    private final Track track;
    private final Piano piano;
    private final Localizer localizer;
    private final Quantizer quantizer;
    private final Scaler scaler;
    private final UndoService undoService;
    private final Ust12Reader ust12Reader;
    private final Ust12Writer ust12Writer;
    private final Ust20Reader ust20Reader;
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
    public UtsuController(
            SongManager songManager,
            Engine engine,
            Track track,
            Piano piano,
            Localizer localizer,
            Quantizer quantizer,
            Scaler scaler,
            UndoService undoService,
            Ust12Reader ust12Reader,
            Ust12Writer ust12Writer,
            Ust20Reader ust20Reader,
            Ust20Writer ust20Writer,
            Provider<FXMLLoader> fxmlLoaders) {
        this.songManager = songManager;
        this.engine = engine;
        this.track = track;
        this.piano = piano;
        this.localizer = localizer;
        this.quantizer = quantizer;
        this.scaler = scaler;
        this.undoService = undoService;
        this.ust12Reader = ust12Reader;
        this.ust12Writer = ust12Writer;
        this.ust20Reader = ust20Reader;
        this.ust20Writer = ust20Writer;
        this.fxmlLoaderProvider = fxmlLoaders;
    }

    // Provide setup for other controllers.
    public void initialize() {
        DoubleProperty scrollbarTracker = new SimpleDoubleProperty();
        scrollbarTracker.bind(scrollPaneRight.hvalueProperty());
        track.initialize(new ViewCallback() {
            @Override
            public AddResponse addNote(NoteData toAdd) throws NoteAlreadyExistsException {
                onSongChange();
                return songManager.getSong().addNote(toAdd);
            }

            @Override
            public RemoveResponse removeNote(int position) {
                onSongChange();
                return songManager.getSong().removeNote(position);
            }

            @Override
            public void modifyNote(NoteData toModify) {
                onSongChange();
                songManager.getSong().modifyNote(toModify);
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
        scrollPaneLeft.vvalueProperty().bindBidirectional(scrollPaneRight.vvalueProperty());
        scrollPaneRight.hvalueProperty().bindBidirectional(scrollPaneBottom.hvalueProperty());
        anchorRight.widthProperty().addListener(observable -> {
            // Sync up the scrollbar's position with where the track thinks it should be.
            if (!scrollbarTracker.isBound()) {
                scrollPaneRight.setHvalue(scrollbarTracker.get());
                scrollbarTracker.bind(scrollPaneRight.hvalueProperty());
            }
        });

        modeChoiceBox.setItems(FXCollections.observableArrayList(Mode.ADD, Mode.EDIT, Mode.DELETE));
        modeChoiceBox.setOnAction((action) -> {
            currentMode = modeChoiceBox.getValue();
        });
        modeChoiceBox.setValue(Mode.ADD);
        quantizeChoiceBox.setItems(
                FXCollections.observableArrayList(
                        "1 per beat",
                        "2 per beat",
                        "4 per beat",
                        "8 per beat"));
        quantizeChoiceBox.setOnAction((action) -> {
            String quantization = quantizeChoiceBox.getValue();
            if (quantization.equals("1 per beat")) {
                quantizer.changeQuant(quantizer.getQuant(), 1);
            } else if (quantization.equals("2 per beat")) {
                quantizer.changeQuant(quantizer.getQuant(), 2);
            } else if (quantization.equals("4 per beat")) {
                quantizer.changeQuant(quantizer.getQuant(), 4);
            } else if (quantization.equals("8 per beat")) {
                quantizer.changeQuant(quantizer.getQuant(), 8);
            }
        });
        quantizeChoiceBox.setValue("1 per beat");

        languageChoiceBox.setItems(FXCollections.observableArrayList(localizer.getAllLocales()));
        languageChoiceBox
                .setOnAction((action) -> localizer.setLocale(languageChoiceBox.getValue()));
        languageChoiceBox.setValue(localizer.getCurrentLocale());

        refreshView();

        // Set up localization.
        localizer.localize(this);
    }

    @FXML
    private Menu fileMenu; // Value injected by FXMLLoader
    @FXML
    private MenuItem openItem; // Value injected by FXMLLoader
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
        fileMenu.setText(bundle.getString("menu.file"));
        openItem.setText(bundle.getString("menu.file.open"));
        saveItem.setText(bundle.getString("menu.file.save"));
        saveItem.setAccelerator(new KeyCodeCombination(KeyCode.S, CONTROL_DOWN));
        saveAsItem.setText(bundle.getString("menu.file.saveAs"));
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
        modeLabel.setText(bundle.getString("top.mode"));
        quantizationLabel.setText(bundle.getString("top.quantization"));
        renderButton.setText(bundle.getString("top.render"));
        exportWavButton.setText(bundle.getString("top.exportWav"));

        // Force the menu to refresh.
        fileMenu.setVisible(false);
        fileMenu.setVisible(true);
    }

    private void refreshView() {
        // Set song image.
        Image image = new Image("file:" + songManager.getSong().getVoicebank().getImagePath());
        voicebankImage.setImage(image);

        anchorLeft.getChildren().add(piano.initPiano());

        // Reloads current
        anchorRight.getChildren().clear();
        anchorRight.getChildren().add(track.createNewTrack(songManager.getSong().getNotes()));
        anchorRight.getChildren().add(track.getNotesElement());
        anchorRight.getChildren().add(track.getPitchbendsElement());
        anchorBottom.getChildren().clear();
        anchorBottom.getChildren().add(track.getDynamicsElement());
        anchorBottom.getChildren().add(track.getEnvelopesElement());
    }

    /** Called whenever a Song is changed. */
    private void onSongChange() {
        if (songManager.getSong().getSaveLocation().isPresent()) {
            saveItem.setDisable(false);
        } else {
            saveItem.setDisable(true);
        }
    }

    /**
     * Called whenever Utsu is closed.
     * 
     * @return true if window should be closed, false otherwise
     */
    boolean onCloseWindow() {
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
    void openFile(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select UST File");
        fc.getExtensionFilters().addAll(
                new ExtensionFilter("UST files", "*.ust"),
                new ExtensionFilter("All files", "*.*"));
        File file = fc.showOpenDialog(null);
        if (file != null) {
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
                    songManager.setSong(ust12Reader.loadSong(content));
                    saveFormat = "UST 1.2 (Shift JIS)";
                } else if (content.contains("UST Version2.0")) {
                    songManager.setSong(ust20Reader.loadSong(content));
                    saveFormat = "UST 2.0 " + (charset.equals("UTF-8") ? "(UTF-8)" : "(Shift JIS)");
                } else {
                    // TODO: Deal with this error.
                    System.out.println("UST format not found!");
                    return;
                }
                undoService.clearActions();
                saveItem.setDisable(true);
                songManager.getSong().setSaveLocation(file);
                songManager.getSong().setSaveFormat(saveFormat);
                refreshView();
            } catch (IOException e) {
                // TODO Handle this.
                errorLogger.logError(e);
            }
        }
    }

    @FXML
    void saveFile(ActionEvent event) {
        if (songManager.getSong().getSaveLocation().isPresent()) {
            String saveFormat = songManager.getSong().getSaveFormat();
            String charset = "UTF-8";
            if (saveFormat.contains("Shift JIS")) {
                charset = "SJIS";
            }
            File saveLocation = songManager.getSong().getSaveLocation().get();
            try (PrintStream ps = new PrintStream(saveLocation, charset)) {
                if (saveFormat.contains("UST 1.2")) {
                    ust12Writer.writeSong(songManager.getSong(), ps);
                } else {
                    ust20Writer.writeSong(songManager.getSong(), ps, charset);
                }
                ps.close();
            } catch (FileNotFoundException | UnsupportedEncodingException e) {
                // TODO: Handle this.
                errorLogger.logError(e);
            }
        }
        saveItem.setDisable(true);
    }

    @FXML
    void saveFileAs(ActionEvent event) {
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
            ExtensionFilter chosenFormat = fc.getSelectedExtensionFilter();
            String charset = "UTF-8";
            if (chosenFormat.getDescription().contains("Shift JIS")) {
                charset = "SJIS";
            }
            try (PrintStream ps = new PrintStream(file, charset)) {
                if (chosenFormat.getDescription().contains("UST 1.2")) {
                    ust12Writer.writeSong(songManager.getSong(), ps);
                } else {
                    ust20Writer.writeSong(songManager.getSong(), ps, charset);
                }
                ps.close();
            } catch (FileNotFoundException | UnsupportedEncodingException e) {
                // TODO: Handle this.
                errorLogger.logError(e);
            }
            songManager.getSong().setSaveLocation(file);
            songManager.getSong().setSaveFormat(chosenFormat.getDescription());
            saveItem.setDisable(true);
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
        refreshView();
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
        refreshView();
    }

    @FXML
    void renderSong(ActionEvent event) {
        Function<Duration, Void> playbackFn = (duration) -> {
            if (duration != Duration.UNKNOWN && duration != Duration.INDEFINITE) {
                // Create a playback bar.
                Line playBar = new Line(0, 0, 0, anchorRight.getHeight());
                playBar.getStyleClass().addAll("playback-bar");
                anchorRight.getChildren().add(playBar);

                // Move the playback bar as the song plays.
                TranslateTransition playback = new TranslateTransition(duration, playBar);
                double numBeats = songManager.getSong().getTempo() * duration.toMinutes();
                playback.setByX(numBeats * scaler.scaleX(Quantizer.COL_WIDTH));
                playback.setInterpolator(Interpolator.LINEAR);
                playback.setOnFinished(action -> {
                    anchorRight.getChildren().remove(playBar);
                });
                playback.play();
            }
            return null;
        };

        // Disable the render button while rendering.
        renderButton.setDisable(true);
        new Thread(() -> {
            engine.playSong(songManager.getSong(), playbackFn);
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
            engine.renderWav(songManager.getSong(), file);
        }
    }

    @FXML
    void openProperties(ActionEvent event) {
        // Open properties modal.
        InputStream fxml = getClass().getResourceAsStream("/fxml/PropertiesScene.fxml");
        FXMLLoader loader = fxmlLoaderProvider.get();
        try {
            Stage currentStage = (Stage) anchorRight.getScene().getWindow();
            Stage propertiesWindow = new Stage();
            propertiesWindow.initModality(Modality.APPLICATION_MODAL);
            propertiesWindow.initOwner(currentStage);
            BorderPane propertiesPane = loader.load(fxml);
            propertiesWindow.setScene(new Scene(propertiesPane));
            propertiesWindow.showAndWait();
        } catch (IOException e) {
            // TODO Handle this.
            errorLogger.logError(e);
        }
        refreshView();
    }
}
