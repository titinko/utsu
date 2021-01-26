package com.utsusynth.utsu.controller.song;

import com.google.inject.Inject;
import com.utsusynth.utsu.common.RegionBounds;
import com.utsusynth.utsu.common.data.EnvelopeData;
import com.utsusynth.utsu.common.data.PitchbendData;
import com.utsusynth.utsu.common.enums.FilterType;
import com.utsusynth.utsu.common.i18n.Localizable;
import com.utsusynth.utsu.common.i18n.Localizer;
import com.utsusynth.utsu.common.utils.RoundUtils;
import com.utsusynth.utsu.files.BulkEditorConfigManager;
import com.utsusynth.utsu.view.song.BulkEditor;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
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

    private final BulkEditorConfigManager configManager;
    private final BulkEditor view;
    private final Localizer localizer;

    private BulkEditorCallback callback;
    private RegionBounds highlightedRegion;
    private DoubleProperty editorWidth;
    private DoubleProperty editorHeight;
    private DoubleProperty listHeight;

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
    private VBox portamentoVBox;
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
    private VBox vibratoVBox;
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
    private VBox envelopeVBox;
    @FXML
    private AnchorPane envelopeListAnchor;

    @Inject
    public BulkEditorController(
            BulkEditorConfigManager configManager, BulkEditor view, Localizer localizer) {
        this.configManager = configManager;
        this.view = view;
        this.localizer = localizer;
    }

    public void initialize() {
        editorWidth = new SimpleDoubleProperty(300);
        editorHeight = new SimpleDoubleProperty(200);
        listHeight = new SimpleDoubleProperty(240);

        // Initialize common elements.
        initializeNoteLengthChoiceBox();

        // Initialize portamento elements.
        ToggleGroup risingOrFallingToggle = new ToggleGroup();
        portamentoAllNotes.setToggleGroup(risingOrFallingToggle);
        portamentoRisingNotes.setToggleGroup(risingOrFallingToggle);
        portamentoFallingNotes.setToggleGroup(risingOrFallingToggle);
        portamentoAllNotes.setSelected(true); // Consider saving user's setting.
        risingOrFallingToggle.selectedToggleProperty().addListener(
                obs -> view.setCurrentFilters(getFilters()));
        ObservableList<PitchbendData> portamentoConfig = configManager.getPortamentoConfig();
        portamentoVBox.getChildren().add(
                0,
                view.createPortamentoEditor(
                        portamentoConfig.get(0),
                        getFilters(),
                        editorWidth,
                        editorHeight));
        portamentoListAnchor.getChildren().add(
                view.createPortamentoList(
                        portamentoConfig,
                        listHeight));

        // Initialize vibrato elements.
        initializeVibratoField(vibratoLengthTF, 0, 100); // Vibrato length (% of note)
        initializeVibratoField(vibratoAmplitudeTF, 5, 200); // Amplitude (cents)
        initializeVibratoField(vibratoPhaseInTF, 0, 100); // Phase in (% of vibrato)
        initializeVibratoField(vibratoPhaseOutTF, 0, 100); // Phase out (% of vibrato)
        initializeVibratoField(vibratoFrequencyTF, 10, 512); // Cycle length (ms)
        initializeVibratoField(vibratoFreqSlopeTF, -100, 100); // Frequency slope
        initializeVibratoField(vibratoHeightTF, -100, 100); // Pitch shift (cents)
        initializeVibratoField(vibratoPhaseTF, 0, 100); // Phase shift (% of cycle)
        ObservableList<PitchbendData> vibratoConfig = configManager.getVibratoConfig();
        vibratoVBox.getChildren().add(
                0,
                view.createVibratoEditor(
                        vibratoConfig.get(0),
                        editorWidth,
                        editorHeight));
        vibratoListAnchor.getChildren().add(
                view.createVibratoList(
                        vibratoConfig,
                        listHeight));

        // Initialize envelope elements.
        ToggleGroup silenceToggle = new ToggleGroup();
        envelopeAllNotes.setToggleGroup(silenceToggle);
        envelopeSilenceBefore.setToggleGroup(silenceToggle);
        envelopeSilenceAfter.setToggleGroup(silenceToggle);
        envelopeAllNotes.setSelected(true); // Consider saving user's setting.
        ObservableList<EnvelopeData> envelopeConfig = configManager.getEnvelopeConfig();
        envelopeVBox.getChildren().add(
                0,
                view.createEnvelopeEditor(
                        envelopeConfig.get(0),
                        editorWidth,
                        editorHeight));
        envelopeListAnchor.getChildren().add(
                view.createEnvelopeList(
                        envelopeConfig,
                        listHeight));

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
        applySelectionButton.setText(bundle.getString("bulkEditor.applySelection"));
        applyAllButton.setText(bundle.getString("bulkEditor.applyAll"));
        envelopeApplyToLabel.setText(bundle.getString("bulkEditor.applyTo"));
        portamentoApplyToLabel.setText(bundle.getString("bulkEditor.applyTo"));
        cancelButton.setText(bundle.getString("general.cancel"));

        portamentoAllNotes.setText(bundle.getString("bulkEditor.portamento.allNotes"));
        portamentoRisingNotes.setText(bundle.getString("bulkEditor.portamento.risingNotes"));
        portamentoFallingNotes.setText(bundle.getString("bulkEditor.portamento.fallingNotes"));
    }

    void openEditor(
            BulkEditorType editorType,
            RegionBounds region,
            Stage window,
            BulkEditorCallback callback) {
        editorWidth.bind(window.widthProperty().subtract(413));
        editorHeight.bind(window.heightProperty().subtract(186));
        listHeight.bind(window.heightProperty().subtract(146));
        this.callback = callback;
        highlightedRegion = region;
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
        view.saveToPortamentoList();
    }

    @FXML
    public void addVibratoConfig(ActionEvent event) {
        view.saveToVibratoList();
    }

    @FXML
    public void addEnvelopeConfig(ActionEvent event) {
        view.saveToEnvelopeList();
    }

    @FXML
    public void applyToSelection(ActionEvent event) {
        applyToNotes(highlightedRegion);
        Stage currentStage = (Stage) root.getScene().getWindow();
        currentStage.close();
    }

    @FXML
    public void applyToAllNotes(ActionEvent event) {
        applyToNotes(RegionBounds.WHOLE_SONG);
        Stage currentStage = (Stage) root.getScene().getWindow();
        currentStage.close();
    }

    private void applyToNotes(RegionBounds regionToUpdate) {
        ArrayList<FilterType> filters = getFilters();
        if (tabPane.getSelectionModel().getSelectedItem() == portamentoTab) {
            callback.updatePortamento(view.getPortamentoData(), regionToUpdate, filters);
        } else if (tabPane.getSelectionModel().getSelectedItem() == vibratoTab) {
            callback.updateVibrato(view.getVibratoData(), regionToUpdate, filters);
        } else if (tabPane.getSelectionModel().getSelectedItem() == envelopeTab){
            callback.updateEnvelope(view.getEnvelopeData(), regionToUpdate, filters);
        }
    }

    private ArrayList<FilterType> getFilters() {
        ArrayList<FilterType> filters = new ArrayList<>();
        if (noteLengthChoiceBox.getValue() != null) {
            filters.add(noteLengthChoiceBox.getValue());
        }
        if (tabPane.getSelectionModel().getSelectedItem() == portamentoTab) {
            if (portamentoRisingNotes.isSelected()) {
                filters.add(FilterType.RISING_NOTE);
            } else if (portamentoFallingNotes.isSelected()) {
                filters.add(FilterType.FALLING_NOTE);
            }
        } else if (tabPane.getSelectionModel().getSelectedItem() == envelopeTab){
            if (envelopeSilenceBefore.isSelected()) {
                filters.add(FilterType.SILENCE_BEFORE);
            } else if (envelopeSilenceAfter.isSelected()) {
                filters.add(FilterType.SILENCE_AFTER);
            }
        }
        return filters;
    }

    @FXML
    public void cancelAndClose(ActionEvent event) {
        Stage currentStage = (Stage) root.getScene().getWindow();
        currentStage.close();
    }
}
