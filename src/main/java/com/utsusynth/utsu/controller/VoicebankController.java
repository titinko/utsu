package com.utsusynth.utsu.controller;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ResourceBundle;
import com.google.inject.Inject;
import com.utsusynth.utsu.common.UndoService;
import com.utsusynth.utsu.common.data.AddResponse;
import com.utsusynth.utsu.common.data.NoteData;
import com.utsusynth.utsu.common.data.RemoveResponse;
import com.utsusynth.utsu.common.exception.ErrorLogger;
import com.utsusynth.utsu.common.exception.NoteAlreadyExistsException;
import com.utsusynth.utsu.common.i18n.Localizable;
import com.utsusynth.utsu.common.i18n.Localizer;
import com.utsusynth.utsu.files.Ust12Writer;
import com.utsusynth.utsu.files.Ust20Writer;
import com.utsusynth.utsu.model.song.SongContainer;
import com.utsusynth.utsu.model.voicebank.VoicebankContainer;
import com.utsusynth.utsu.view.song.SongCallback;
import com.utsusynth.utsu.view.song.SongEditor;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;

/**
 * 'VoicebankScene.fxml' Controller Class
 */
public class VoicebankController implements EditorController, Localizable {
    private static final ErrorLogger errorLogger = ErrorLogger.getLogger();

    // User session data goes here.
    private EditorCallback callback;

    // Helper classes go here.
    private final SongContainer songContainer;
    private final VoicebankContainer voicebank;
    private final SongEditor track;
    private final Localizer localizer;
    private final UndoService undoService;
    private final Ust12Writer ust12Writer;
    private final Ust20Writer ust20Writer;

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

    @Inject
    public VoicebankController(
            SongContainer songContainer, // Inject an empty song.
            VoicebankContainer voicebankContainer, // Start with default voicebank.
            SongEditor track,
            Localizer localizer,
            UndoService undoService,
            Ust12Writer ust12Writer,
            Ust20Writer ust20Writer) {
        this.songContainer = songContainer;
        this.voicebank = voicebankContainer;
        this.track = track;
        this.localizer = localizer;
        this.undoService = undoService;
        this.ust12Writer = ust12Writer;
        this.ust20Writer = ust20Writer;
    }

    // Provide setup for other frontend song management.
    // This is called automatically when fxml loads.
    public void initialize() {
        track.initialize(new SongCallback() {
            @Override
            public AddResponse addNote(NoteData toAdd) throws NoteAlreadyExistsException {
                onSongChange();
                return songContainer.get().addNote(toAdd);
            }

            @Override
            public RemoveResponse removeNote(int position) {
                onSongChange();
                return songContainer.get().removeNote(position);
            }

            @Override
            public void modifyNote(NoteData toModify) {
                onSongChange();
                songContainer.get().modifyNote(toModify);
            }

            @Override
            public SongController.Mode getCurrentMode() {
                return null;
            }

            @Override
            public void adjustScrollbar(double oldWidth, double newWidth) {
                // Note down what scrollbar position will be next time anchorRight's width changes.
            }
        });

        refreshView();

        // Set up localization.
        localizer.localize(this);
    }

    @FXML
    private Label nameLabel; // Value injected by FXMLLoader
    @FXML
    private Label authorLabel; // Value injected by FXMLLoader
    @FXML
    private Button changeNameButton; // Value injected by FXMLLoader
    @FXML
    private Button changeAuthorButton; // Value injected by FXMLLoader

    @Override
    public void localize(ResourceBundle bundle) {
        nameLabel.setText(bundle.getString("top.mode"));
        authorLabel.setText(bundle.getString("top.quantization"));
        nameLabel.setText(bundle.getString("top.render"));
        authorLabel.setText(bundle.getString("top.exportWav"));
    }

    @Override
    public void refreshView() {
        // Set song image.
        Image image = new Image("file:" + songContainer.get().getVoicebank().getImagePath());
        voicebankImage.setImage(image);

        // Reloads current
        anchorRight.getChildren().clear();
        anchorRight.getChildren().add(track.createNewTrack(songContainer.get().getNotes()));
        anchorRight.getChildren().add(track.getNotesElement());
        anchorRight.getChildren().add(track.getPitchbendsElement());
        anchorRight.getChildren().add(track.getPlaybackElement());
        anchorBottom.getChildren().clear();
        anchorBottom.getChildren().add(track.getDynamicsElement());
        anchorBottom.getChildren().add(track.getEnvelopesElement());
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
            callback.enableSave(false);
            refreshView();
        }
        return voicebank.getLocation().getName();
    }

    @Override
    public String save() {
        callback.enableSave(false);
        if (songContainer.hasPermanentLocation()) {
            String saveFormat = songContainer.getSaveFormat();
            String charset = "UTF-8";
            if (saveFormat.contains("Shift JIS")) {
                charset = "SJIS";
            }
            File saveLocation = songContainer.getLocation();
            try (PrintStream ps = new PrintStream(saveLocation, charset)) {
                if (saveFormat.contains("UST 1.2")) {
                    ust12Writer.writeSong(songContainer.get(), ps);
                } else {
                    ust20Writer.writeSong(songContainer.get(), ps, charset);
                }
                ps.close();
            } catch (FileNotFoundException | UnsupportedEncodingException e) {
                // TODO: Handle this.
                errorLogger.logError(e);
            }
            return saveLocation.getName();
        }
        return "*Untitled";
    }

    @Override
    public String saveAs() {
        // TODO: Enable Save As for voicebank.
        return voicebank.getLocation().getName();
    }

    /** Called whenever a Song is changed. */
    private void onSongChange() {
        if (songContainer.hasPermanentLocation()) {
            callback.enableSave(true);
        } else {
            callback.enableSave(false);
        }
    }

    @Override
    public void openProperties() {
        // TODO: Implement properties for voicebank if necessary, no-op otherwise.
    }
}
