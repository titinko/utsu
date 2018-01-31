package com.utsusynth.utsu.controller;

import java.io.File;
import java.util.ResourceBundle;
import com.google.inject.Inject;
import com.utsusynth.utsu.common.UndoService;
import com.utsusynth.utsu.common.i18n.Localizable;
import com.utsusynth.utsu.common.i18n.Localizer;
import com.utsusynth.utsu.files.VoicebankWriter;
import com.utsusynth.utsu.model.voicebank.VoicebankContainer;
import com.utsusynth.utsu.view.voicebank.VoicebankEditor;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;

/**
 * 'VoicebankScene.fxml' Controller Class
 */
public class VoicebankController implements EditorController, Localizable {
    // User session data goes here.
    private EditorCallback callback;

    // Helper classes go here.
    private final VoicebankContainer voicebank;
    private final VoicebankEditor editor;
    private final Localizer localizer;
    private final UndoService undoService;
    private final VoicebankWriter voicebankWriter;

    @FXML // fx:id="anchorPitch"
    private AnchorPane anchorPitch; // Value injected by FXMLLoader

    @FXML // fx:id="anchorOto"
    private AnchorPane anchorOto; // Value injected by FXMLLoader

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

    @Inject
    public VoicebankController(
            VoicebankContainer voicebankContainer, // Start with default voicebank.
            VoicebankEditor editor,
            Localizer localizer,
            UndoService undoService,
            VoicebankWriter voicebankWriter) {
        this.voicebank = voicebankContainer;
        this.editor = editor;
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

        refreshView();

        // Set up localization.
        localizer.localize(this);
    }

    @FXML // fx:id="nameLabel"
    private Label nameLabel; // Value injected by FXMLLoader
    @FXML // fx:id="authorLabel"
    private Label authorLabel; // Value injected by FXMLLoader

    @Override
    public void localize(ResourceBundle bundle) {
        nameLabel.setText(bundle.getString("voice.name"));
        authorLabel.setText(bundle.getString("voice.author"));
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
        anchorOto.getChildren().clear();
        anchorOto.getChildren().add(editor.createNew(voicebank.get().getLyricConfigData()));
        // anchorRight.getChildren().clear();
        // anchorRight.getChildren().add(track.createNewTrack(songContainer.get().getNotes()));
        // anchorRight.getChildren().add(track.getNotesElement());
        // anchorRight.getChildren().add(track.getPitchbendsElement());
        // anchorRight.getChildren().add(track.getPlaybackElement());
        // anchorBottom.getChildren().clear();
        // anchorBottom.getChildren().add(track.getDynamicsElement());
        // anchorBottom.getChildren().add(track.getEnvelopesElement());
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

    /** Called whenever voicebank is changed. */
    private void onVoicebankChange() {
        // TODO: Add handling of the undo service.
        if (callback != null) {
            callback.enableSave(true);
        }
    }

    @Override
    public void openProperties() {
        // TODO: Implement properties for voicebank, for example whether oto should be foldered.
    }
}
