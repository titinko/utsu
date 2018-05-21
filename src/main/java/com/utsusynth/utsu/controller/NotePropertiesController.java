package com.utsusynth.utsu.controller;

import java.io.File;
import java.util.ResourceBundle;
import com.google.inject.Inject;
import com.utsusynth.utsu.common.i18n.Localizable;
import com.utsusynth.utsu.common.i18n.Localizer;
import com.utsusynth.utsu.engine.Engine;
import com.utsusynth.utsu.model.song.SongContainer;
import com.utsusynth.utsu.model.voicebank.VoicebankContainer;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

/**
 * 'NotePropertiesScene.fxml' Controller Class
 */
public class NotePropertiesController implements Localizable {
    private final Engine engine;
    private final Localizer localizer;
    private final VoicebankContainer voicebankContainer;

    private SongContainer songContainer;
    private File resamplerPath;
    private File wavtoolPath;

    @FXML // fx:id="titleLabel"
    private Label titleLabel; // Value injected by FXMLLoader

    @FXML // fx:id="consonantVelocityLabel"
    private Label consonantVelocityLabel; // Value injected by FXMLLoader

    @FXML // fx:id="consonantVelocitySlider"
    private Slider consonantVelocitySlider; // Value injected by FXMLLoader

    @FXML // fx:id="curConsonantVelocity"
    private Label curConsonantVelocity; // Value injected by FXMLLoader

    @FXML // fx:id="preutterLabel"
    private Label preutterLabel; // Value injected by FXMLLoader

    @FXML // fx:id="preutterSlider"
    private Slider preutterSlider; // Value injected by FXMLLoader

    @FXML // fx:id="curPreutter"
    private Label curPreutter; // Value injected by FXMLLoader

    @FXML // fx:id="overlapLabel"
    private Label overlapLabel; // Value injected by FXMLLoader

    @FXML // fx:id="overlapSlider"
    private Slider overlapSlider; // Value injected by FXMLLoader

    @FXML // fx:id="curOverlap"
    private Label curOverlap; // Value injected by FXMLLoader

    @FXML // fx:id="startPointLabel"
    private Label startPointLabel;

    @FXML // fx:id="startPointSlider"
    private Slider startPointSlider; // Value injected by FXMLLoader

    @FXML // fx:id="curStartPoint"
    private Label curStartPoint; // Value injected by FXMLLoader

    @FXML // fx:id="tempoLabel"
    private Label tempoLabel; // Value injected by FXMLLoader

    @FXML // fx:id="tempoSlider"
    private Slider tempoSlider; // Value injected by FXMLLoader

    @FXML // fx:id="curTempo"
    private Label curTempo; // Value injected by FXMLLoader

    @FXML // fx:id="intensityLabel"
    private Label intensityLabel; // Value injected by FXMLLoader

    @FXML // fx:id="intensitySlider"
    private Slider intensitySlider; // Value injected by FXMLLoader

    @FXML // fx:id="curIntensity"
    private Label curIntensity; // Value injected by FXMLLoader

    @FXML // fx:id="flagsLabel"
    private Label flagsLabel; // Value injected by FXMLLoader

    @FXML // fx:id="curFlags"
    private Label curFlags; // Value injected by FXMLLoader

    @Inject
    public NotePropertiesController(
            Engine engine,
            Localizer localizer,
            VoicebankContainer voicebankContainer) {
        this.engine = engine;
        this.localizer = localizer;
        this.voicebankContainer = voicebankContainer;
    }

    public void initialize() {
        // Set up localization.
        localizer.localize(this);
    }

    /* Initializes properties panel with a SongContainer with the song to edit. */
    void setSongContainer(SongContainer songContainer) {
        this.songContainer = songContainer;

        // Set values to save.
        resamplerPath = engine.getResamplerPath();
        wavtoolPath = engine.getWavtoolPath();
        voicebankContainer.setVoicebank(songContainer.get().getVoiceDir());

        // Set current values.
        curFlags.setText(songContainer.get().getFlags());

        // Setup tempo slider.
        tempoSlider.valueProperty().addListener((event) -> {
            int sliderValue = (int) Math.round(tempoSlider.getValue());
            tempoSlider.setValue(sliderValue);
            curTempo.setText(Integer.toString(sliderValue));
        });
        tempoSlider.setValue(songContainer.get().getTempo());
    }

    @Override
    public void localize(ResourceBundle bundle) {
        consonantVelocityLabel.setText(bundle.getString("properties.projectName"));
        preutterLabel.setText(bundle.getString("properties.outputFile"));
        overlapLabel.setText(bundle.getString("properties.flags"));
        startPointLabel.setText(bundle.getString("properties.resampler"));
        tempoLabel.setText(bundle.getString("properties.tempo"));
        intensityLabel.setText(bundle.getString("properties.voicebank"));
        flagsLabel.setText(bundle.getString("properties.tempo"));
    }

    @FXML
    void changeResampler(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select executable file");
        fc.getExtensionFilters().addAll(
                new ExtensionFilter("Executables", "*", "*.exe"),
                new ExtensionFilter("OSX Executables", "*.out", "*.app"),
                new ExtensionFilter("All Files", "*.*"));
        File file = fc.showOpenDialog(null);
        if (file != null) {
            resamplerPath = file;
            curFlags.setText(resamplerPath.getName());
        }
    }

    @FXML
    void changeWavtool(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select executable file");
        fc.getExtensionFilters().addAll(
                new ExtensionFilter("Executables", "*", "*.exe"),
                new ExtensionFilter("OSX Executables", "*.out", "*.app"),
                new ExtensionFilter("All Files", "*.*"));
        File file = fc.showOpenDialog(null);
        if (file != null) {
            wavtoolPath = file;
            curFlags.setText(wavtoolPath.getName());
        }
    }

    @FXML
    void changeVoicebank(ActionEvent event) {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Select voicebank");
        File file = dc.showDialog(null);
        if (file != null) {
            voicebankContainer.setVoicebank(file);
            curFlags.setText(voicebankContainer.get().getName());
        }
    }

    @FXML
    void restoreDefaults(ActionEvent event) {
        songContainer.setSong(
                songContainer.get().toBuilder().setVoiceDirectory(voicebankContainer.getLocation())
                        .setTempo((int) Math.round(tempoSlider.getValue())).build());
        engine.setResamplerPath(resamplerPath);
        engine.setWavtoolPath(wavtoolPath);
    }

    @FXML
    void closeProperties(ActionEvent event) {
        // TODO
    }
}
