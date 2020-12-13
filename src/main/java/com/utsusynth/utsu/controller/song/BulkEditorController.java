package com.utsusynth.utsu.controller.song;

import com.google.inject.Inject;
import com.utsusynth.utsu.common.i18n.Localizable;
import com.utsusynth.utsu.common.i18n.Localizer;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;

import javafx.event.ActionEvent;
import java.util.ResourceBundle;

public class BulkEditorController implements Localizable {
    // All available filters for applying edits.
    public enum FilterType {
        // Common.
        HIGHLIGHTED,
        // Note length filters.
        GREATER_THAN_8TH,
        GREATER_THAN_4TH,
        GREATER_THAN_2ND,
        // Envelope filters.
        SILENCE_BEFORE,
        SILENCE_AFTER,
        // Portamento filters.
        RISING_NOTE,
        FALLING_NOTE,
    }

    private final Localizer localizer;

    /* Common elements. */
    @FXML
    private BorderPane root;
    @FXML
    private TabPane tabPane;
    @FXML
    private Label noteLengthFilterLabel;
    @FXML
    private ChoiceBox<FilterType> noteLengthChoiceBox;
    @FXML
    private Button applyAllButton;
    @FXML
    private Button applySelectionButton;
    @FXML
    private Button cancelButton;

    /* Portamento elements. */
    @FXML
    private Tab portamentoTab;
    @FXML
    private Label portamentoApplyToLabel;
    @FXML
    private RadioButton portamentoAllNotes;
    @FXML
    private RadioButton portamentoRisingNotes;
    @FXML
    private RadioButton portamentoFallingNotes;
    @FXML
    private AnchorPane portamentoAnchor;
    @FXML
    private ListView<String> portamentoListView;

    /* Vibrato elements. */
    @FXML
    private Tab vibratoTab;
    @FXML
    private Label vibratoLengthLabel;
    @FXML
    private TextField vibratoLengthTF;
    @FXML
    private Label vibratoAmplitudeLabel;
    @FXML
    private TextField vibratoAmplitudeTF;
    @FXML
    private Label vibratoPhaseInLabel;
    @FXML
    private TextField vibratoPhaseInTF;
    @FXML
    private Label vibratoPhaseOutLabel;
    @FXML
    private TextField vibratoPhaseOutTF;
    @FXML
    private Label vibratoFrequencyLabel;
    @FXML
    private TextField vibratoFrequencyTF;
    @FXML
    private Label vibratoFreqSlopeLabel;
    @FXML
    private TextField vibratoFreqSlopeTF;
    @FXML
    private Label vibratoHeightLabel;
    @FXML
    private TextField vibratoHeightTF;
    @FXML
    private Label vibratoPhaseLabel;
    @FXML
    private TextField vibratoPhaseTF;
    @FXML
    private AnchorPane vibratoAnchor;
    @FXML
    private ListView<String> vibratoListView;

    /* Envelope elements. */
    @FXML
    private Tab envelopeTab;
    @FXML
    private Label envelopeApplyToLabel;
    @FXML
    private RadioButton envelopeAllNotes;
    @FXML
    private RadioButton envelopeSilenceBefore;
    @FXML
    private RadioButton envelopeSilenceAfter;
    @FXML
    private AnchorPane envelopeAnchor;
    @FXML
    private ListView<String> envelopeListView;

    @Inject
    public BulkEditorController(Localizer localizer) {
        this.localizer = localizer;
    }

    public void initialize() {
        // Initialize common elements.
        noteLengthChoiceBox.setItems(FXCollections.observableArrayList(
                null,
                FilterType.GREATER_THAN_2ND,
                FilterType.GREATER_THAN_4TH,
                FilterType.GREATER_THAN_8TH));

        // Initialize portamento elements.

        // Initialize vibrato elements.

        // Initialize envelope elements.

        localizer.localize(this);
    }

    @Override
    public void localize(ResourceBundle bundle) {
        cancelButton.setText(bundle.getString("general.cancel"));
    }

    @FXML
    public void addPortamentoConfig(ActionEvent event) {
        // TODO
    }

    @FXML
    public void addVibratoConfig(ActionEvent event) {
        // TODO
    }

    @FXML
    public void addEnvelopeConfig(ActionEvent event) {
        // TODO
    }

    @FXML
    public void applyToSelection(ActionEvent event) {
        // TODO, should depend on active tab.
    }

    @FXML
    public void applyToAllNotes(ActionEvent event) {
        // TODO, should depend on active tab.
    }

    @FXML
    public void cancelAndClose(ActionEvent event) {
        // TODO, should depend on active tab.
    }
}
