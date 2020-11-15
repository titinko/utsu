package com.utsusynth.utsu.view.config;

import com.utsusynth.utsu.common.i18n.Localizable;
import com.utsusynth.utsu.common.i18n.Localizer;
import com.utsusynth.utsu.common.i18n.NativeLocale;
import com.utsusynth.utsu.files.PreferencesManager;
import com.utsusynth.utsu.files.PreferencesManager.AutoscrollMode;
import com.utsusynth.utsu.files.PreferencesManager.AutoscrollCancelMode;
import javafx.collections.FXCollections;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import javax.inject.Inject;
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
        GridPane.setValignment(autoscrollLabel, VPos.TOP);
        ToggleGroup autoscrollGroup = new ToggleGroup();
        VBox autoscrollVBox = new VBox(5);
        autoscrollDisabled = new RadioButton("Disabled");
        autoscrollDisabled.setToggleGroup(autoscrollGroup);
        autoscrollEnabledEnd = new RadioButton("Enabled (End)");
        autoscrollEnabledEnd.setToggleGroup(autoscrollGroup);
        autoscrollEnabledMiddle = new RadioButton("Enabled (Middle)");
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

        languageLabel = new Label("Language");
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
        viewInternal.add(languageLabel, 0, 2);
        viewInternal.add(languageChoiceBox, 1, 2);
        return viewInternal;
    }

    @Override
    public void localize(ResourceBundle bundle) {
        autoscrollLabel.setText("Autoscroll during playback");
        autoscrollDisabled.setText("Disabled");
        autoscrollEnabledEnd.setText("Enabled (End)");
        autoscrollEnabledMiddle.setText("Enabled (Middle)");
        autoscrollCancelLabel.setText("Cancel playback autoscroll");
        autoscrollCancelDisabled.setText("Disabled");
        autoscrollCancelEnabled.setText("Enabled");
        languageLabel.setText("Language");
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
        preferencesManager.setLocale(localizer.getCurrentLocale());
    }

    @Override
    public void revertToPreferences() {
        localizer.setLocale(preferencesManager.getLocale());
    }
}
