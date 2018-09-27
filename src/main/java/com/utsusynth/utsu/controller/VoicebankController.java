package com.utsusynth.utsu.controller;

import java.io.File;
import java.util.Iterator;
import java.util.ResourceBundle;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.utsusynth.utsu.common.StatusBar;
import com.utsusynth.utsu.common.UndoService;
import com.utsusynth.utsu.common.data.LyricConfigData;
import com.utsusynth.utsu.common.data.PitchMapData;
import com.utsusynth.utsu.common.exception.ErrorLogger;
import com.utsusynth.utsu.common.i18n.Localizable;
import com.utsusynth.utsu.common.i18n.Localizer;
import com.utsusynth.utsu.files.VoicebankWriter;
import com.utsusynth.utsu.model.voicebank.VoicebankContainer;
import com.utsusynth.utsu.view.voicebank.LyricConfigEditor;
import com.utsusynth.utsu.view.voicebank.PitchCallback;
import com.utsusynth.utsu.view.voicebank.PitchEditor;
import com.utsusynth.utsu.view.voicebank.VoicebankCallback;
import com.utsusynth.utsu.view.voicebank.VoicebankEditor;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.DirectoryChooser;

/**
 * 'VoicebankScene.fxml' Controller Class
 */
public class VoicebankController implements EditorController, Localizable {
    private static final ErrorLogger errorLogger = ErrorLogger.getLogger();

    // User session data goes here.
    private EditorCallback callback;

    // Helper classes go here.
    private final VoicebankContainer voicebank;
    private final VoicebankEditor voiceEditor;
    private final PitchEditor pitchEditor;
    private final LyricConfigEditor configEditor;
    private final Localizer localizer;
    private final UndoService undoService;
    private final StatusBar statusBar;
    private final VoicebankWriter voicebankWriter;

    @FXML // fx:id="pitchPane"
    private ScrollPane pitchPane; // Value injected by FXMLLoader

    @FXML // fx:id="otoPane"
    private HBox otoPane; // Value injected by FXMLLoader

    @FXML // fx:id="anchorBottom"
    private AnchorPane anchorBottom; // Value injected by FXMLLoader

    @FXML // fx:id="voicebankImage"
    private ImageView voicebankImage; // Value injected by FXMLLoader

    @FXML // fx:id="nameTextField"
    private TextField nameTextField; // Value injected by FXMLLoader

    @FXML // fx:id="authorTextField"
    private TextField authorTextField; // Value injected by FXMLLoader

    @FXML // fx:id="descriptionTextArea"
    private TextArea descriptionTextArea; // Value injected by FXMLLoader

    @FXML // fx:id="suffixTextField"
    private TextField suffixTextField;

    @Inject
    public VoicebankController(
            VoicebankContainer voicebankContainer, // Start with default voicebank.
            VoicebankEditor voiceEditor,
            PitchEditor pitchEditor,
            LyricConfigEditor configEditor,
            Localizer localizer,
            UndoService undoService,
            StatusBar statusBar,
            VoicebankWriter voicebankWriter) {
        this.voicebank = voicebankContainer;
        this.voiceEditor = voiceEditor;
        this.pitchEditor = pitchEditor;
        this.configEditor = configEditor;
        this.localizer = localizer;
        this.undoService = undoService;
        this.statusBar = statusBar;
        this.voicebankWriter = voicebankWriter;
    }

    // Provide setup for other frontend song management.
    // This is called automatically when fxml loads.
    public void initialize() {
        nameTextField.textProperty().addListener(event -> {
            String newText = nameTextField.getText();
            voicebank.mutate(voicebank.get().toBuilder().setName(newText).build());
            onVoicebankChange();
        });
        authorTextField.textProperty().addListener(event -> {
            String newText = authorTextField.getText();
            voicebank.mutate(voicebank.get().toBuilder().setAuthor(newText).build());
            onVoicebankChange();
        });
        descriptionTextArea.textProperty().addListener(event -> {
            String newText = descriptionTextArea.getText();
            voicebank.mutate(voicebank.get().toBuilder().setDescription(newText).build());
            onVoicebankChange();
        });

        // Pass callback to voicebank editor.
        voiceEditor.initialize(new VoicebankCallback() {
            @Override
            public Iterator<LyricConfigData> getLyricData(String category) {
                return voicebank.get().getLyricData(category);
            }

            @Override
            public void displayLyric(LyricConfigData lyricData) {
                // Load lyric config editor.
                anchorBottom.getChildren().clear();
                anchorBottom.getChildren().add(configEditor.createConfigEditor(lyricData));
                anchorBottom.getChildren().add(configEditor.getControlElement());
                anchorBottom.getChildren().add(configEditor.getChartElement());
                bindLabelsAndControlBars(configEditor.getControlElement());
            }

            @Override
            public boolean addLyric(LyricConfigData lyricData) {
                boolean wasSuccessful = voicebank.get().addLyricData(lyricData);
                onVoicebankChange();
                return wasSuccessful;
            }

            @Override
            public void removeLyric(String lyric) {
                voicebank.get().removeLyricConfig(lyric);
                onVoicebankChange();
            }

            @Override
            public void modifyLyric(LyricConfigData lyricData) {
                voicebank.get().modifyLyricData(lyricData);
                onVoicebankChange();
            }

            @Override
            public void generateFrqFiles(Iterator<LyricConfigData> lyricIterator) {
                statusBar.setStatus("Generating .frq files...");
                new Thread(() -> {
                    voicebank.get().generateFrqs(lyricIterator);
                    Platform.runLater(() -> statusBar.setStatus("Finished generating .frq files."));
                    // Change cannot be saved or undone, so don't call onVoicebankChange.
                }).start();
            }
        });

        // Pass callback to pitch editor.
        pitchEditor.initialize(new PitchCallback() {
            @Override
            public void setPitch(PitchMapData pitchData) {
                voicebank.get().setPitchData(pitchData);
                onVoicebankChange();
            }
        });

        refreshView();

        // Set up localization.
        localizer.localize(this);
    }

    @FXML // fx:id="nameLabel"
    private Label nameLabel; // Value injected by FXMLLoader
    @FXML // fx:id="authorLabel"
    private Label authorLabel; // Value injected by FXMLLoader
    @FXML // fx:id="applySuffixButton"
    private Button applySuffixButton; // Value injected by FXMLLoader
    @FXML // fx:id="offsetLabel"
    private Label offsetLabel; // Value injected by FXMLLoader
    @FXML // fx:id="cutoffLabel"
    private Label cutoffLabel; // Value injected by FXMLLoader
    @FXML // fx:id="consonantLabel"
    private Label consonantLabel; // Value injected by FXMLLoader
    @FXML // fx:id="preutteranceLabel"
    private Label preutteranceLabel; // Value injected by FXMLLoader
    @FXML // fx:id="overlapLabel"
    private Label overlapLabel; // Value injected by FXMLLoader

    @Override
    public void localize(ResourceBundle bundle) {
        nameLabel.setText(bundle.getString("voice.name"));
        authorLabel.setText(bundle.getString("voice.author"));
        applySuffixButton.setText(bundle.getString("general.apply"));
    }

    @Override
    public void refreshView() {
        // Set song image.
        try {
            Image image = new Image("file:" + voicebank.get().getImagePath());
            voicebankImage.setImage(image);
        } catch (Exception e) {
            System.out.println("Exception while loading voicebank image.");
            errorLogger.logWarning(e);
        }

        // Set name, author, and description.
        nameTextField.setText(voicebank.get().getName());
        authorTextField.setText(voicebank.get().getAuthor());
        descriptionTextArea.setText(voicebank.get().getDescription());

        // Reload voicebank editor.
        otoPane.getChildren().clear();
        otoPane.getChildren().add(voiceEditor.createNew(voicebank.get().getCategories()));

        // Reload pitch map editor.
        pitchPane.setContent(pitchEditor.createPitchView(voicebank.get().getPitchData()));

        // Remove lyric config editor.
        anchorBottom.getChildren().clear();
    }

    @Override
    public void openEditor(EditorCallback callback) {
        this.callback = callback;
    }

    @Override
    public void closeEditor() {
        // Remove this voicebank from memory, forcing songs using it to reload.
        voicebank.removeVoicebank();
    }

    @Override
    public String getFileName() {
        return voicebank.getLocation().getName();
    }

    @Override
    public boolean hasPermanentLocation() {
        return true;
    }

    @Override
    public boolean onKeyPressed(KeyEvent keyEvent) {
        if (new KeyCodeCombination(KeyCode.SPACE).match(keyEvent)) {
            configEditor.playSound(); // Does nothing if config editor not loaded.
            return true;
        } else {
            // No need to override default key behavior.
            return false;
        }
    }

    @Override
    public Optional<String> open() {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Select Voicebank Directory");
        File file = dc.showDialog(null);
        if (file != null) {
            statusBar.setStatus("Loading " + file.getName() + "...");
            new Thread(() -> {
                try {
                    voicebank.setVoicebank(file);
                    undoService.clearActions();
                    voicebank.get(); // Loads voicebank from file if necessary.
                    Platform.runLater(() -> {
                        // UI thread.
                        refreshView();
                        callback.enableSave(false);
                        statusBar.setStatus("Loaded voicebank: " + file.getName());
                    });
                } catch (Exception e) {
                    Platform.runLater(
                            () -> statusBar.setStatus(
                                    "Error: Unable to load voicebank: " + file.getName()));
                }
            }).start();
            return Optional.of(file.getName());
        }
        return Optional.absent();
    }

    @Override
    public Optional<String> save() {
        statusBar.setStatus("Saving...");
        new Thread(() -> {
            try {
                voicebankWriter.writeVoicebankToDirectory(voicebank.get(), voicebank.getLocation());
                Platform.runLater(() -> {
                    statusBar.setStatus(
                            "Saved changes to voicebank: " + voicebank.getLocation().getName());
                    callback.enableSave(false);
                });
            } catch (Exception e) {
                Platform.runLater(
                        () -> statusBar.setStatus("Error: Unable to save changes to voicebank."));
            }
        }).start();
        return Optional.absent();
    }

    @Override
    public Optional<String> saveAs() {
        // TODO: Enable Save As for voicebanks.
        return Optional.absent();
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
        // Voicebanks do not support cut/copy/paste right now.
    }

    @Override
    public void copy() {
        // Voicebanks do not support cut/copy/paste right now.
    }

    @Override
    public void paste() {
        // Voicebanks do not support cut/copy/paste right now.
    }

    @Override
    public void delete() {
        // Voicebanks do not support delete shortcut right now.
    }

    @Override
    public void selectAll() {
        if (descriptionTextArea.isFocused()) {
            descriptionTextArea.selectAll();
        } else {
            pitchEditor.selectAll();
        }
    }

    @FXML
    public void applySuffix(ActionEvent event) {
        String suffix = suffixTextField.getText() != null ? suffixTextField.getText() : "";
        pitchEditor.setSelected(suffix);
        onVoicebankChange();
    }

    /** Called whenever voicebank is changed. */
    private void onVoicebankChange() {
        // TODO: Add handling of the undo service.
        if (callback == null) {
            return;
        }
        callback.enableSave(true);
        // TODO: Refresh lyrics/envelopes after this.
    }

    private void bindLabelsAndControlBars(Group controlBars) {
        for (Node node : controlBars.getChildren()) {
            Line controlBar = (Line) node;
            Label label;
            if (controlBar.getStyleClass().contains("offset")) {
                label = offsetLabel;
            } else if (controlBar.getStyleClass().contains("cutoff")) {
                label = cutoffLabel;
            } else if (controlBar.getStyleClass().contains("consonant")) {
                label = consonantLabel;
            } else if (controlBar.getStyleClass().contains("preutterance")) {
                label = preutteranceLabel;
            } else {
                label = overlapLabel;
            }
            EventHandler<? super MouseEvent> onMouseEntered = controlBar.getOnMouseEntered();
            label.onMouseEnteredProperty().bindBidirectional(controlBar.onMouseEnteredProperty());
            label.setOnMouseEntered(event -> {
                onMouseEntered.handle(event);
                label.setFont(Font.font("verdana", FontWeight.EXTRA_BOLD, 12));
            });
            EventHandler<? super MouseEvent> onMouseExited = controlBar.getOnMouseExited();
            label.onMouseExitedProperty().bindBidirectional(controlBar.onMouseExitedProperty());
            label.setOnMouseExited(event -> {
                onMouseExited.handle(event);
                label.setFont(Font.font("verdana", FontWeight.NORMAL, 12));
            });
        }
    }

    @Override
    public void exportToWav() {
        // Voicebanks cannot be exported to WAV files.
    }

    @Override
    public void openProperties() {
        // TODO: Implement properties for voicebank, for example whether oto should be foldered.
    }

    @Override
    public Optional<File> openPlugin() {
        // Voicebanks do not have plugins of their own right now.
        return Optional.absent();
    }

    @Override
    public void invokePlugin(File plugin) {
        // Voicebanks do not have plugins of their own right now.
    }
}
