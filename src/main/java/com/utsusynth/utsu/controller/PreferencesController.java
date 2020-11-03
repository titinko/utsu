package com.utsusynth.utsu.controller;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.utsusynth.utsu.common.RegionBounds;
import com.utsusynth.utsu.common.data.NoteConfigData;
import com.utsusynth.utsu.common.exception.ErrorLogger;
import com.utsusynth.utsu.common.i18n.Localizable;
import com.utsusynth.utsu.common.i18n.Localizer;
import com.utsusynth.utsu.common.utils.RoundUtils;
import com.utsusynth.utsu.files.PreferencesManager;
import com.utsusynth.utsu.model.song.Note;
import com.utsusynth.utsu.model.song.NoteIterator;
import com.utsusynth.utsu.model.song.SongContainer;
import com.utsusynth.utsu.model.voicebank.LyricConfig;
import com.utsusynth.utsu.model.voicebank.Voicebank;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.text.MessageFormat;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * 'PreferencesScene.fxml' Controller Class
 */
public class PreferencesController implements Localizable {
    private final PreferencesManager preferencesManager;
    private final Localizer localizer;

    @FXML // fx:id="root"
    private BorderPane root; // Value injected by FXMLLoader

    @FXML // fx:id="applyButton"
    private Button applyButton; // Value injected by FXMLLoader

    @FXML // fx:id="cancelButton"
    private Button cancelButton; // Value injected by FXMLLoader

    @FXML // fx:id="okButton"
    private Button okButton; // Value injected by FXMLLoader

    @Inject
    public PreferencesController(PreferencesManager preferencesManager, Localizer localizer) {
        this.preferencesManager = preferencesManager;
        this.localizer = localizer;
    }

    public void initialize() {
        // Set up localization.
        localizer.localize(this);

        // Other setup.
    }

    @Override
    public void localize(ResourceBundle bundle) {
        applyButton.setText(bundle.getString("general.apply"));
        cancelButton.setText(bundle.getString("general.cancel"));
        okButton.setText("ok");
    }

    @FXML
    void closePreferences(ActionEvent event) {
        // Restore from file?
        Stage currentStage = (Stage) root.getScene().getWindow();
        currentStage.close();
    }

    @FXML
    void applyPreferences(ActionEvent event) {
        preferencesManager.save();
    }

    @FXML
    void applyAndClose(ActionEvent event) {
        applyPreferences(null);
        closePreferences(null);
    }
}
