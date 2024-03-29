package com.utsusynth.utsu.controller.song;

import com.google.inject.Inject;
import com.utsusynth.utsu.common.utils.RegionBounds;
import com.utsusynth.utsu.common.data.EnvelopeData;
import com.utsusynth.utsu.common.data.PitchbendData;
import com.utsusynth.utsu.common.enums.FilterType;
import com.utsusynth.utsu.common.i18n.Localizable;
import com.utsusynth.utsu.common.i18n.Localizer;
import com.utsusynth.utsu.common.utils.RoundUtils;
import com.utsusynth.utsu.files.BulkEditorConfigManager;
import com.utsusynth.utsu.view.song.BulkEditor;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.prefs.Preferences;
import java.util.ResourceBundle;

public class BulkEditorController implements Localizable {
    // All available editors.
    public enum BulkEditorType {
        PORTAMENTO,
        VIBRATO,
        ENVELOPE,
    }

    private static final double EDITOR_WIDTH = 320;
    private static final double EDITOR_HEIGHT = 220;
    private static final double LIST_HEIGHT = 260;

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
    private Label vibratoCheckboxLabel;
    @FXML
    private CheckBox vibratoCheckbox;
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
        editorWidth = new SimpleDoubleProperty(EDITOR_WIDTH);
        editorHeight = new SimpleDoubleProperty(EDITOR_HEIGHT);
        listHeight = new SimpleDoubleProperty(LIST_HEIGHT);

        // Initialize common elements.
        Preferences bulkEditorPreferences = Preferences.userRoot().node("utsu/bulkEditor");
        FilterType noteLengthChoice = null;
        switch (bulkEditorPreferences.get("filterTo", "allNotes")) {
            case "1/2":
                noteLengthChoice = FilterType.GREATER_THAN_2ND;
                break;
            case "1/4":
                noteLengthChoice = FilterType.GREATER_THAN_4TH;
                break;
            case "1/8":
                noteLengthChoice = FilterType.GREATER_THAN_8TH;
                break;
        }
        initializeNoteLengthChoiceBox(noteLengthChoice);
        noteLengthChoiceBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                bulkEditorPreferences.put("filterTo", "allNotes");
            } else {
                switch (newVal) {
                    case GREATER_THAN_2ND:
                        bulkEditorPreferences.put("filterTo", "1/2");
                        break;
                    case GREATER_THAN_4TH:
                        bulkEditorPreferences.put("filterTo", "1/4");
                        break;
                    case GREATER_THAN_8TH:
                        bulkEditorPreferences.put("filterTo", "1/8");
                        break;
                }
            }
        });

        // Common setup for all bulk editors.
        view.initialize(editorWidth, editorHeight);

        // Initialize portamento elements.
        ToggleGroup risingOrFallingToggle = new ToggleGroup();
        portamentoAllNotes.setToggleGroup(risingOrFallingToggle);
        portamentoRisingNotes.setToggleGroup(risingOrFallingToggle);
        portamentoFallingNotes.setToggleGroup(risingOrFallingToggle);
        Preferences portamentoPrefs = Preferences.userRoot().node("utsu/bulkEditor/portamento");
        String portamentoChoice = portamentoPrefs.get("applyTo", "allNotes");
        if (portamentoChoice.equals("risingNotes")) {
            portamentoRisingNotes.setSelected(true);
        } else if (portamentoChoice.equals("fallingNotes")) {
            portamentoFallingNotes.setSelected(true);
        } else {
            portamentoAllNotes.setSelected(true);
        }
        risingOrFallingToggle.selectedToggleProperty().addListener(
                obs -> {
                    if (portamentoRisingNotes.isSelected()) {
                        portamentoPrefs.put("applyTo", "risingNotes");
                    } else if (portamentoFallingNotes.isSelected()) {
                        portamentoPrefs.put("applyTo", "fallingNotes");
                    } else {
                        portamentoPrefs.put("applyTo", "allNotes");
                    }
                    view.setCurrentFilters(getFilters());
                });
        ObservableList<PitchbendData> portamentoConfig = configManager.getPortamentoConfig();
        portamentoVBox.getChildren().add(
                0, view.createPortamentoEditor(portamentoConfig.get(0), getFilters()));
        portamentoListAnchor.getChildren().add(
                view.createPortamentoList(portamentoConfig, listHeight));

        // Initialize vibrato elements.
        vibratoCheckbox.selectedProperty().addListener(
                obs -> view.toggleVibrato(vibratoCheckbox.isSelected()));
        initializeVibratoField(vibratoLengthTF, 0, 1, 100); // Vibrato length (% of note)
        initializeVibratoField(vibratoAmplitudeTF, 2, 5, 200); // Amplitude (cents)
        initializeVibratoField(vibratoPhaseInTF, 3, 0, 100); // Phase in (% of vibrato)
        initializeVibratoField(vibratoPhaseOutTF, 4, 0, 100); // Phase out (% of vibrato)
        initializeVibratoField(vibratoFrequencyTF, 1, 10, 512); // Cycle length (ms)
        initializeVibratoField(vibratoFreqSlopeTF, 8, -100, 100); // Frequency slope
        initializeVibratoField(vibratoHeightTF, 6, -100, 100); // Pitch shift (cents)
        initializeVibratoField(vibratoPhaseTF, 5, 0, 100); // Phase shift (% of cycle)
        Runnable vibratoCallback = () -> {
            int[] newVibrato = view.getVibratoData().getVibrato();
            vibratoLengthTF.setText(Integer.toString(newVibrato[0]));
            vibratoAmplitudeTF.setText(Integer.toString(newVibrato[2]));
            vibratoPhaseInTF.setText(Integer.toString(newVibrato[3]));
            vibratoPhaseOutTF.setText(Integer.toString(newVibrato[4]));
            vibratoFrequencyTF.setText(Integer.toString(newVibrato[1]));
            vibratoFreqSlopeTF.setText(Integer.toString(newVibrato[8]));
            vibratoHeightTF.setText(Integer.toString(newVibrato[6]));
            vibratoPhaseTF.setText(Integer.toString(newVibrato[5]));
            vibratoCheckbox.setSelected(newVibrato[0] > 0);
        };
        ObservableList<PitchbendData> vibratoConfig = configManager.getVibratoConfig();
        vibratoVBox.getChildren().add(
                0, view.createVibratoEditor(vibratoConfig.get(0), vibratoCallback));
        vibratoListAnchor.getChildren().add(view.createVibratoList(vibratoConfig, listHeight));

        // Initialize envelope elements.
        ToggleGroup silenceToggle = new ToggleGroup();
        envelopeAllNotes.setToggleGroup(silenceToggle);
        envelopeSilenceBefore.setToggleGroup(silenceToggle);
        envelopeSilenceAfter.setToggleGroup(silenceToggle);
        Preferences envelopePrefs = Preferences.userRoot().node("utsu/bulkEditor/envelope");
        String envelopeChoice = envelopePrefs.get("applyTo", "allNotes");
        if (envelopeChoice.equals("silenceBefore")) {
            envelopeSilenceBefore.setSelected(true);
        } else if (envelopeChoice.equals("silenceAfter")) {
            envelopeSilenceAfter.setSelected(true);
        } else {
            envelopeAllNotes.setSelected(true);
        }
        silenceToggle.selectedToggleProperty().addListener(
                obs -> {
                    if (envelopeSilenceBefore.isSelected()) {
                        envelopePrefs.put("applyTo", "silenceBefore");
                    } else if (envelopeSilenceAfter.isSelected()) {
                        envelopePrefs.put("applyTo", "silenceAfter");
                    } else {
                        envelopePrefs.put("applyTo", "allNotes");
                    }
                });
        ObservableList<EnvelopeData> envelopeConfig = configManager.getEnvelopeConfig();
        envelopeVBox.getChildren().add(0, view.createEnvelopeEditor(envelopeConfig.get(0)));
        envelopeListAnchor.getChildren().add(view.createEnvelopeList(envelopeConfig, listHeight));

        localizer.localize(this);
    }

    private void initializeNoteLengthChoiceBox(FilterType initialValue) {
        noteLengthChoiceBox.setItems(FXCollections.observableArrayList(
                null,
                FilterType.GREATER_THAN_2ND,
                FilterType.GREATER_THAN_4TH,
                FilterType.GREATER_THAN_8TH));
        noteLengthChoiceBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(FilterType filterType) {
                if (filterType == FilterType.GREATER_THAN_2ND) {
                    return MessageFormat.format(
                            localizer.getMessage("bulkEditor.allNotesGreaterThan"), "1/2");
                } else if (filterType == FilterType.GREATER_THAN_4TH) {
                    return MessageFormat.format(
                            localizer.getMessage("bulkEditor.allNotesGreaterThan"), "1/4");
                } else if (filterType == FilterType.GREATER_THAN_8TH) {
                    return MessageFormat.format(
                            localizer.getMessage("bulkEditor.allNotesGreaterThan"), "1/8");
                }
                return localizer.getMessage("bulkEditor.allNotes");
            }

            @Override
            public FilterType fromString(String displayName) {
                return null; // Never used.
            }
        });
        noteLengthChoiceBox.setValue(initialValue);
    }

    private void initializeVibratoField(TextField textField, int index, int min, int max) {
        textField.focusedProperty().addListener(event -> {
            if (!textField.isFocused() && textField.isEditable()) {
                // Round data.
                try {
                    double value = Double.parseDouble(textField.getText());
                    int adjusted = Math.max(min, Math.min(max, RoundUtils.round(value)));
                    textField.setText(Integer.toString(adjusted));
                    view.setVibratoValue(index, adjusted);
                } catch (NullPointerException | NumberFormatException e) {
                    int vibratoValue = view.getVibratoData().getVibrato(index);
                    textField.setText(Integer.toString(vibratoValue));
                }
            }
        });
        textField.disableProperty().bind(vibratoCheckbox.selectedProperty().not());
    }

    @Override
    public void localize(ResourceBundle bundle) {
        noteLengthFilterLabel.setText(bundle.getString("bulkEditor.filterTo"));
        initializeNoteLengthChoiceBox(noteLengthChoiceBox.getValue()); // Re-translate options.
        applySelectionButton.setText(bundle.getString("bulkEditor.applySelection"));
        applyAllButton.setText(bundle.getString("bulkEditor.applyAll"));
        cancelButton.setText(bundle.getString("general.cancel"));

        portamentoTab.setText(bundle.getString("menu.tools.bulkEditor.portamento"));
        portamentoApplyToLabel.setText(bundle.getString("bulkEditor.applyTo"));
        portamentoAllNotes.setText(bundle.getString("bulkEditor.allNotes"));
        portamentoRisingNotes.setText(bundle.getString("bulkEditor.portamento.risingNotes"));
        portamentoFallingNotes.setText(bundle.getString("bulkEditor.portamento.fallingNotes"));

        vibratoTab.setText(bundle.getString("song.note.vibrato"));
        vibratoCheckboxLabel.setText(bundle.getString("song.note.vibrato"));
        vibratoLengthLabel.setText(bundle.getString("bulkEditor.vibrato.length"));
        vibratoAmplitudeLabel.setText(bundle.getString("bulkEditor.vibrato.amplitude"));
        vibratoPhaseInLabel.setText(bundle.getString("bulkEditor.vibrato.phaseIn"));
        vibratoPhaseOutLabel.setText(bundle.getString("bulkEditor.vibrato.phaseOut"));
        vibratoFrequencyLabel.setText(bundle.getString("bulkEditor.vibrato.frequency"));
        vibratoFreqSlopeLabel.setText(bundle.getString("bulkEditor.vibrato.freqSlope"));
        vibratoHeightLabel.setText(bundle.getString("bulkEditor.vibrato.height"));
        vibratoPhaseLabel.setText(bundle.getString("bulkEditor.vibrato.phase"));

        envelopeTab.setText(bundle.getString("menu.tools.bulkEditor.envelope"));
        envelopeApplyToLabel.setText(bundle.getString("bulkEditor.applyTo"));
        envelopeAllNotes.setText(bundle.getString("bulkEditor.allNotes"));
        envelopeSilenceBefore.setText(bundle.getString("bulkEditor.envelope.silenceBefore"));
        envelopeSilenceAfter.setText(bundle.getString("bulkEditor.envelope.silenceAfter"));
    }

    /** Secondary initialization. */
    void openEditor(
            BulkEditorType editorType,
            RegionBounds region,
            Stage window,
            BulkEditorCallback callback) {
        this.callback = callback;
        highlightedRegion = region;

        // Open the correct tab.
        if (editorType.equals(BulkEditorType.PORTAMENTO)) {
            tabPane.getSelectionModel().select(portamentoTab);
        } else if (editorType.equals(BulkEditorType.VIBRATO)) {
            tabPane.getSelectionModel().select(vibratoTab);
        } else if (editorType.equals(BulkEditorType.ENVELOPE)) {
            tabPane.getSelectionModel().select(envelopeTab);
        }

        // Bind editor width strictly to window size.
        InvalidationListener widthUpdate = new InvalidationListener() {
            @Override
            public void invalidated(Observable observable) {
                if (window.getWidth() > 0) {
                    double widthDifference = window.getWidth() - EDITOR_WIDTH;
                    editorWidth.bind(Bindings.max(
                            EDITOR_WIDTH, window.widthProperty().subtract(widthDifference)));
                    window.widthProperty().removeListener(this);
                }
            }
        };
        window.widthProperty().addListener(widthUpdate);

        // Bind editor/list heights strictly to window size.
        InvalidationListener heightUpdate = new InvalidationListener() {
            @Override
            public void invalidated(Observable observable) {
                if (window.getHeight() > 0) {
                    double heightDifference = window.getHeight() - EDITOR_HEIGHT;
                    editorHeight.bind(Bindings.max(
                            EDITOR_HEIGHT, window.heightProperty().subtract(heightDifference)));

                    double listHeightDifference = window.getHeight() - LIST_HEIGHT;
                    listHeight.bind(Bindings.max(
                            LIST_HEIGHT, window.heightProperty().subtract(listHeightDifference)));

                    window.heightProperty().removeListener(this);
                }
            }
        };
        window.heightProperty().addListener(heightUpdate);
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
