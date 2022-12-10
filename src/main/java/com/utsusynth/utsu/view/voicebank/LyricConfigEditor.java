package com.utsusynth.utsu.view.voicebank;

import java.io.File;
import java.util.List;
import java.util.Optional;

import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.utsusynth.utsu.common.StatusBar;
import com.utsusynth.utsu.common.data.FrequencyData;
import com.utsusynth.utsu.common.data.LyricConfigData;
import com.utsusynth.utsu.common.data.WavData;
import com.utsusynth.utsu.common.i18n.Localizer;
import com.utsusynth.utsu.files.voicebank.SoundFileReader;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

public class LyricConfigEditor {
    private static final double SCALE_X = 0.8;
    private static final int HEIGHT = 150;
    private static MediaPlayer mediaPlayer; // Used for audio playback.

    private final Group controlBars;
    private final SoundFileReader soundFileReader;
    private final Spectrogram spectrogram;
    private final Localizer localizer;
    private final StatusBar statusBar;

    private final BooleanProperty isPlaying;
    private final BooleanProperty showFrequency;
    private final BooleanProperty showWaveform;
    private final BooleanProperty showSpectrogram;

    private LyricConfigCallback model;

    // Recreated on each call to createConfigEditor.
    private LyricConfigData configData;
    private WavData wavData;
    private GridPane background;
    private LineChart<Number, Number> chart;
    private ImageView spectrogramView;
    private Label playButton; // May be null.

    // Temporary cache values.
    private boolean changed = false;
    private double[] cachedConfig;

    @Inject
    public LyricConfigEditor(
            SoundFileReader soundFileReader, Spectrogram spectrogram, Localizer localizer, StatusBar statusBar) {
        this.soundFileReader = soundFileReader;
        this.spectrogram = spectrogram;
        this.localizer = localizer;
        this.statusBar = statusBar;

        // Initialize with dummy data.
        background = new GridPane();
        chart = new LineChart<>(new NumberAxis(), new NumberAxis());
        chart.setOpacity(0);
        controlBars = new Group();
        spectrogramView = new ImageView();

        isPlaying = new SimpleBooleanProperty(false);
        isPlaying.addListener(obs -> {
            if (isPlaying.get()) {
                if (playButton != null && playButton.getStyleClass().stream().noneMatch(
                        str -> str.equals("highlighted"))) {
                    playButton.getStyleClass().add("highlighted");
                }
                playSoundInternal();
            } else {
                if (playButton != null) {
                    playButton.getStyleClass().remove("highlighted");
                }
            }
        });
        showFrequency = new SimpleBooleanProperty(true);
        showWaveform = new SimpleBooleanProperty(true);
        showSpectrogram = new SimpleBooleanProperty(false);
        showSpectrogram.addListener(obs -> {
            if (!showSpectrogram.get() || spectrogramView == null
                    || spectrogramView.getImage() != null || wavData == null) {
                return;
            }
            spectrogramView.setImage(spectrogram.createSpectrogram(wavData, HEIGHT));
        });

    }

    /** Initialize editor with data from the controller. */
    public void initialize(LyricConfigCallback callback) {
        model = callback;
    }

    public List<Node> createConfigEditor(LyricConfigData config) {
        this.configData = config;
        double lengthMs = createLineChart(config);

        boolean drawSpec = showSpectrogram.get() && wavData != null;
        spectrogramView = drawSpec ? new ImageView(spectrogram.createSpectrogram(wavData, HEIGHT))
                : new ImageView();
        spectrogramView.setFitWidth(lengthMs * SCALE_X);
        spectrogramView.setMouseTransparent(true);
        spectrogramView.visibleProperty().bind(showSpectrogram);

        background = new GridPane();
        double curLength = lengthMs;
        double offsetLength = Math.max(Math.min(config.offsetProperty().get(), curLength), 0);
        curLength -= offsetLength;
        double cutoffLength;
        if (config.cutoffProperty().get() >= 0) {
            cutoffLength = Math.max(Math.min(config.cutoffProperty().get(), curLength), 0);
        } else {
            cutoffLength = Math.max(curLength + config.cutoffProperty().get(), 0);
        }
        curLength -= cutoffLength;
        double consonantLength = Math.max(Math.min(config.consonantProperty().get(), curLength), 0);

        double totalX = lengthMs * SCALE_X;
        double offsetBarX = offsetLength * SCALE_X;
        double consonantBarX = (offsetLength + consonantLength) * SCALE_X;
        double cutoffBarX = (lengthMs - cutoffLength) * SCALE_X;
        double preutterBarX = Math.min(
                Math.max((offsetLength + config.preutterProperty().get()) * SCALE_X, 0),
                totalX);
        double overlapBarX = Math
                .min(Math.max((offsetLength + config.overlapProperty().get()) * SCALE_X, 0), totalX);

        // Background colors.
        Pane offsetPane = createBackground(offsetBarX, "offset");
        Pane consonantPane = createBackground(consonantBarX - offsetBarX, "consonant");
        Pane vowelPane = createBackground(cutoffBarX - consonantBarX, "vowel");
        Pane cutoffPane = createBackground(totalX - cutoffBarX, "cutoff");
        background.addRow(0, offsetPane, consonantPane, vowelPane, cutoffPane);

        // Control bars.
        controlBars.getChildren().clear();
        Line offsetBar = createControlBar(config, offsetBarX, "offset");
        Line consonantBar = createControlBar(config, consonantBarX, "consonant");
        Line cutoffBar = createControlBar(config, cutoffBarX, "cutoff");
        Line preutterBar = createControlBar(config, preutterBarX, "preutterance");
        Line overlapBar = createControlBar(config, overlapBarX, "overlap");
        controlBars.getChildren()
                .setAll(offsetBar, overlapBar, preutterBar, consonantBar, cutoffBar);

        // Context menu for config editor.
        MenuItem playItem = new MenuItem("Play");
        playItem.setOnAction(event -> playSound());
        MenuItem playItemWithResampler = new MenuItem("Play with resampler");
        playItemWithResampler.setOnAction(event -> playSoundWithResampler(100));
        MenuItem playItemWithResamplerNoModulation =
                new MenuItem("Play with resampler (no modulation)");
        playItemWithResamplerNoModulation.setOnAction(event -> playSoundWithResampler(0));

        ContextMenu contextMenu = new ContextMenu();
        contextMenu.getItems().addAll(
                playItem, playItemWithResampler, playItemWithResamplerNoModulation);
        contextMenu.setOnShowing(event -> playItem.setText(localizer.getMessage("voice.play")));
        contextMenu.setOnShowing(event -> playItemWithResampler.setText(
                localizer.getMessage("voice.playWithResampler")));
        contextMenu.setOnShowing(event -> playItemWithResamplerNoModulation.setText(
                localizer.getMessage("voice.playWithResamplerNoModulation")));
        background.setOnContextMenuRequested(event -> {
            contextMenu.show(background, event.getScreenX(), event.getScreenY());
        });
        controlBars.setOnContextMenuRequested(event -> {
            contextMenu.show(controlBars, event.getScreenX(), event.getScreenY());
        });
        background.setOnMousePressed(event -> {
            contextMenu.hide();
        });
        controlBars.setOnMousePressed(event -> {
            contextMenu.hide();
        });

        // Enable dragging bars to change config values.
        offsetBar.setOnMouseDragged(event -> {
            changed = true;
            double newX = Math.min(
                    Math.max(event.getX(), 0),
                    cutoffBar.getStartX() - consonantPane.getPrefWidth());
            double addedX = newX - offsetBar.getStartX();
            offsetBar.setStartX(newX);
            offsetBar.setEndX(newX);
            consonantBar.setStartX(consonantBar.getStartX() + addedX);
            consonantBar.setEndX(consonantBar.getEndX() + addedX);
            offsetPane.setPrefWidth(offsetPane.getPrefWidth() + addedX);
            vowelPane.setPrefWidth(vowelPane.getPrefWidth() - addedX);
            config.offsetProperty().set(newX / SCALE_X);

            // Move preutter bar along with offset bar.
            double rawPreutterX = preutterBar.getStartX() + addedX;
            double preutterX = Math.min(Math.max(rawPreutterX, 0), totalX);
            preutterBar.setStartX(preutterX);
            preutterBar.setEndX(preutterX);
            if (rawPreutterX != preutterX) {
                config.preutterProperty().set((preutterX - newX) / SCALE_X);
            }
            // Move overlap bar along with offset bar.
            double rawOverlapX = overlapBar.getStartX() + addedX;
            double overlapX = Math.min(Math.max(rawOverlapX, 0), totalX);
            overlapBar.setStartX(overlapX);
            overlapBar.setEndX(overlapX);
            if (rawOverlapX != overlapX) {
                config.overlapProperty().set((overlapX - newX) / SCALE_X);
            }
        });
        consonantBar.setOnMouseDragged(event -> {
            changed = true;
            double newX =
                    Math.min(Math.max(event.getX(), offsetBar.getStartX()), cutoffBar.getStartX());
            double addedX = newX - consonantBar.getStartX();
            consonantBar.setStartX(newX);
            consonantBar.setEndX(newX);
            consonantPane.setPrefWidth(consonantPane.getPrefWidth() + addedX);
            vowelPane.setPrefWidth(vowelPane.getPrefWidth() - addedX);
            config.consonantProperty().set(consonantPane.getPrefWidth() / SCALE_X);
        });
        cutoffBar.setOnMouseDragged(event -> {
            changed = true;
            double newX = Math.min(Math.max(event.getX(), consonantBar.getStartX()), totalX);
            double addedX = newX - cutoffBar.getStartX();
            cutoffBar.setStartX(newX);
            cutoffBar.setEndX(newX);
            vowelPane.setPrefWidth(vowelPane.getPrefWidth() + addedX);
            cutoffPane.setPrefWidth(cutoffPane.getPrefWidth() - addedX);
            config.cutoffProperty().set(cutoffPane.getPrefWidth() / SCALE_X);
        });
        preutterBar.setOnMouseDragged(event -> {
            changed = true;
            double newX = Math.min(Math.max(event.getX(), 0), totalX);
            preutterBar.setStartX(newX);
            preutterBar.setEndX(newX);
            double newPreutterX = newX - offsetBar.getStartX();
            config.preutterProperty().set(newPreutterX / SCALE_X);
        });
        overlapBar.setOnMouseDragged(event -> {
            changed = true;
            double newX = Math.min(Math.max(event.getX(), 0), totalX);
            overlapBar.setStartX(newX);
            overlapBar.setEndX(newX);
            double newOverlapX = newX - offsetBar.getStartX();
            config.overlapProperty().set(newOverlapX / SCALE_X);
        });

        return ImmutableList.of(background, spectrogramView, controlBars, chart);
    }

    public Group getControlElement() {
        return controlBars;
    }

    public List<Node> createConfigSidebar() {
        playButton = createLabelButton("▶", "Play", isPlaying);
        return ImmutableList.of(
                playButton,
                createLabelButton("F", "Show Frequency", showFrequency),
                createLabelButton("W", "Show Waveform", showWaveform),
                createLabelButton("S", "Show Spectrogram", showSpectrogram));
    }

    public void redrawSpectrogram() {
        if (spectrogramView != null && wavData != null) {
            spectrogramView.setImage(spectrogram.createSpectrogram(wavData, HEIGHT));
        }
    }

    /**
     * Play a note using the resampler.
     * The note is played using pitch C4 (midi 60) for 2 seconds.
     */
    public void playSoundWithResampler(int modulation) {
        if (configData == null) {
            return;
        }
        model.playLyricWithResampler(configData, modulation);
    }

    public void playSound() {
        isPlaying.set(true);
    }

    private void playSoundInternal() {
        if (configData == null) {
            isPlaying.set(false);
            return;
        }
        // TODO: Consider moving this code to the engine.
        Media media = new Media(configData.getPathToFile().toURI().toString());
        mediaPlayer = new MediaPlayer(media);
        mediaPlayer.setOnReady(() -> {
            double lengthMs = media.getDuration().toMillis();
            double offsetLengthMs =
                    Math.max(Math.min(configData.offsetProperty().get(), lengthMs), 0);
            double cutoffLengthMs = configData.cutoffProperty().get();
            if (cutoffLengthMs >= 0) {
                cutoffLengthMs = Math.max(Math.min(cutoffLengthMs, lengthMs - offsetLengthMs), 0);
            } else {
                cutoffLengthMs = Math.max(lengthMs - offsetLengthMs + cutoffLengthMs, 0);
            }
            mediaPlayer.setStartTime(Duration.millis(offsetLengthMs));
            mediaPlayer.setStopTime(Duration.millis(lengthMs - cutoffLengthMs));
            mediaPlayer.play();
            mediaPlayer.setOnEndOfMedia(() -> {
                isPlaying.set(false);
            });
        });
    }

    private Pane createBackground(double widthX, String style) {
        Pane pane = new Pane();
        pane.setPrefWidth(widthX);
        pane.setPrefHeight(HEIGHT);
        pane.getStyleClass().addAll("background", style);
        return pane;
    }

    private Line createControlBar(
            LyricConfigData config,
            double xPos,
            String style) {
        Line bar = new Line(xPos, 0, xPos, HEIGHT);
        bar.getStyleClass().add(style);
        bar.setOnMouseEntered(event -> {
            if (Math.abs(event.getX() - bar.getStartX()) <= 3) {
                bar.getScene().setCursor(Cursor.W_RESIZE);
            }
            if (!bar.getStyleClass().contains("selected")) {
                bar.getStyleClass().add("selected");
            }
        });
        bar.setOnMouseExited(event -> {
            bar.getScene().setCursor(Cursor.DEFAULT);
            if (bar.getStyleClass().contains("selected")) {
                bar.getStyleClass().remove("selected");
            }
        });
        bar.setOnMousePressed(event -> {
            changed = false;
            cachedConfig = config.getConfigValues();
        });
        bar.setOnMouseReleased(event -> {
            if (changed) {
                final double[] oldConfig = cachedConfig;
                final double[] newConfig = config.getConfigValues();
                model.recordAction(() -> {
                    config.setConfigValues(newConfig);
                    model.refreshEditor(config);
                }, () -> {
                    config.setConfigValues(oldConfig);
                    model.refreshEditor(config);
                });
            }
        });
        return bar;
    }

    private double createLineChart(LyricConfigData config) {
        // Initialize chart data sets.
        ObservableList<Data<Number, Number>> wavSamples = FXCollections.observableArrayList();
        Series<Number, Number> waveform = new Series<>(wavSamples);
        ObservableList<Data<Number, Number>> frqSamples = FXCollections.observableArrayList();
        Series<Number, Number> frequency = new Series<>(frqSamples);

        // Populate wav chart data.
        File pathToWav = config.getPathToFile();
        Optional<WavData> maybeWavData = soundFileReader.loadWavData(pathToWav, message -> {
            statusBar.setText(message); // Read frq data and report output on status bar.
            return null;
        });
        if (maybeWavData.isEmpty()) {
            chart = new LineChart<>(new NumberAxis(), new NumberAxis());
            chart.setMouseTransparent(true);
            chart.setOpacity(0); // Make chart invisible if wav file can't be read.
            return 0.0;
        }
        wavData = maybeWavData.get();
        double msPerSample = wavData.getLengthMs() / wavData.getSamples().length;
        double currentTimeMs = msPerSample / 2; // Data point is halfway through sample.
        double ampSum = 0;
        for (int i = 0; i < wavData.getSamples().length; i++) {
            ampSum += Math.abs(wavData.getSamples()[i]);
            if (i % 100 == 0) {
                // Only render every 100th sample to avoid overloading the frontend.
                double ampValue = i % 200 == 0 ? ampSum / 100.0 : ampSum / -100.0;
                wavSamples.add(new Data<>(currentTimeMs, ampValue));
                ampSum = 0;
            }
            currentTimeMs += msPerSample;
        }
        // Preferred width is 800 pixels per second.
        NumberAxis xAxis = new NumberAxis();
        xAxis.setAutoRanging(false);
        xAxis.setLowerBound(0);
        xAxis.setUpperBound(wavData.getLengthMs());
        xAxis.setTickUnit(100);
        xAxis.setSide(Side.TOP);
        xAxis.setTickLabelsVisible(true);
        xAxis.setTickMarkVisible(false);
        xAxis.setMinorTickVisible(false);
        xAxis.setTickLabelGap(-15);
        NumberAxis yAxis = new NumberAxis();
        yAxis.setAutoRanging(false);
        yAxis.setLowerBound(-1);
        yAxis.setUpperBound(1);
        yAxis.setTickUnit(0.2);
        yAxis.setSide(Side.RIGHT);
        yAxis.setOpacity(0);
        yAxis.setTickLabelsVisible(false);
        yAxis.setTickMarkVisible(false);
        yAxis.setMinorTickVisible(false);
        chart = new LineChart<>(xAxis, yAxis);
        chart.setMouseTransparent(true);
        chart.setHorizontalGridLinesVisible(false);
        chart.setVerticalGridLinesVisible(false);
        chart.setVerticalZeroLineVisible(false);
        chart.setCreateSymbols(false);
        chart.setPrefHeight(HEIGHT);
        chart.setPrefWidth(wavData.getLengthMs() * SCALE_X);
        chart.getData().setAll(ImmutableList.of(waveform, frequency));
        waveform.getNode().visibleProperty().bind(showWaveform);
        chart.horizontalZeroLineVisibleProperty().bind(showWaveform);
        frequency.getNode().visibleProperty().bind(showFrequency);

        // Populate frequency chart data.
        populateFrqValues(frqSamples, pathToWav);

        return wavData.getLengthMs();
    }

    private void populateFrqValues(ObservableList<Data<Number, Number>> frqSamples, File wavFile) {
        if (wavData == null) {
            return; // Don't bother populating frq values if wav data not present.
        }
        double msPerSample = wavData.getLengthMs() / wavData.getSamples().length;

        // Populate frequency chart data.
        String wavName = wavFile.getName();
        String frqName = wavFile.getName().substring(0, wavName.length() - 4) + "_wav.frq";
        File frqFile = wavFile.getParentFile().toPath().resolve(frqName).toFile();
        Optional<FrequencyData> frqData = soundFileReader.loadFrqData(frqFile, message -> {
            statusBar.setText(message); // Read frq data and report output on status bar.
            return null;
        });
        // Don't bother populating frq data if there is no wav data.
        if (frqData.isPresent()) {
            frqSamples.clear();
            double avgFreq = frqData.get().getAverageFreq();
            double msPerFrqValue = frqData.get().getSamplesPerFreqValue() * msPerSample;
            double currentTimeMs = msPerFrqValue / 2; // Data point is halfway through frq value.
            for (double frqValue : frqData.get().getFrequencies()) {
                // Scale to a value of [-10, 10] to make a good logistic function input.
                double scaledFrq = (frqValue - avgFreq) * 10 / avgFreq;
                // Apply logistic function to enhance central values.
                double squashedFrq = (2.0 / (1.0 + Math.exp(-scaledFrq)));
                frqSamples.add(new Data<>(currentTimeMs, squashedFrq - 1));
                currentTimeMs += msPerFrqValue;
            }
        }
    }

    private Label createLabelButton(String icon, String altText, BooleanProperty toggle) {
        Label button = new Label(icon);
        button.setPrefWidth(15);
        button.setAlignment(Pos.CENTER);

        if (CharMatcher.ascii().matchesAllOf(icon)) {
            button.setFont(Font.font("verdana", FontWeight.NORMAL, 12));
        }
        Tooltip.install(button, new Tooltip(altText));
        button.setOnMouseEntered(event -> {
            if (CharMatcher.ascii().matchesAllOf(icon)) {
                button.setFont(Font.font("verdana", FontWeight.EXTRA_BOLD, 12));
            } else {
                button.setUnderline(true);
            }
        });
        button.setOnMouseExited(event -> {
            if (CharMatcher.ascii().matchesAllOf(icon)) {
                button.setFont(Font.font("verdana", FontWeight.NORMAL, 12));
            } else {
                button.setUnderline(false);
            }
        });
        button.getStyleClass().add("lyric-config-settings");
        if (toggle.get()) {
            button.getStyleClass().add("highlighted");
        }
        button.setOnMouseClicked(event -> {
            toggle.set(!toggle.get());
            if (toggle.get() && !button.getStyleClass().contains("highlighted")) {
                button.getStyleClass().add("highlighted");
            } else if (!toggle.get()) {
                button.getStyleClass().remove("highlighted");
            }
        });
        return button;
    }
}
