package com.utsusynth.utsu.controller.song;

import com.google.inject.Inject;
import com.utsusynth.utsu.common.i18n.Localizable;
import com.utsusynth.utsu.common.i18n.Localizer;
import com.utsusynth.utsu.common.utils.RoundUtils;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.ResourceBundle;

public class BulkEditorController implements Localizable {
    // All available editors.
    public enum BulkEditorType {
        PORTAMENTO,
        VIBRATO,
        ENVELOPE,
    }

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

    private BulkEditorCallback callback;

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
    private AnchorPane portamentoListAnchor;

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
    private AnchorPane vibratoListAnchor;

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
    private AnchorPane envelopeListAnchor;

    @Inject
    public BulkEditorController(Localizer localizer) {
        this.localizer = localizer;
    }

    public void initialize() {
        // Initialize common elements.
        initializeNoteLengthChoiceBox();

        // Initialize portamento elements.
        ToggleGroup risingOrFallingToggle = new ToggleGroup();
        portamentoAllNotes.setToggleGroup(risingOrFallingToggle);
        portamentoRisingNotes.setToggleGroup(risingOrFallingToggle);
        portamentoFallingNotes.setToggleGroup(risingOrFallingToggle);
        portamentoAllNotes.setSelected(true); // Consider saving user's setting.
        // TODO: Initialize visual editor.
        // TODO: Initialize config list.

        // Initialize vibrato elements.
        initializeVibratoField(vibratoLengthTF, 0, 100); // Vibrato length (% of note)
        initializeVibratoField(vibratoAmplitudeTF, 5, 200); // Amplitude (cents)
        initializeVibratoField(vibratoPhaseInTF, 0, 100); // Phase in (% of vibrato)
        initializeVibratoField(vibratoPhaseOutTF, 0, 100); // Phase out (% of vibrato)
        initializeVibratoField(vibratoFrequencyTF, 10, 512); // Cycle length (ms)
        initializeVibratoField(vibratoFreqSlopeTF, -100, 100); // Frequency slope
        initializeVibratoField(vibratoHeightTF, -100, 100); // Pitch shift (cents)
        initializeVibratoField(vibratoPhaseTF, 0, 100); // Phase shift (% of cycle)
        // TODO: Initialize visual editor.
        // TODO: Initialize config list.

        // Initialize envelope elements.
        ToggleGroup silenceToggle = new ToggleGroup();
        envelopeAllNotes.setToggleGroup(silenceToggle);
        envelopeSilenceBefore.setToggleGroup(silenceToggle);
        envelopeSilenceAfter.setToggleGroup(silenceToggle);
        envelopeAllNotes.setSelected(true); // Consider saving user's setting.
        // TOOD: Initialize visual editor.
        // TODO: Initialize config list.

        localizer.localize(this);
    }

    private void initializeNoteLengthChoiceBox() {
        noteLengthChoiceBox.setItems(FXCollections.observableArrayList(
                null,
                FilterType.GREATER_THAN_2ND,
                FilterType.GREATER_THAN_4TH,
                FilterType.GREATER_THAN_8TH));
        noteLengthChoiceBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(FilterType filterType) {
                if (filterType == FilterType.GREATER_THAN_2ND) {
                    return "All notes greater than 1/2";
                } else if (filterType == FilterType.GREATER_THAN_4TH) {
                    return "All notes greater than 1/4";
                } else if (filterType == FilterType.GREATER_THAN_8TH) {
                    return "All notes greater than 1/8";
                }
                return "All notes";
            }

            @Override
            public FilterType fromString(String displayName) {
                return null; // Never used.
            }
        });
    }

    private void initializeVibratoField(TextField textField, int min, int max) {
        textField.focusedProperty().addListener(event -> {
            if (!textField.isFocused()) {
                // Round data.
                try {
                    double value = Double.parseDouble(textField.getText());
                    int adjusted = Math.max(min, Math.min(max, RoundUtils.round(value)));
                    textField.setText(Integer.toString(adjusted));
                } catch (NullPointerException | NumberFormatException e) {
                    // TODO: Reset from visual editor.
                    textField.setText("0");
                }
            }
        });
    }

    @Override
    public void localize(ResourceBundle bundle) {
        cancelButton.setText(bundle.getString("general.cancel"));
    }

    void openEditor(BulkEditorType editorType, BulkEditorCallback callback) {
        this.callback = callback;
        if (editorType.equals(BulkEditorType.PORTAMENTO)) {
            tabPane.getSelectionModel().select(portamentoTab);
        } else if (editorType.equals(BulkEditorType.VIBRATO)) {
            tabPane.getSelectionModel().select(vibratoTab);
        } else if (editorType.equals(BulkEditorType.ENVELOPE)) {
            tabPane.getSelectionModel().select(envelopeTab);
        }
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
        ArrayList<FilterType> filters = new ArrayList<>();
        filters.add(FilterType.HIGHLIGHTED);
        applyToNotes(filters);
        Stage currentStage = (Stage) root.getScene().getWindow();
        currentStage.close();
    }

    @FXML
    public void applyToAllNotes(ActionEvent event) {
        applyToNotes(new ArrayList<>());
        Stage currentStage = (Stage) root.getScene().getWindow();
        currentStage.close();
    }

    private void applyToNotes(ArrayList<FilterType> filters) {
        if (noteLengthChoiceBox.getValue() != null) {
            filters.add(noteLengthChoiceBox.getValue());
        }
        if (tabPane.getSelectionModel().getSelectedItem() == portamentoTab) {
            if (portamentoRisingNotes.isSelected()) {
                filters.add(FilterType.RISING_NOTE);
            } else if (portamentoFallingNotes.isSelected()) {
                filters.add(FilterType.FALLING_NOTE);
            }
            callback.updatePortamento(null, filters);
        } else if (tabPane.getSelectionModel().getSelectedItem() == vibratoTab) {
            callback.updateVibrato(null, filters);
        } else if (tabPane.getSelectionModel().getSelectedItem() == envelopeTab){
            if (envelopeSilenceBefore.isSelected()) {
                filters.add(FilterType.SILENCE_BEFORE);
            } else if (envelopeSilenceAfter.isSelected()) {
                filters.add(FilterType.SILENCE_AFTER);
            }
            callback.updateEnvelope(null, filters);
        }
    }

    @FXML
    public void cancelAndClose(ActionEvent event) {
        Stage currentStage = (Stage) root.getScene().getWindow();
        currentStage.close();
    }
}
