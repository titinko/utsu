package com.utsusynth.utsu.controller.song;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.utsusynth.utsu.common.i18n.Localizable;
import com.utsusynth.utsu.common.i18n.Localizer;
import com.utsusynth.utsu.common.utils.RoundUtils;
import com.utsusynth.utsu.engine.Engine;
import com.utsusynth.utsu.engine.wavtool.Wavtool;
import com.utsusynth.utsu.engine.wavtool.WavtoolConverter;
import com.utsusynth.utsu.files.PreferencesManager;
import com.utsusynth.utsu.model.song.SongContainer;
import com.utsusynth.utsu.model.voicebank.VoicebankContainer;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Function;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

/**
 * 'SongPropertiesScene.fxml' Controller Class
 */
public class SongPropertiesController implements Localizable {
    private static final int NUM_CACHED_FILES = 5;
    private static final String RESAMPLER_CATEGORY = "recentResamplers";
    private static final String WAVTOOL_CATEGORY = "recentWavtools";
    private static final String VOICEBANK_CATEGORY = "voicebank";

    private final Localizer localizer;
    private final VoicebankContainer voicebankContainer;
    private final PreferencesManager preferencesManager;
    private final WavtoolConverter wavtoolConverter;
    private final Preferences utsuPreferences;

    private SongContainer songContainer;
    private Engine engine;
    private File resamplerPath;
    private Wavtool wavtool;
    // private Optional<File> instrumentalPath;
    private Runnable onSongChange; // Call when applying properties.

    @FXML // fx:id="root"
    private BorderPane root; // Value injected by FXMLLoader

    @FXML // fx:id="projectNameLabel"
    private Label projectNameLabel; // Value injected by FXMLLoader

    @FXML // fx:id="projectNameTF"
    private TextField projectNameTF; // Value injected by FXMLLoader

    @FXML // fx:id="outputFileLabel"
    private Label outputFileLabel; // Value injected by FXMLLoader

    @FXML // fx:id="outputFileTF"
    private TextField outputFileTF; // Value injected by FXMLLoader

    @FXML // fx:id="flagsLabel"
    private Label flagsLabel; // Value injected by FXMLLoader

    @FXML // fx:id="flagsTF"
    private TextField flagsTF; // Value injected by FXMLLoader

    @FXML // fx:id="resamplerLabel"
    private Label resamplerLabel; // Value injected by FXMLLoader

    @FXML // fx:id="resamplerChoiceBox"
    private ChoiceBox<File> resamplerChoiceBox; // Value injected by FXMLLoader

    @FXML // fx:id="importResamplerButton"
    private Button importResamplerButton; // Value injected by FXMLLoader

    @FXML // fx:id="wavtoolLabel"
    private Label wavtoolLabel; // Value injected by FXMLLoader

    @FXML // fx:id="wavtoolChoiceBox"
    private ChoiceBox<Wavtool> wavtoolChoiceBox; // Value injected by FXMLLoader

    @FXML // fx:id="importWavtoolButton"
    private Button importWavtoolButton; // Value injected by FXMLLoader

    @FXML // fx:id="voicebankLabel"
    private Label voicebankLabel; // Value injected by FXMLLoader

    @FXML // fx:id="voicebankChoiceBox"
    private ChoiceBox<File> voicebankChoiceBox; // Value injected by FXMLLoader

    @FXML // fx:id="importVoicebankButton"
    private Button importVoicebankButton; // Value injected by FXMLLoader

    // @FXML // fx:id="instrumentalLabel"
    // private Label instrumentalLabel; // Value injected by FXMLLoader

    // @FXML // fx:id="instrumentalName"
    // private TextField instrumentalName; // Value injected by FXMLLoader

    // @FXML // fx:id="changeInstrumentalButton"
    // private Button changeInstrumentalButton; // Value injected by FXMLLoader

    @FXML // fx:id="tempoLabel"
    private Label tempoLabel; // Value injected by FXMLLoader

    @FXML // fx:id="tempoSlider"
    private Slider tempoSlider; // Value injected by FXMLLoader

    @FXML // fx:id="curTempo"
    private TextField curTempo; // Value injected by FXMLLoader

    @FXML // fx:id="applyButton"
    private Button applyButton; // Value injected by FXMLLoader

    @FXML // fx:id="cancelButton"
    private Button cancelButton; // Value injected by FXMLLoader

    @Inject
    public SongPropertiesController(
            Localizer localizer,
            VoicebankContainer voicebankContainer,
            PreferencesManager preferencesManager,
            WavtoolConverter wavtoolConverter) {
        this.localizer = localizer;
        this.voicebankContainer = voicebankContainer;
        this.preferencesManager = preferencesManager;
        this.wavtoolConverter = wavtoolConverter;
        utsuPreferences = Preferences.userRoot().node("utsu");
    }

    public void initialize() {
        // Set up localization.
        localizer.localize(this);
    }

    /* Initializes properties panel with a SongContainer with the song to edit. */
    void setData(SongContainer songContainer, Engine engine, Runnable updateViewCallback) {
        this.songContainer = songContainer;
        this.engine = engine;
        this.onSongChange = updateViewCallback;

        // Set values to save.
        voicebankContainer.setVoicebankForRead(songContainer.get().getVoiceDir());
        // instrumentalPath = songContainer.get().getInstrumental();

        // Set text boxes.
        projectNameTF.setText(songContainer.get().getProjectName());
        outputFileTF.setText(songContainer.get().getOutputFile().getAbsolutePath());
        flagsTF.setText(songContainer.get().getFlags());
        // instrumentalName.setText(instrumentalPath.orElse(new File("")).getName());

        // Set choice boxes.
        resamplerChoiceBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(File file) {
                return file == null ? "" : file.getName();
            }

            @Override
            public File fromString(String s) {
                return null; // Never used.
            }
        });
        resamplerChoiceBox.setItems(
                FXCollections.observableArrayList(getFileList(RESAMPLER_CATEGORY)));
        resamplerChoiceBox.getItems().add(0, preferencesManager.getResamplerDefault());
        resamplerChoiceBox.setValue(engine.getResamplerPath());
        ArrayList<File> allWavtoolFiles = getFileList(WAVTOOL_CATEGORY);
        ImmutableList<Wavtool> allWavtools = createWavtoolList(allWavtoolFiles);
        wavtoolChoiceBox.setItems(FXCollections.observableArrayList(allWavtools));
        wavtoolChoiceBox.setValue(engine.getWavtool());
        voicebankChoiceBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(File file) {
                return file == null ? "" : file.getName();
            }

            @Override
            public File fromString(String s) {
                return null; // Never used.
            }
        });
        voicebankChoiceBox.setItems(
                FXCollections.observableArrayList(getFileList(VOICEBANK_CATEGORY)));
        voicebankChoiceBox.getItems().add(0, preferencesManager.getVoicebankDefault());
        voicebankChoiceBox.setValue(voicebankContainer.getLocation());
        voicebankChoiceBox.setOnAction(event -> {
            new Thread(() -> {
                // Pre-loads the voicebank.
                voicebankContainer.setVoicebankForRead(voicebankChoiceBox.getValue());
                voicebankContainer.get();
            }).start();
        });

        // Setup tempo slider.
        tempoSlider.valueProperty().addListener((event) -> {
            int sliderValue = RoundUtils.round(tempoSlider.getValue());
            tempoSlider.setValue(sliderValue);
            curTempo.setText(Integer.toString(sliderValue));
        });
        curTempo.focusedProperty().addListener((event) -> {
            if (!curTempo.isFocused()) {
                try {
                    double boundedTempo = Math.max(
                            Math.min(tempoSlider.getMax(), Double.parseDouble(curTempo.getText())),
                            tempoSlider.getMin());
                    tempoSlider.setValue(boundedTempo);
                } catch (NullPointerException | NumberFormatException e) {
                    curTempo.setText(Integer.toString(RoundUtils.round(tempoSlider.getValue())));
                }
            }
        });
        tempoSlider.setValue(songContainer.get().getTempo());
        curTempo.setText(Integer.toString(RoundUtils.round(tempoSlider.getValue())));
    }

    private ArrayList<File> getFileList(String category) {
        ArrayList<File> fileList = new ArrayList<>();
        for (String filename : utsuPreferences.get(category, "").split("\\|")) {
            try {
                File file = new File(filename);
                if (file.exists() && !fileList.contains(file)) {
                    fileList.add(file);
                }
            } catch (Exception e) {
                // Don't throw exception for a non-critical thing like this.
                System.out.println("Warning: Un-parseable " + category + " filename.");
            }
        }
        return fileList;
    }

    private ArrayList<File> updateFileList(String category, File newFile) {
        // Create new list, knocking off the older file(s) if necessary.
        ArrayList<File> fileList = getFileList(category);
        ArrayList<File> newList = new ArrayList<>();
        newList.add(newFile);
        for (int i = 0; i < NUM_CACHED_FILES - 1; i++) {
            if (fileList.size() > i) {
                if (!newList.contains(fileList.get(i))) {
                    newList.add(fileList.get(i));
                }
            }
        }
        // Save list in user preferences.
        StringBuilder stringBuilder = new StringBuilder();
        for (File file : newList) {
            stringBuilder.append(file.getAbsolutePath()).append("|");
        }
        utsuPreferences.put(category, stringBuilder.toString());
        return newList;
    }

    @Override
    public void localize(ResourceBundle bundle) {
        projectNameLabel.setText(bundle.getString("properties.projectName"));
        outputFileLabel.setText(bundle.getString("properties.outputFile"));
        flagsLabel.setText(bundle.getString("properties.flags"));
        resamplerLabel.setText(bundle.getString("properties.resampler"));
        wavtoolLabel.setText(bundle.getString("properties.wavtool"));
        voicebankLabel.setText(bundle.getString("properties.voicebank"));
        // instrumentalLabel.setText(bundle.getString("properties.instrumental"));
        tempoLabel.setText(bundle.getString("properties.tempo"));
        importResamplerButton.setText(bundle.getString("properties.import"));
        importWavtoolButton.setText(bundle.getString("properties.import"));
        importVoicebankButton.setText(bundle.getString("properties.import"));
        // changeInstrumentalButton.setText(bundle.getString("properties.change"));
        applyButton.setText(bundle.getString("general.apply"));
        cancelButton.setText(bundle.getString("general.cancel"));
    }

    @FXML
    void importResampler(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select executable file");
        fc.getExtensionFilters().addAll(
                new ExtensionFilter("Executables", "*", "*.exe"),
                new ExtensionFilter("OSX Executables", "*.out", "*.app"),
                new ExtensionFilter("All Files", "*.*"));
        File file = fc.showOpenDialog(null);
        if (file != null) {
            resamplerPath = file;
            ArrayList<File> allResamplerFiles = updateFileList(RESAMPLER_CATEGORY, file);
            resamplerChoiceBox.setItems(FXCollections.observableArrayList(allResamplerFiles));
            resamplerChoiceBox.getItems().add(0, preferencesManager.getResamplerDefault());
            resamplerChoiceBox.setValue(resamplerPath);
        }
    }

    @FXML
    void importWavtool(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select executable file");
        fc.getExtensionFilters().addAll(
                new ExtensionFilter("Executables", "*", "*.exe"),
                new ExtensionFilter("OSX Executables", "*.out", "*.app"),
                new ExtensionFilter("All Files", "*.*"));
        File file = fc.showOpenDialog(null);
        if (file != null) {
            wavtool = wavtoolConverter.fromString(file.getAbsolutePath());
            ArrayList<File> allWavtoolFiles = updateFileList(WAVTOOL_CATEGORY, file);
            ImmutableList<Wavtool> allWavtools = createWavtoolList(allWavtoolFiles);
            wavtoolChoiceBox.setItems(FXCollections.observableList(allWavtools));
            wavtoolChoiceBox.setValue(wavtool);
        }
    }

    private ImmutableList<Wavtool> createWavtoolList(List<File> wavtoolFiles) {
        List<Wavtool> externalWavtools = wavtoolFiles.stream()
                .map(file -> wavtoolConverter.fromString(file.getAbsolutePath()))
                .collect(Collectors.toList());
        return ImmutableList.<Wavtool>builder()
                .add(wavtoolConverter.fromString(preferencesManager.getWavtoolDefault()))
                .addAll(externalWavtools)
                .build();
    }

    @FXML
    void importVoicebank(ActionEvent event) {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Select voicebank");
        File file = dc.showDialog(null);
        if (file != null && file.isDirectory()) {
            Task<ArrayList<File>> voicebankReadTask = new Task<>() {
                @Override
                protected ArrayList<File> call() throws Exception {
                    voicebankContainer.setVoicebankForRead(file);
                    voicebankContainer.get();
                    // TODO: Throw error if voicebank doesn't load.
                    return updateFileList(VOICEBANK_CATEGORY, file);
                }

                @Override
                protected void succeeded() {
                    super.succeeded();
                    ArrayList<File> allVoicebanks = getValue();
                    voicebankChoiceBox.setItems(FXCollections.observableArrayList(allVoicebanks));
                    voicebankChoiceBox.getItems().add(0, preferencesManager.getVoicebankDefault());
                    voicebankChoiceBox.setValue(file);
                }
            };
            new Thread(voicebankReadTask).start();
        }
    }

    /*@FXML
    void changeInstrumental(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select sound file");
        fc.getExtensionFilters().addAll(
                new ExtensionFilter("Sound files", "*.wav", "*.mp3"),
                new ExtensionFilter("All Files", "*.*"));
        File file = fc.showOpenDialog(null);
        if (file != null) {
            instrumentalPath = Optional.of(file);
            instrumentalName.setText(file.getName());
        }
    }*/

    @FXML
    void applyProperties(ActionEvent event) {
        // Returns whether the cache should be cleared.
        Task<Void> applyPropertiesTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                boolean shouldClearCache = !engine.getResamplerPath().equals(resamplerPath)
                        || !songContainer.get().getFlags().equals(flagsTF.getText())
                        || !songContainer.get().getVoiceDir().equals(voicebankContainer.getLocation());
                songContainer.setSong(
                        songContainer.get().toBuilder().setProjectName(projectNameTF.getText())
                                .setOutputFile(new File(outputFileTF.getText()))
                                .setFlags(flagsTF.getText())
                                .setVoiceDirectory(voicebankContainer.getLocation())
                                .setTempo(RoundUtils.round(tempoSlider.getValue()))
                                //.setInstrumental(instrumentalPath)
                                .build());
                engine.setResamplerPath(resamplerChoiceBox.getValue());
                engine.setWavtool(wavtoolChoiceBox.getValue());
                if (shouldClearCache) {
                    // Should only be called after song changes are applied.
                    songContainer.get().clearAllCacheValues();
                }
                return null;
            }

            @Override
            protected void succeeded() {
                super.succeeded();
                onSongChange.run();
            }
        };
        new Thread(applyPropertiesTask).start();
        Stage currentStage = (Stage) root.getScene().getWindow();
        currentStage.close();
    }

    @FXML
    void closeProperties(ActionEvent event) {
        Stage currentStage = (Stage) root.getScene().getWindow();
        currentStage.close();
    }
}
