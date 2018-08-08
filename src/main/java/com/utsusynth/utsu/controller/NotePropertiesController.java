package com.utsusynth.utsu.controller;

import java.util.ResourceBundle;
import com.google.inject.Inject;
import com.utsusynth.utsu.common.RegionBounds;
import com.utsusynth.utsu.common.RoundUtils;
import com.utsusynth.utsu.common.i18n.Localizable;
import com.utsusynth.utsu.common.i18n.Localizer;
import com.utsusynth.utsu.model.song.Note;
import com.utsusynth.utsu.model.song.NoteIterator;
import com.utsusynth.utsu.model.song.SongContainer;
import com.utsusynth.utsu.model.voicebank.VoicebankContainer;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/**
 * 'NotePropertiesScene.fxml' Controller Class
 */
public class NotePropertiesController implements Localizable {
    private final Localizer localizer;
    private final VoicebankContainer voicebankContainer;

    private SongContainer songContainer;

    @FXML // fx:id="root"
    private BorderPane root; // Value injected by FXMLLoader

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

    @FXML // fx:id="intensityLabel"
    private Label intensityLabel; // Value injected by FXMLLoader

    @FXML // fx:id="intensitySlider"
    private Slider intensitySlider; // Value injected by FXMLLoader

    @FXML // fx:id="curIntensity"
    private Label curIntensity; // Value injected by FXMLLoader

    @FXML // fx:id="flagsLabel"
    private Label flagsLabel; // Value injected by FXMLLoader

    @FXML // fx:id="curFlags"
    private TextField flagsTF; // Value injected by FXMLLoader

    @Inject
    public NotePropertiesController(Localizer localizer, VoicebankContainer voicebankContainer) {
        this.localizer = localizer;
        this.voicebankContainer = voicebankContainer;
    }

    public void initialize() {
        // Set up localization.
        localizer.localize(this);

        // Setup consonant velocity slider.
        consonantVelocitySlider.valueProperty().addListener((event) -> {
            double sliderValue = consonantVelocitySlider.getValue();
            curConsonantVelocity.setText(RoundUtils.roundDecimal(sliderValue, "#.#"));
        });

        // Setup preutter slider.
        preutterSlider.valueProperty().addListener((event) -> {
            double sliderValue = preutterSlider.getValue();
            curPreutter.setText(RoundUtils.roundDecimal(sliderValue, "#.#"));
        });

        // Setup overlap slider.
        overlapSlider.valueProperty().addListener((event) -> {
            double sliderValue = overlapSlider.getValue();
            curOverlap.setText(RoundUtils.roundDecimal(sliderValue, "#.#"));
        });

        // Setup startPoint slider.
        startPointSlider.valueProperty().addListener((event) -> {
            double sliderValue = startPointSlider.getValue();
            curStartPoint.setText(RoundUtils.roundDecimal(sliderValue, "#.#"));
        });

        // Setup intensity slider.
        intensitySlider.valueProperty().addListener((event) -> {
            int sliderValue = RoundUtils.round(intensitySlider.getValue());
            intensitySlider.setValue(sliderValue);
            curIntensity.setText(Integer.toString(sliderValue));
        });
    }

    /* Initializes properties panel with a SongEditor with the song to edit. */
    void setNotes(SongContainer songContainer, RegionBounds selectedRegion) {
        this.songContainer = songContainer;
        NoteIterator notes = songContainer.get().getNoteIterator(selectedRegion);
        int startIndex = notes.getCurIndex(); // Index of first note.
        while (notes.hasNext()) {
            Note note = notes.next();
            // Populate values.
            // consonantVelocitySlider.setValue(note.getConsonantVelocity());
            preutterSlider.setValue(note.getPreutter());
            overlapSlider.setValue(note.getOverlap());
            startPointSlider.setValue(note.getStartPoint());
            intensitySlider.setValue(note.getIntensity());
            flagsTF.setText(note.getNoteFlags());
        }
        int endIndex = notes.getCurIndex() - 1; // Index of last note.

        // Set title.
        titleLabel.setText(
                String.format(
                        "Notes %d to %d of %d",
                        startIndex,
                        endIndex,
                        songContainer.get().getNumNotes()));
    }

    @Override
    public void localize(ResourceBundle bundle) {
        consonantVelocityLabel.setText(bundle.getString("properties.projectName"));
        preutterLabel.setText(bundle.getString("properties.outputFile"));
        overlapLabel.setText(bundle.getString("properties.flags"));
        startPointLabel.setText(bundle.getString("properties.resampler"));
        intensityLabel.setText(bundle.getString("properties.voicebank"));
        flagsLabel.setText(bundle.getString("properties.tempo"));
    }

    @FXML
    void applyProperties(ActionEvent event) {
        Stage currentStage = (Stage) root.getScene().getWindow();
        currentStage.close();
    }

    @FXML
    void restoreDefaults(ActionEvent event) {
        consonantVelocitySlider.setValue(100);
        preutterSlider.setValue(0);
        overlapSlider.setValue(0);
        startPointSlider.setValue(0);
        intensitySlider.setValue(0);
        flagsTF.clear();
    }

    @FXML
    void closeProperties(ActionEvent event) {
        Stage currentStage = (Stage) root.getScene().getWindow();
        currentStage.close();
    }
}
