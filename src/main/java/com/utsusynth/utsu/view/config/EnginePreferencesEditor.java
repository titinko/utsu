package com.utsusynth.utsu.view.config;

import com.google.inject.Inject;
import com.utsusynth.utsu.common.i18n.Localizable;
import com.utsusynth.utsu.common.i18n.Localizer;
import com.utsusynth.utsu.files.PreferencesManager;
import com.utsusynth.utsu.model.voicebank.VoicebankContainer;
import javafx.application.Platform;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.ResourceBundle;

public class EnginePreferencesEditor extends PreferencesEditor implements Localizable {
    private final PreferencesManager preferencesManager;
    private final VoicebankContainer voicebankContainer;
    private final Localizer localizer;

    private String displayName = "Engine";
    private BorderPane view;
    private Label cacheLabel;
    private RadioButton cacheDisabled;
    private RadioButton cacheEnabled;
    private Label defaultResamplerLabel;
    private File currentResampler;
    private Button changeResamplerButton;
    private Button resetResamplerButton;
    private Label defaultWavtoolLabel;
    private String currentWavtool;
    private Button changeWavtoolButton;
    private Button resetWavtoolButton;
    private Label defaultVoicebankLabel;
    private Button changeVoicebankButton;
    private Button resetVoicebankButton;

    @Inject
    public EnginePreferencesEditor(
            PreferencesManager preferencesManager,
            VoicebankContainer voicebankContainer,
            Localizer localizer) {
        this.preferencesManager = preferencesManager;
        this.voicebankContainer = voicebankContainer;
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
        cacheLabel = new Label("Cache render output");
        cacheLabel.setWrapText(true);
        cacheLabel.setMaxWidth(170);
        GridPane.setValignment(cacheLabel, VPos.TOP);
        ToggleGroup cacheGroup = new ToggleGroup();
        VBox cacheVBox = new VBox(5);
        cacheDisabled = new RadioButton("Disabled");
        cacheDisabled.setToggleGroup(cacheGroup);
        cacheEnabled = new RadioButton("Enabled");
        cacheEnabled.setToggleGroup(cacheGroup);
        cacheVBox.getChildren().addAll(cacheDisabled, cacheEnabled);
        switch (preferencesManager.getCache()) {
            case DISABLED:
                cacheDisabled.setSelected(true);
                break;
            case ENABLED:
                cacheEnabled.setSelected(true);
        }

        defaultResamplerLabel = new Label("Default resampler");
        defaultResamplerLabel.setWrapText(true);
        defaultResamplerLabel.setMaxWidth(170);
        GridPane.setValignment(defaultResamplerLabel, VPos.TOP);
        VBox resamplerVBox = new VBox(5);
        TextField resamplerName = new TextField();
        resamplerName.setEditable(false);
        currentResampler = preferencesManager.getResampler();
        resamplerName.setText(currentResampler.getName());
        HBox resamplerHBox = new HBox(5);
        changeResamplerButton = new Button("Change...");
        changeResamplerButton.setOnAction(event -> {
            File newResampler = selectExecutable();
            if (newResampler != null && newResampler.canExecute()) {
                currentResampler = newResampler;
                resamplerName.setText(currentResampler.getName());
            }
        });
        resetResamplerButton = new Button("Reset");
        resetResamplerButton.setOnAction(event -> {
            currentResampler = preferencesManager.getResamplerDefault();
            resamplerName.setText(currentResampler.getName());
        });
        resamplerHBox.getChildren().addAll(changeResamplerButton, resetResamplerButton);
        resamplerVBox.getChildren().addAll(resamplerName, resamplerHBox);

        defaultWavtoolLabel = new Label("Default wavtool");
        defaultWavtoolLabel.setWrapText(true);
        defaultWavtoolLabel.setMaxWidth(170);
        GridPane.setValignment(defaultWavtoolLabel, VPos.TOP);
        VBox wavtoolVBox = new VBox(5);
        TextField wavtoolName = new TextField();
        wavtoolName.setEditable(false);
        currentWavtool = preferencesManager.getWavtool();
        wavtoolName.setText(guessFileName(currentWavtool));
        HBox wavtoolHBox = new HBox(5);
        changeWavtoolButton = new Button("Change...");
        changeWavtoolButton.setOnAction(event -> {
            File newWavtoolFile = selectExecutable();
            if (newWavtoolFile != null && newWavtoolFile.canExecute()) {
                currentWavtool = newWavtoolFile.getAbsolutePath();
                wavtoolName.setText(newWavtoolFile.getName());
            }
        });
        resetWavtoolButton = new Button("Reset");
        resetWavtoolButton.setOnAction(event -> {
            currentWavtool = preferencesManager.getWavtoolDefault();
            wavtoolName.setText(guessFileName(currentWavtool));
        });
        wavtoolHBox.getChildren().addAll(changeWavtoolButton, resetWavtoolButton);
        wavtoolVBox.getChildren().addAll(wavtoolName, wavtoolHBox);

        defaultVoicebankLabel = new Label("Default voicebank");
        defaultVoicebankLabel.setWrapText(true);
        defaultVoicebankLabel.setMaxWidth(170);
        GridPane.setValignment(defaultVoicebankLabel, VPos.TOP);
        VBox voicebankVBox = new VBox(5);
        TextField voicebankName = new TextField();
        voicebankName.setEditable(false);
        voicebankContainer.setVoicebankForRead(preferencesManager.getVoicebank());
        setVoicebankName(voicebankName);
        voicebankName.setText(voicebankContainer.get().getName());
        HBox voicebankHBox = new HBox(5);
        changeVoicebankButton = new Button("Change...");
        changeVoicebankButton.setOnAction(event -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Select voicebank");
            File file = dc.showDialog(null);
            if (file != null) {
                voicebankContainer.setVoicebankForRead(file);
                setVoicebankName(voicebankName);
            }
        });
        resetVoicebankButton = new Button("Reset");
        resetVoicebankButton.setOnAction(event -> {
            voicebankContainer.setVoicebankForRead(preferencesManager.getVoicebankDefault());
            setVoicebankName(voicebankName);
        });
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
        cacheLabel.setText(bundle.getString("preferences.engine.cacheRenderOutput"));
        cacheDisabled.setText(bundle.getString("preferences.disabled"));
        cacheEnabled.setText(bundle.getString("preferences.enabled"));
        defaultResamplerLabel.setText(bundle.getString("preferences.engine.defaultResampler"));
        changeResamplerButton.setText(bundle.getString("properties.change"));
        resetResamplerButton.setText(bundle.getString("general.reset"));
        defaultWavtoolLabel.setText(bundle.getString("preferences.engine.defaultWavtool"));
        changeWavtoolButton.setText(bundle.getString("properties.change"));
        resetWavtoolButton.setText(bundle.getString("general.reset"));
        defaultVoicebankLabel.setText(bundle.getString("preferences.engine.defaultVoicebank"));
        changeVoicebankButton.setText(bundle.getString("properties.change"));
        resetVoicebankButton.setText(bundle.getString("general.reset"));
    }

    @Override
    public boolean onCloseEditor(Stage stage) {
        return true;
    }

    @Override
    public void savePreferences() {
        if (cacheDisabled.isSelected()) {
            preferencesManager.setCache(PreferencesManager.CacheMode.DISABLED);
        } else if (cacheEnabled.isSelected()) {
            preferencesManager.setCache(PreferencesManager.CacheMode.ENABLED);
        }
        preferencesManager.setResampler(currentResampler);
        preferencesManager.setWavtool(currentWavtool);
        preferencesManager.setVoicebank(voicebankContainer.getLocation());
    }

    @Override
    public void revertToPreferences() {
        // No action needed.
    }

    private File selectExecutable() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select executable file");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Executables", "*", "*.exe"),
                new FileChooser.ExtensionFilter("OSX Executables", "*.out", "*.app"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));
        return fc.showOpenDialog(null);
    }

    // If string is a file path, give it a more human-readable name.
    private static String guessFileName(String filePath) {
        File file = new File(filePath);
        if (file.exists() && file.canExecute()) {
            return file.getName();
        }
        return filePath;
    }

    /* Loads voicebank in a new thread in case it takes a while. */
    private void setVoicebankName(TextField voicebankName) {
        new Thread(() -> {
            String name = voicebankContainer.get().getName();
            Platform.runLater(() -> voicebankName.setText(name));
        }).start();
    }
}
