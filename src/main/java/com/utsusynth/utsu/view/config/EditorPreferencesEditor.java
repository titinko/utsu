package com.utsusynth.utsu.view.config;

import com.google.inject.Inject;
import com.utsusynth.utsu.common.i18n.Localizable;
import com.utsusynth.utsu.common.i18n.Localizer;
import com.utsusynth.utsu.common.i18n.NativeLocale;
import com.utsusynth.utsu.files.PreferencesManager;
import com.utsusynth.utsu.files.PreferencesManager.AutoscrollMode;
import com.utsusynth.utsu.files.PreferencesManager.AutoscrollCancelMode;
import com.utsusynth.utsu.files.PreferencesManager.GuessAliasMode;
import com.utsusynth.utsu.files.PreferencesManager.PlayPianoNotesMode;
import javafx.collections.FXCollections;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.ResourceBundle;

public class EditorPreferencesEditor extends PreferencesEditor implements Localizable {
    private final PreferencesManager preferencesManager;
    private final Localizer localizer;

    private String displayName = "Editor";
    private BorderPane view;
    private Label pianoNotesLabel;
    private RadioButton pianoNotesDisabled;
    private RadioButton pianoNotesEnabledHalf;
    private RadioButton pianoNotesEnabledFull;
    private Label autoscrollLabel;
    private RadioButton autoscrollDisabled;
    private RadioButton autoscrollEnabledEnd;
    private RadioButton autoscrollEnabledMiddle;
    private Label autoscrollCancelLabel;
    private RadioButton autoscrollCancelDisabled;
    private RadioButton autoscrollCancelEnabled;
    private Label guessAliasLabel;
    private RadioButton guessAliasDisabled;
    private RadioButton guessAliasEnabled;
    private Label voicebankImageLabel;
    private CheckBox voicebankFaceCheckBox;
    private CheckBox voicebankBodyCheckBox;
    private Label languageLabel;
    private ChoiceBox<NativeLocale> languageChoiceBox;

    @Inject
    public EditorPreferencesEditor(PreferencesManager preferencesManager, Localizer localizer) {
        this.preferencesManager = preferencesManager;
        this.localizer = localizer;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    protected void setDisplayNameInternal(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public BorderPane getView() {
        return view;
    }

    @Override
    protected void setViewInternal(BorderPane view) {
        this.view = view;
    }

    @Override
    protected Node initializeInternal() {
        autoscrollLabel = createLabel("Autoscroll during playback");
        ToggleGroup autoscrollGroup = new ToggleGroup();
        VBox autoscrollVBox = new VBox(5);
        autoscrollDisabled = new RadioButton("Disabled");
        autoscrollDisabled.setToggleGroup(autoscrollGroup);
        autoscrollEnabledEnd = new RadioButton("Enabled (Standard)");
        autoscrollEnabledEnd.setToggleGroup(autoscrollGroup);
        autoscrollEnabledMiddle = new RadioButton("Enabled (Smooth)");
        autoscrollEnabledMiddle.setToggleGroup(autoscrollGroup);
        autoscrollVBox.getChildren().addAll(
                autoscrollDisabled, autoscrollEnabledEnd, autoscrollEnabledMiddle);
        switch (preferencesManager.getAutoscroll()) {
            case DISABLED:
                autoscrollDisabled.setSelected(true);
                break;
            case ENABLED_END:
                autoscrollEnabledEnd.setSelected(true);
                break;
            case ENABLED_MIDDLE:
                autoscrollEnabledMiddle.setSelected(true);
        }

        autoscrollCancelLabel = createLabel("Cancel playback autoscroll");
        ToggleGroup autoscrollCancelGroup = new ToggleGroup();
        VBox autoscrollCancelVBox = new VBox(5);
        autoscrollCancelDisabled = new RadioButton("Disabled");
        autoscrollCancelDisabled.setToggleGroup(autoscrollCancelGroup);
        autoscrollCancelEnabled = new RadioButton("Enabled");
        autoscrollCancelEnabled.setToggleGroup(autoscrollCancelGroup);
        autoscrollCancelVBox.getChildren().addAll(
                autoscrollCancelDisabled, autoscrollCancelEnabled);
        switch (preferencesManager.getAutoscrollCancel()) {
            case DISABLED:
                autoscrollCancelDisabled.setSelected(true);
                break;
            case ENABLED:
                autoscrollCancelEnabled.setSelected(true);
        }

        pianoNotesLabel = createLabel("Play piano notes");
        ToggleGroup pianoNotesGroup = new ToggleGroup();
        VBox pianoNotesVBox = new VBox(5);
        pianoNotesDisabled = new RadioButton("Disabled");
        pianoNotesDisabled.setToggleGroup(pianoNotesGroup);
        pianoNotesEnabledHalf = new RadioButton("Enabled (Half Volume)");
        pianoNotesEnabledHalf.setToggleGroup(pianoNotesGroup);
        pianoNotesEnabledFull = new RadioButton("Enabled (Full Volume)");
        pianoNotesEnabledFull.setToggleGroup(pianoNotesGroup);
        pianoNotesVBox.getChildren().addAll(
                pianoNotesDisabled, pianoNotesEnabledHalf, pianoNotesEnabledFull);
        switch (preferencesManager.getPlayPianoNotes()) {
            case DISABLED:
                pianoNotesDisabled.setSelected(true);
                break;
            case ENABLED_HALF:
                pianoNotesEnabledHalf.setSelected(true);
                break;
            case ENABLED_FULL:
                pianoNotesEnabledFull.setSelected(true);
        }

        guessAliasLabel = createLabel("Guess alias for lyrics");
        ToggleGroup guessAliasGroup = new ToggleGroup();
        VBox guessAliasVBox = new VBox(5);
        guessAliasDisabled = new RadioButton("Disabled");
        guessAliasDisabled.setToggleGroup(guessAliasGroup);
        guessAliasEnabled = new RadioButton("Enabled");
        guessAliasEnabled.setToggleGroup(guessAliasGroup);
        guessAliasVBox.getChildren().addAll(guessAliasDisabled, guessAliasEnabled);
        switch (preferencesManager.getGuessAlias()) {
            case DISABLED:
                guessAliasDisabled.setSelected(true);
                break;
            case ENABLED:
                guessAliasEnabled.setSelected(true);
        }

        voicebankImageLabel = createLabel("Show voicebank image");
        VBox voicebankImageVBox = new VBox(5);
        voicebankFaceCheckBox = new CheckBox("Face");
        voicebankFaceCheckBox.setSelected(preferencesManager.getShowVoicebankFace().get());
        voicebankFaceCheckBox.setOnAction(action ->
                preferencesManager.getShowVoicebankFace().setValue(
                        voicebankFaceCheckBox.isSelected()));
        voicebankBodyCheckBox = new CheckBox("Full body");
        voicebankBodyCheckBox.setSelected(preferencesManager.getShowVoicebankBody().get());
        voicebankBodyCheckBox.setOnAction(action ->
                preferencesManager.getShowVoicebankBody().setValue(
                        voicebankBodyCheckBox.isSelected()));
        voicebankImageVBox.getChildren().addAll(
                voicebankFaceCheckBox, voicebankBodyCheckBox);

        languageLabel = createLabel("Language");
        languageChoiceBox = new ChoiceBox<>();
        languageChoiceBox.setItems(FXCollections.observableArrayList(localizer.getAllLocales()));
        languageChoiceBox
                .setOnAction((action) -> localizer.setLocale(languageChoiceBox.getValue()));
        languageChoiceBox.setValue(localizer.getCurrentLocale());

        GridPane viewInternal = new GridPane();
        viewInternal.setHgap(10);
        viewInternal.setVgap(10);
        viewInternal.add(autoscrollLabel, 0, 0);
        viewInternal.add(autoscrollVBox, 1, 0);
        viewInternal.add(autoscrollCancelLabel, 0, 1);
        viewInternal.add(autoscrollCancelVBox, 1, 1);
        viewInternal.add(pianoNotesLabel, 0, 2);
        viewInternal.add(pianoNotesVBox, 1, 2);
        viewInternal.add(guessAliasLabel, 0, 3);
        viewInternal.add(guessAliasVBox, 1, 3);
        viewInternal.add(voicebankImageLabel, 0, 4);
        viewInternal.add(voicebankImageVBox, 1, 4);
        viewInternal.add(languageLabel, 0, 5);
        viewInternal.add(languageChoiceBox, 1, 5);

        localizer.localize(this);
        return viewInternal;
    }

    @Override
    public void localize(ResourceBundle bundle) {
        autoscrollLabel.setText(bundle.getString("preferences.editor.autoscroll"));
        autoscrollDisabled.setText(bundle.getString("preferences.disabled"));
        autoscrollEnabledEnd.setText(bundle.getString("preferences.editor.enabledStandard"));
        autoscrollEnabledMiddle.setText(bundle.getString("preferences.editor.enabledSmooth"));
        autoscrollCancelLabel.setText(bundle.getString("preferences.editor.autoscrollCancel"));
        autoscrollCancelDisabled.setText(bundle.getString("preferences.disabled"));
        autoscrollCancelEnabled.setText(bundle.getString("preferences.enabled"));
        pianoNotesLabel.setText(bundle.getString("preferences.editor.playPianoNotes"));
        pianoNotesDisabled.setText(bundle.getString("preferences.disabled"));
        pianoNotesEnabledHalf.setText(bundle.getString("preferences.editor.enabledHalfVolume"));
        pianoNotesEnabledFull.setText(bundle.getString("preferences.editor.enabledFullVolume"));
        guessAliasLabel.setText(bundle.getString("preferences.editor.guessAlias"));
        guessAliasDisabled.setText(bundle.getString("preferences.disabled"));
        guessAliasEnabled.setText(bundle.getString("preferences.enabled"));
        voicebankImageLabel.setText(bundle.getString("preferences.editor.showVoicebankImage"));
        voicebankFaceCheckBox.setText(bundle.getString("preferences.editor.voicebankFace"));
        voicebankBodyCheckBox.setText(bundle.getString("preferences.editor.voicebankFullBody"));
        languageLabel.setText(bundle.getString("preferences.editor.language"));
    }

    @Override
    public boolean onCloseEditor(Stage stage) {
        return true;
    }

    @Override
    public void savePreferences() {
        // Playback autoscroll.
        if (autoscrollDisabled.isSelected()) {
            preferencesManager.setAutoscroll(AutoscrollMode.DISABLED);
        } else if (autoscrollEnabledEnd.isSelected()) {
            preferencesManager.setAutoscroll(AutoscrollMode.ENABLED_END);
        } else if (autoscrollEnabledMiddle.isSelected()) {
            preferencesManager.setAutoscroll(AutoscrollMode.ENABLED_MIDDLE);
        }
        // Whether playback autoscroll is cancellable through manual scrollbar movement.
        if (autoscrollCancelDisabled.isSelected()) {
            preferencesManager.setAutoscrollCancel(AutoscrollCancelMode.DISABLED);
        } else if (autoscrollCancelEnabled.isSelected()) {
            preferencesManager.setAutoscrollCancel(AutoscrollCancelMode.ENABLED);
        }
        // Piano notes that play when moving a single note.
        if (pianoNotesDisabled.isSelected()) {
            preferencesManager.setPlayPianoNotes(PlayPianoNotesMode.DISABLED);
        } else if (pianoNotesEnabledHalf.isSelected()) {
            preferencesManager.setPlayPianoNotes(PlayPianoNotesMode.ENABLED_HALF);
        } else if (pianoNotesEnabledFull.isSelected()) {
            preferencesManager.setPlayPianoNotes(PlayPianoNotesMode.ENABLED_FULL);
        }
        // Whether to try to guess the alias of lyrics by adding prefixes/suffixes, or only allow
        // exact match.
        if (guessAliasDisabled.isSelected()) {
            preferencesManager.setGuessAlias(GuessAliasMode.DISABLED);
        } else if (guessAliasEnabled.isSelected()) {
            preferencesManager.setGuessAlias(GuessAliasMode.ENABLED);
        }
        // Whether to show the voicebank face/body image on the song editor.
        preferencesManager.saveShowVoicebankFace();
        preferencesManager.saveShowVoicebankBody();
        preferencesManager.setLocale(localizer.getCurrentLocale());
    }

    @Override
    public void revertToPreferences() {
        preferencesManager.revertShowVoicebankFace();
        preferencesManager.revertShowVoicebankBody();
        localizer.setLocale(preferencesManager.getLocale());
    }

    private static Label createLabel(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setMaxWidth(170);
        GridPane.setValignment(label, VPos.TOP);
        return label;
    }
}
