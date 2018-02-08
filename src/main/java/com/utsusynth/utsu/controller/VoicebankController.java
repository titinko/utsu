package com.utsusynth.utsu.controller;

import java.io.File;
import java.util.Iterator;
import java.util.ResourceBundle;
import com.google.inject.Inject;
import com.utsusynth.utsu.common.UndoService;
import com.utsusynth.utsu.common.data.LyricConfigData;
import com.utsusynth.utsu.common.data.PitchMapData;
import com.utsusynth.utsu.common.i18n.Localizable;
import com.utsusynth.utsu.common.i18n.Localizer;
import com.utsusynth.utsu.files.VoicebankWriter;
import com.utsusynth.utsu.model.voicebank.VoicebankContainer;
import com.utsusynth.utsu.view.voicebank.LyricConfigEditor;
import com.utsusynth.utsu.view.voicebank.PitchCallback;
import com.utsusynth.utsu.view.voicebank.PitchEditor;
import com.utsusynth.utsu.view.voicebank.VoicebankCallback;
import com.utsusynth.utsu.view.voicebank.VoicebankEditor;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.stage.DirectoryChooser;

/**
 * 'VoicebankScene.fxml' Controller Class
 */
public class VoicebankController implements EditorController, Localizable {
    // User session data goes here.
    private EditorCallback callback;

    // Helper classes go here.
    private final VoicebankContainer voicebank;
    private final VoicebankEditor voiceEditor;
    private final PitchEditor pitchEditor;
    private final LyricConfigEditor configEditor;
    private final Localizer localizer;
    private final UndoService undoService;
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
            VoicebankWriter voicebankWriter) {
        this.voicebank = voicebankContainer;
        this.voiceEditor = voiceEditor;
        this.pitchEditor = pitchEditor;
        this.configEditor = configEditor;
        this.localizer = localizer;
        this.undoService = undoService;
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

    @Override
    public void localize(ResourceBundle bundle) {
        nameLabel.setText(bundle.getString("voice.name"));
        authorLabel.setText(bundle.getString("voice.author"));
        applySuffixButton.setText(bundle.getString("voice.applySuffix"));
    }

    @Override
    public void refreshView() {
        // Set song image.
        Image image = new Image("file:" + voicebank.get().getImagePath());
        voicebankImage.setImage(image);

        // Set name, author, and description.
        nameTextField.setText(voicebank.get().getName());
        authorTextField.setText(voicebank.get().getAuthor());
        descriptionTextArea.setText(voicebank.get().getDescription());

        // Reload voicebank editor.
        otoPane.getChildren().clear();
        otoPane.getChildren().add(voiceEditor.createNew(voicebank.get().getCategories()));

        // Reload pitch map editor.
        pitchPane.setContent(pitchEditor.createPitchView(voicebank.get().getPitchData()));
    }

    @Override
    public void openEditor(EditorCallback callback) {
        this.callback = callback;
    }

    @Override
    public String open() {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Select Voicebank Directory");
        File file = dc.showDialog(null);
        if (file != null) {
            voicebank.setVoicebank(file);
            undoService.clearActions();
            refreshView();
            callback.enableSave(false);
        }
        return voicebank.getLocation().getName();
    }

    @Override
    public String save() {
        callback.enableSave(false);
        voicebankWriter.writeVoicebankToDirectory(voicebank.get(), voicebank.getLocation());
        return voicebank.getLocation().getName();
    }

    @Override
    public String saveAs() {
        // TODO: Enable Save As for voicebank.
        return voicebank.getLocation().getName();
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
        if (callback != null) {
            callback.enableSave(true);
        }
        // TODO: Refresh lyrics/envelopes after this.
    }

    @Override
    public void openProperties() {
        // TODO: Implement properties for voicebank, for example whether oto should be foldered.
    }
}
