package com.utsusynth.utsu.view.config;

import com.google.inject.Inject;
import com.utsusynth.utsu.common.i18n.Localizable;
import com.utsusynth.utsu.common.i18n.Localizer;
import com.utsusynth.utsu.common.i18n.NativeLocale;
import com.utsusynth.utsu.files.PreferencesManager;
import com.utsusynth.utsu.files.PreferencesManager.AutoscrollMode;
import com.utsusynth.utsu.files.PreferencesManager.AutoscrollCancelMode;
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
    private Label autoscrollLabel;
    private RadioButton autoscrollDisabled;
    private RadioButton autoscrollEnabledEnd;
    private RadioButton autoscrollEnabledMiddle;
    private Label autoscrollCancelLabel;
    private RadioButton autoscrollCancelDisabled;
    private RadioButton autoscrollCancelEnabled;
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
        autoscrollLabel = new Label("Autoscroll during playback");
        autoscrollLabel.setWrapText(true);
        autoscrollLabel.setMaxWidth(170);
        GridPane.setValignment(autoscrollLabel, VPos.TOP);
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

        autoscrollCancelLabel = new Label("Cancel playback autoscroll");
        autoscrollCancelLabel.setWrapText(true);
        autoscrollCancelLabel.setMaxWidth(170);
        GridPane.setValignment(autoscrollCancelLabel, VPos.TOP);
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

        voicebankImageLabel = new Label("Show voicebank image");
        voicebankImageLabel.setWrapText(true);
        voicebankImageLabel.setMaxWidth(170);
        GridPane.setValignment(voicebankImageLabel, VPos.TOP);
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

        languageLabel = new Label("Language");
        languageLabel.setWrapText(true);
        languageLabel.setMaxWidth(170);
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
        viewInternal.add(voicebankImageLabel, 0, 2);
        viewInternal.add(voicebankImageVBox, 1, 2);
        viewInternal.add(languageLabel, 0, 3);
        viewInternal.add(languageChoiceBox, 1, 3);

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
}
