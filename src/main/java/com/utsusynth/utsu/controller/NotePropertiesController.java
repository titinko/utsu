package com.utsusynth.utsu.controller;

import java.util.Optional;
import java.util.ResourceBundle;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.utsusynth.utsu.common.RegionBounds;
import com.utsusynth.utsu.common.data.NoteConfigData;
import com.utsusynth.utsu.common.exception.ErrorLogger;
import com.utsusynth.utsu.common.i18n.Localizable;
import com.utsusynth.utsu.common.i18n.Localizer;
import com.utsusynth.utsu.common.utils.RoundUtils;
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

/**
 * 'NotePropertiesScene.fxml' Controller Class
 */
public class NotePropertiesController implements Localizable {
    private static final ErrorLogger errorLogger = ErrorLogger.getLogger();

    private final Localizer localizer;

    private ImmutableList<Note> notes;
    private Voicebank voicebank;
    private NotePropertiesCallback callback;

    @FXML // fx:id="root"
    private BorderPane root; // Value injected by FXMLLoader

    @FXML // fx:id="titleLabel"
    private Label titleLabel; // Value injected by FXMLLoader

    @FXML // fx:id="consonantVelocityLabel"
    private Label consonantVelocityLabel; // Value injected by FXMLLoader

    @FXML // fx:id="consonantVelocitySlider"
    private Slider consonantVelocitySlider; // Value injected by FXMLLoader

    @FXML // fx:id="curConsonantVelocity"
    private TextField curConsonantVelocity; // Value injected by FXMLLoader

    @FXML // fx:id="preutterLabel"
    private Label preutterLabel; // Value injected by FXMLLoader

    @FXML // fx:id="preutterSlider"
    private Slider preutterSlider; // Value injected by FXMLLoader

    @FXML // fx:id="curPreutter"
    private TextField curPreutter; // Value injected by FXMLLoader

    @FXML // fx:id="overlapLabel"
    private Label overlapLabel; // Value injected by FXMLLoader

    @FXML // fx:id="overlapSlider"
    private Slider overlapSlider; // Value injected by FXMLLoader

    @FXML // fx:id="curOverlap"
    private TextField curOverlap; // Value injected by FXMLLoader

    @FXML // fx:id="startPointLabel"
    private Label startPointLabel;

    @FXML // fx:id="startPointSlider"
    private Slider startPointSlider; // Value injected by FXMLLoader

    @FXML // fx:id="curStartPoint"
    private TextField curStartPoint; // Value injected by FXMLLoader

    @FXML // fx:id="intensityLabel"
    private Label intensityLabel; // Value injected by FXMLLoader

    @FXML // fx:id="intensitySlider"
    private Slider intensitySlider; // Value injected by FXMLLoader

    @FXML // fx:id="curIntensity"
    private TextField curIntensity; // Value injected by FXMLLoader

    @FXML // fx:id="modulationLabel"
    private Label modulationLabel; // Value injected by FXMLLoader

    @FXML // fx:id="modulationSlider"
    private Slider modulationSlider; // Value injected by FXMLLoader

    @FXML // fx:id ="curModulation"
    private TextField curModulation; // Value injected by FXMLLoader

    @FXML // fx:id="flagsLabel"
    private Label flagsLabel; // Value injected by FXMLLoader

    @FXML // fx:id="flagsTF"
    private TextField flagsTF; // Value injected by FXMLLoader

    @FXML // fx:id="resetButton"
    private Button resetButton; // Value injected by FXMLLoader

    @FXML // fx:id="applyButton"
    private Button applyButton; // Value injected by FXMLLoader

    @FXML // fx:id="cancelButton"
    private Button cancelButton; // Value injected by FXMLLoader

    @Inject
    public NotePropertiesController(Localizer localizer) {
        this.localizer = localizer;
        notes = ImmutableList.of();
    }

    public void initialize() {
        // Set up localization.
        localizer.localize(this);

        // Setup sliders that allow decimal points.
        initializeDoubleSlider(consonantVelocitySlider, curConsonantVelocity);
        initializeDoubleSlider(preutterSlider, curPreutter);
        initializeDoubleSlider(overlapSlider, curOverlap);
        initializeDoubleSlider(startPointSlider, curStartPoint);

        // Setup sliders that don't allow decimal points.
        initializeIntSlider(intensitySlider, curIntensity);
        initializeIntSlider(modulationSlider, curModulation);

        flagsTF.setOnMouseClicked(
                event -> flagsTF.setStyle("-fx-control-inner-background: white;"));
    }

    void initializeDoubleSlider(Slider slider, TextField textField) {
        slider.valueProperty().addListener((event) -> {
            double sliderValue = slider.getValue();
            textField.setText(RoundUtils.roundDecimal(sliderValue, "#.#"));
        });
        textField.focusedProperty().addListener((event) -> {
            if (!textField.isFocused() && !textField.getText().equals("n/a")) {
                try {
                    double boundedValue = Math.max(
                            Math.min(slider.getMax(), Double.parseDouble(textField.getText())),
                            slider.getMin());
                    slider.setValue(boundedValue);
                } catch (NullPointerException | NumberFormatException e) {
                    textField.setText(RoundUtils.roundDecimal(slider.getValue(), "#.#"));
                }
            }
        });
    }

    void initializeIntSlider(Slider slider, TextField textField) {
        // Setup modulation slider.
        slider.valueProperty().addListener((event) -> {
            int sliderValue = RoundUtils.round(slider.getValue());
            slider.setValue(sliderValue);
            textField.setText(Integer.toString(sliderValue));
        });
        textField.focusedProperty().addListener((event) -> {
            if (!textField.isFocused() && !textField.getText().equals("n/a")) {
                try {
                    double boundedValue = Math.max(
                            Math.min(slider.getMax(), Double.parseDouble(textField.getText())),
                            slider.getMin());
                    slider.setValue(boundedValue);
                } catch (NullPointerException | NumberFormatException e) {
                    textField.setText(Integer.toString(RoundUtils.round(slider.getValue())));
                }
            }
        });
    }

    /* Initializes properties panel with a SongEditor with the song to edit. */
    void setData(
            SongContainer songContainer,
            RegionBounds selectedRegion,
            NotePropertiesCallback callback) {
        this.voicebank = songContainer.get().getVoicebank();
        this.callback = callback;

        ImmutableList.Builder<Note> noteBuilder = ImmutableList.builder();
        NoteIterator iterator = songContainer.get().getNoteIterator(selectedRegion);
        int startIndex = iterator.getCurIndex(); // Index of first note.
        while (iterator.hasNext()) {
            noteBuilder.add(iterator.next());
        }
        int endIndex = iterator.getCurIndex() - 1; // Index of last note.
        this.notes = noteBuilder.build();
        if (notes.isEmpty()) {
            // TODO: Handle this better.
            errorLogger.logError(
                    new IllegalArgumentException(
                            "Called note properties editor on an empty region of notes."));
            return;
        }

        // Set title.
        if (notes.size() == 1) {
            titleLabel.setText(
                    String.format(
                            "Note %d of %d (%s)",
                            startIndex + 1,
                            songContainer.get().getNumNotes(),
                            notes.get(0).getLyric()));
        } else {
            titleLabel.setText(
                    String.format(
                            "Notes %d to %d of %d",
                            startIndex + 1,
                            endIndex + 1,
                            songContainer.get().getNumNotes()));
        }

        // Set values.
        Note note1 = notes.get(0);
        if (allValuesEqual(note -> note.getVelocity())) {
            consonantVelocitySlider.setValue(note1.getVelocity());
        } else {
            curConsonantVelocity.setText("n/a");
        }
        if (notes.size() == 1) {
            // Preutter and overlap.
            double preutter = note1.getPreutter().isPresent() ? note1.getPreutter().get()
                    : lyricPreutter(note1);
            preutterSlider.setMin(preutter - 100);
            preutterSlider.setMax(preutter + 100);
            preutterSlider.setValue(preutter);
            double overlap =
                    note1.getOverlap().isPresent() ? note1.getOverlap().get() : lyricOverlap(note1);
            overlapSlider.setMin(overlap - 100);
            overlapSlider.setMax(overlap + 100);
            overlapSlider.setValue(overlap);
        } else {
            preutterSlider.setDisable(true);
            curPreutter.setDisable(true);
            curPreutter.setText("");
            overlapSlider.setDisable(true);
            curOverlap.setDisable(true);
            curOverlap.setText("");
        }
        if (allValuesEqual(note -> note.getStartPoint())) {
            startPointSlider.setValue(note1.getStartPoint());
        } else {
            curStartPoint.setText("n/a");
        }
        if (allValuesEqual(note -> (double) note.getIntensity())) {
            intensitySlider.setValue(note1.getIntensity());
        } else {
            curIntensity.setText("n/a");
        }
        if (allValuesEqual(note -> (double) note.getModulation())) {
            modulationSlider.setValue(note1.getModulation());
        } else {
            curModulation.setText("n/a");
        }
        if (allFlagsEqual()) {
            flagsTF.setText(note1.getNoteFlags());
        } else {
            flagsTF.setStyle("-fx-control-inner-background: lightgray;");
        }
    }

    private double lyricPreutter(Note note) {
        // Finds default preutterance from a note's lyric config.
        Optional<LyricConfig> config = voicebank.getLyricConfig(note.getTrueLyric());
        if (config.isPresent()) {
            return config.get().getPreutterance();
        } else {
            return 0;
        }
    }

    private double lyricOverlap(Note note) {
        // Finds default overlap from a note's lyric config.
        Optional<LyricConfig> config = voicebank.getLyricConfig(note.getTrueLyric());
        if (config.isPresent()) {
            return config.get().getOverlap();
        } else {
            return 0;
        }
    }

    private boolean allValuesEqual(Function<Note, Double> fxn) {
        if (notes.isEmpty()) {
            return false;
        }
        double firstValue = fxn.apply(notes.get(0));
        for (Note note : notes) {
            if (!closeEnough(firstValue, fxn.apply(note))) {
                return false;
            }
        }
        return true;
    }

    private boolean allFlagsEqual() {
        if (notes.isEmpty()) {
            return false;
        }
        String firstFlags = notes.get(0).getNoteFlags();
        for (Note note : notes) {
            if (!firstFlags.equals(note.getNoteFlags())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void localize(ResourceBundle bundle) {
        consonantVelocityLabel.setText(bundle.getString("properties.consonantVelocity"));
        preutterLabel.setText(bundle.getString("properties.preutterance"));
        overlapLabel.setText(bundle.getString("properties.overlap"));
        startPointLabel.setText(bundle.getString("properties.startPoint"));
        intensityLabel.setText(bundle.getString("properties.intensity"));
        modulationLabel.setText(bundle.getString("properties.modulation"));
        flagsLabel.setText(bundle.getString("properties.flags"));
        resetButton.setText(bundle.getString("general.reset"));
        applyButton.setText(bundle.getString("general.apply"));
        cancelButton.setText(bundle.getString("general.cancel"));
    }

    @FXML
    void applyProperties(ActionEvent event) {
        ImmutableList.Builder<NoteConfigData> oldData =
                ImmutableList.builderWithExpectedSize(notes.size());
        ImmutableList.Builder<NoteConfigData> newData =
                ImmutableList.builderWithExpectedSize(notes.size());
        for (Note note : notes) {
            double oldVelocity = note.getVelocity();
            double newVelocity = oldVelocity;
            if (!consonantVelocityLabel.getText().equals("n/a")) {
                newVelocity = consonantVelocitySlider.getValue();
            }
            Optional<Double> oldPreutter = note.getPreutter();
            Optional<Double> newPreutter = oldPreutter;
            if (!preutterSlider.isDisabled() && notes.size() == 1) {
                if (closeEnough(lyricPreutter(note), preutterSlider.getValue())) {
                    newPreutter = Optional.empty();
                } else {
                    newPreutter = Optional.of(preutterSlider.getValue());
                }
            }
            Optional<Double> oldOverlap = note.getOverlap();
            Optional<Double> newOverlap = oldOverlap;
            if (!overlapSlider.isDisabled() && notes.size() == 1) {
                if (closeEnough(lyricOverlap(note), overlapSlider.getValue())) {
                    newOverlap = Optional.empty();
                } else {
                    newOverlap = Optional.of(overlapSlider.getValue());
                }
            }
            double oldStartPoint = note.getStartPoint();
            double newStartPoint = oldStartPoint;
            if (!startPointLabel.getText().equals("n/a")) {
                newStartPoint = startPointSlider.getValue();
            }
            int oldIntensity = note.getIntensity();
            int newIntensity = oldIntensity;
            if (!intensityLabel.getText().equals("n/a")) {
                newIntensity = RoundUtils.round(intensitySlider.getValue());
            }
            int oldModulation = note.getModulation();
            int newModulation = oldModulation;
            if (!modulationLabel.getText().equals("n/a")) {
                newModulation = RoundUtils.round(modulationSlider.getValue());
            }
            String oldFlags = note.getNoteFlags();
            String newFlags = oldFlags;
            if (!flagsTF.getStyle().equals("-fx-control-inner-background: lightgray;")) {
                newFlags = flagsTF.getText();
            }
            oldData.add(
                    new NoteConfigData(
                            oldPreutter,
                            oldOverlap,
                            oldVelocity,
                            oldStartPoint,
                            oldIntensity,
                            oldModulation,
                            oldFlags));
            NoteConfigData newNoteData = new NoteConfigData(
                    newPreutter,
                    newOverlap,
                    newVelocity,
                    newStartPoint,
                    newIntensity,
                    newModulation,
                    newFlags);
            newData.add(newNoteData);
            note.setConfigData(newNoteData);
        }
        callback.updateNotes(oldData.build(), newData.build());
        Stage currentStage = (Stage) root.getScene().getWindow();
        currentStage.close();
    }

    @FXML
    void restoreDefaults(ActionEvent event) {
        consonantVelocitySlider.setValue(100);
        curConsonantVelocity.setText("100.0");
        if (notes.size() == 1) {
            preutterSlider.setValue(lyricPreutter(notes.get(0)));
            curPreutter.setText(Double.toString(lyricPreutter(notes.get(0))));
            overlapSlider.setValue(lyricOverlap(notes.get(0)));
            curOverlap.setText(Double.toString(lyricOverlap(notes.get(0))));
        }
        startPointSlider.setValue(0);
        curStartPoint.setText("0.0");
        intensitySlider.setValue(100);
        curIntensity.setText("100");
        modulationSlider.setValue(0);
        curModulation.setText("0");
        flagsTF.setText("");
    }

    @FXML
    void closeProperties(ActionEvent event) {
        Stage currentStage = (Stage) root.getScene().getWindow();
        currentStage.close();
    }

    private static boolean closeEnough(double value1, double value2) {
        return RoundUtils.roundDecimal(value1, "#.#")
                .equals(RoundUtils.roundDecimal(value2, "#.#"));
    }
}
