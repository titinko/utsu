package com.utsusynth.utsu.view.config;

import com.utsusynth.utsu.common.i18n.Localizable;
import com.utsusynth.utsu.common.i18n.Localizer;
import com.utsusynth.utsu.files.PreferencesManager;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import javax.inject.Inject;
import java.util.ResourceBundle;

public class EnginePreferencesEditor extends PreferencesEditor implements Localizable {
    private final PreferencesManager preferencesManager;
    private final Localizer localizer;

    private String displayName = "Engine";
    private BorderPane view;
    private Label cacheLabel;
    private RadioButton cacheDisabled;
    private RadioButton cacheEnabled;
    private Label defaultResamplerLabel;
    private Button changeResamplerButton;
    private Button resetResamplerButton;
    private Label defaultWavtoolLabel;
    private Button changeWavtoolButton;
    private Button resetWavtoolButton;
    private Label defaultVoicebankLabel;
    private Button changeVoicebankButton;
    private Button resetVoicebankButton;

    @Inject
    public EnginePreferencesEditor(PreferencesManager preferencesManager, Localizer localizer) {
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
        cacheLabel = new Label("Cache resampler output");
        GridPane.setValignment(cacheLabel, VPos.TOP);
        ToggleGroup cacheGroup = new ToggleGroup();
        VBox cacheVBox = new VBox(5);
        cacheDisabled = new RadioButton("Disabled");
        cacheDisabled.setToggleGroup(cacheGroup);
        cacheEnabled = new RadioButton("Enabled");
        cacheEnabled.setToggleGroup(cacheGroup);
        cacheVBox.getChildren().addAll(cacheDisabled, cacheEnabled);

        defaultResamplerLabel = new Label("Default resampler");
        GridPane.setValignment(defaultResamplerLabel, VPos.TOP);
        VBox resamplerVBox = new VBox(5);
        TextField resamplerName = new TextField();
        HBox resamplerHBox = new HBox(5);
        changeResamplerButton = new Button("Change...");
        resetResamplerButton = new Button("Reset");
        resamplerHBox.getChildren().addAll(changeResamplerButton, resetResamplerButton);
        resamplerVBox.getChildren().addAll(resamplerName, resamplerHBox);

        defaultWavtoolLabel = new Label("Default wavtool");
        GridPane.setValignment(defaultWavtoolLabel, VPos.TOP);
        VBox wavtoolVBox = new VBox(5);
        TextField wavtoolName = new TextField();
        HBox wavtoolHBox = new HBox(5);
        changeWavtoolButton = new Button("Change...");
        resetWavtoolButton = new Button("Reset");
        wavtoolHBox.getChildren().addAll(changeWavtoolButton, resetWavtoolButton);
        wavtoolVBox.getChildren().addAll(wavtoolName, wavtoolHBox);

        defaultVoicebankLabel = new Label("Default voicebank");
        GridPane.setValignment(defaultVoicebankLabel, VPos.TOP);
        VBox voicebankVBox = new VBox(5);
        TextField voicebankName = new TextField();
        HBox voicebankHBox = new HBox(5);
        changeVoicebankButton = new Button("Change...");
        resetVoicebankButton = new Button("Reset");
        voicebankHBox.getChildren().addAll(changeVoicebankButton, resetVoicebankButton);
        voicebankVBox.getChildren().addAll(voicebankName, voicebankHBox);

        GridPane viewInternal = new GridPane();
        viewInternal.setHgap(10);
        viewInternal.setVgap(10);
        viewInternal.add(cacheLabel, 0, 0);
        viewInternal.add(cacheVBox, 1, 0);
        viewInternal.add(defaultResamplerLabel, 0, 1);
        viewInternal.add(resamplerVBox, 1, 1);
        viewInternal.add(defaultWavtoolLabel, 0, 2);
        viewInternal.add(wavtoolVBox, 1, 2);
        viewInternal.add(defaultVoicebankLabel, 0, 3);
        viewInternal.add(voicebankVBox, 1, 3);

        localizer.localize(this);
        return viewInternal;
    }

    @Override
    public void localize(ResourceBundle bundle) {
        cacheLabel.setText("Cache resampler output");
        cacheDisabled.setText("Disabled");
        cacheEnabled.setText("Enabled");
        defaultResamplerLabel.setText("Default resampler");
        changeResamplerButton.setText(bundle.getString("properties.change"));
        resetResamplerButton.setText(bundle.getString("general.reset"));
        defaultWavtoolLabel.setText("Default wavtool");
        changeWavtoolButton.setText(bundle.getString("properties.change"));
        resetWavtoolButton.setText(bundle.getString("general.reset"));
        defaultVoicebankLabel.setText("Default voicebank");
        changeVoicebankButton.setText(bundle.getString("properties.change"));
        resetVoicebankButton.setText(bundle.getString("general.reset"));
    }

    @Override
    public boolean onCloseEditor(Stage stage) {
        return true;
    }

    @Override
    public void savePreferences() {
        // TODO
    }

    @Override
    public void revertToPreferences() {
        // No action needed.
    }
}
