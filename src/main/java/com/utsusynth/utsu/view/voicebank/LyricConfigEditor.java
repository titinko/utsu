package com.utsusynth.utsu.view.voicebank;

import java.io.File;
import java.util.Optional;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.utsusynth.utsu.common.data.FrequencyData;
import com.utsusynth.utsu.common.data.LyricConfigData;
import com.utsusynth.utsu.common.data.WavData;
import com.utsusynth.utsu.common.i18n.Localizer;
import com.utsusynth.utsu.engine.Engine;
import com.utsusynth.utsu.engine.Resampler;
import com.utsusynth.utsu.files.CacheManager;
import com.utsusynth.utsu.files.voicebank.SoundFileReader;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Side;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.shape.Line;
import javafx.util.Duration;

public class LyricConfigEditor {
    private static final int maxAmplitude = 32767;
    private static final double scaleX = 0.8;
    private static final int height = 150;
    private static MediaPlayer mediaPlayer; // Used for audio playback.

    private final SoundFileReader soundFileReader;
    private final Localizer localizer;


    private Optional<LyricConfigData> configData;
    private LyricConfigCallback model;
    private GridPane background;
    private LineChart<Number, Number> chart;
    private Group controlBars;


    // Temporary cache values.
    private boolean changed = false;
    private double[] cachedConfig;

    @Inject
    public LyricConfigEditor(
            SoundFileReader soundFileReader,
            Localizer localizer
    ) {
        this.soundFileReader = soundFileReader;
        this.localizer = localizer;
        // Initialize with dummy data.
        configData = Optional.empty();
        background = new GridPane();
        chart = new LineChart<>(new NumberAxis(), new NumberAxis());
        chart.setOpacity(0);
        controlBars = new Group();

    }

    /** Initialize editor with data from the controller. */
    public void initialize(LyricConfigCallback callback) {
        model = callback;
    }

    public GridPane createConfigEditor(LyricConfigData config) {
        this.configData = Optional.of(config);
        double lengthMs = createLineChart(config);

        background = new GridPane();
        double curLength = lengthMs;
        double offsetLength = Math.max(Math.min(config.offsetProperty().get(), curLength), 0);
        curLength -= offsetLength;
        double cutoffLength = 0;
        if (config.cutoffProperty().get() >= 0) {
            cutoffLength = Math.max(Math.min(config.cutoffProperty().get(), curLength), 0);
        } else {
            cutoffLength = Math.max(curLength + config.cutoffProperty().get(), 0);
        }
        curLength -= cutoffLength;
        double consonantLength = Math.max(Math.min(config.consonantProperty().get(), curLength), 0);

        double totalX = lengthMs * scaleX;
        double offsetBarX = offsetLength * scaleX;
        double consonantBarX = (offsetLength + consonantLength) * scaleX;
        double cutoffBarX = (lengthMs - cutoffLength) * scaleX;
        double preutterBarX = Math.min(
                Math.max((offsetLength + config.preutterProperty().get()) * scaleX, 0),
                totalX);
        double overlapBarX = Math
                .min(Math.max((offsetLength + config.overlapProperty().get()) * scaleX, 0), totalX);

        // Background colors.
        Pane offsetPane = createBackground(offsetBarX, "offset");
        Pane consonantPane = createBackground(consonantBarX - offsetBarX, "consonant");
        Pane vowelPane = createBackground(cutoffBarX - consonantBarX, "vowel");
        Pane cutoffPane = createBackground(totalX - cutoffBarX, "cutoff");
        background.addRow(0, offsetPane, consonantPane, vowelPane, cutoffPane);

        // Control bars.
        controlBars.getChildren().clear();
        Line offsetBar = createControlBar(config, offsetBarX, totalX, "offset");
        Line consonantBar = createControlBar(config, consonantBarX, totalX, "consonant");
        Line cutoffBar = createControlBar(config, cutoffBarX, totalX, "cutoff");
        Line preutterBar = createControlBar(config, preutterBarX, totalX, "preutterance");
        Line overlapBar = createControlBar(config, overlapBarX, totalX, "overlap");
        controlBars.getChildren()
                .setAll(offsetBar, overlapBar, preutterBar, consonantBar, cutoffBar);

        // Context menu for config editor.

        MenuItem playItem = new MenuItem("Play");
        playItem.setOnAction(event -> {
            playSound();
        });


        MenuItem playItemWithResampler = new MenuItem("Play with resampler");
        playItemWithResampler.setOnAction(event -> {
            playSoundWithResampler(true);
        });

        MenuItem playItemWithResamplerNoModulation = new MenuItem("Play with resampler (no modulation)");
        playItemWithResamplerNoModulation.setOnAction(event -> {
            playSoundWithResampler(false);
        });

        ContextMenu contextMenu = new ContextMenu();
        contextMenu.getItems().addAll(playItem, playItemWithResampler,playItemWithResamplerNoModulation);
        contextMenu.setOnShowing(event -> playItem.setText(localizer.getMessage("voice.play")));
        contextMenu.setOnShowing(event -> playItemWithResampler.setText(localizer.getMessage("voice.playWithResampler")));
        contextMenu.setOnShowing(event -> playItemWithResamplerNoModulation.setText(localizer.getMessage("voice.playWithResamplerNoModulation")));
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
            config.offsetProperty().set(newX / scaleX);

            // Move preutter bar along with offset bar.
            double rawPreutterX = preutterBar.getStartX() + addedX;
            double preutterX = Math.min(Math.max(rawPreutterX, 0), totalX);
            preutterBar.setStartX(preutterX);
            preutterBar.setEndX(preutterX);
            if (rawPreutterX != preutterX) {
                config.preutterProperty().set((preutterX - newX) / scaleX);
            }
            // Move overlap bar along with offset bar.
            double rawOverlapX = overlapBar.getStartX() + addedX;
            double overlapX = Math.min(Math.max(rawOverlapX, 0), totalX);
            overlapBar.setStartX(overlapX);
            overlapBar.setEndX(overlapX);
            if (rawOverlapX != overlapX) {
                config.overlapProperty().set((overlapX - newX) / scaleX);
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
            config.consonantProperty().set(consonantPane.getPrefWidth() / scaleX);
        });
        cutoffBar.setOnMouseDragged(event -> {
            changed = true;
            double newX = Math.min(Math.max(event.getX(), consonantBar.getStartX()), totalX);
            double addedX = newX - cutoffBar.getStartX();
            cutoffBar.setStartX(newX);
            cutoffBar.setEndX(newX);
            vowelPane.setPrefWidth(vowelPane.getPrefWidth() + addedX);
            cutoffPane.setPrefWidth(cutoffPane.getPrefWidth() - addedX);
            config.cutoffProperty().set(cutoffPane.getPrefWidth() / scaleX);
        });
        preutterBar.setOnMouseDragged(event -> {
            changed = true;
            double newX = Math.min(Math.max(event.getX(), 0), totalX);
            preutterBar.setStartX(newX);
            preutterBar.setEndX(newX);
            double newPreutterX = newX - offsetBar.getStartX();
            config.preutterProperty().set(newPreutterX / scaleX);
        });
        overlapBar.setOnMouseDragged(event -> {
            changed = true;
            double newX = Math.min(Math.max(event.getX(), 0), totalX);
            overlapBar.setStartX(newX);
            overlapBar.setEndX(newX);
            double newOverlapX = newX - offsetBar.getStartX();
            config.overlapProperty().set(newOverlapX / scaleX);
        });

        return background;
    }

    public LineChart<Number, Number> getChartElement() {
        return chart;
    }

    public Group getControlElement() {
        return controlBars;
    }

    /**
     * Play a note using the resampler
     * The note is played using pitch C4 (midi 60) during 2 seconds
     *
     * @param modulation if false, modulation on resampler is set to 0
     */
    public void playSoundWithResampler(boolean modulation) {
        if (!configData.isPresent()) {
            return;
        }

        model.playLyricWithResampler(configData.get(), modulation);
    }
    public void playSound() {
        if (!configData.isPresent()) {
            return;
        }
        Media media = new Media(configData.get().getPathToFile().toURI().toString());
        mediaPlayer = new MediaPlayer(media);
        mediaPlayer.setOnReady(() -> {
            double lengthMs = media.getDuration().toMillis();

            double offsetLengthMs =
                    Math.max(Math.min(configData.get().offsetProperty().get(), lengthMs), 0);
            double cutoffLengthMs = configData.get().cutoffProperty().get();
            if (cutoffLengthMs >= 0) {
                cutoffLengthMs = Math.max(Math.min(cutoffLengthMs, lengthMs - offsetLengthMs), 0);
            } else {
                cutoffLengthMs = Math.max(lengthMs - offsetLengthMs + cutoffLengthMs, 0);
            }
            mediaPlayer.setStartTime(Duration.millis(offsetLengthMs));
            mediaPlayer.setStopTime(Duration.millis(lengthMs - cutoffLengthMs));

            mediaPlayer.play();
        });
    }

    private Pane createBackground(double widthX, String style) {
        Pane pane = new Pane();
        pane.setPrefWidth(widthX);
        pane.setPrefHeight(height);
        pane.getStyleClass().addAll("background", style);
        return pane;
    }

    private Line createControlBar(
            LyricConfigData config,
            double xPos,
            double totalX,
            String style) {
        Line bar = new Line(xPos, 0, xPos, height);
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
        NumberAxis xAxis = new NumberAxis();
        xAxis.setOpacity(0);
        xAxis.setTickLabelsVisible(false);
        xAxis.setTickMarkVisible(false);
        xAxis.setMinorTickVisible(false);
        NumberAxis yAxis = new NumberAxis();
        yAxis.setAutoRanging(false);
        yAxis.setLowerBound(-maxAmplitude);
        yAxis.setUpperBound(maxAmplitude);
        yAxis.setTickUnit(maxAmplitude / 5.0);
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
        chart.setPrefHeight(height);

        // Initialize chart data sets.
        ObservableList<Data<Number, Number>> wavSamples = FXCollections.observableArrayList();
        Series<Number, Number> waveform = new Series<>(wavSamples);
        ObservableList<Data<Number, Number>> frqSamples = FXCollections.observableArrayList();
        Series<Number, Number> frequency = new Series<>(frqSamples);
        chart.getData().setAll(ImmutableList.of(waveform, frequency));

        // Populate wav chart data.
        File pathToWav = config.getPathToFile();
        Optional<WavData> wavData = soundFileReader.loadWavData(pathToWav);
        double msPerSample = 0;
        if (wavData.isPresent()) {
            msPerSample = wavData.get().getLengthMs() / wavData.get().getSamples().length;
            double currentTimeMs = msPerSample / 2; // Data point is halfway through sample.
            double ampSum = 0;
            for (int i = 0; i < wavData.get().getSamples().length; i++) {
                ampSum += Math.abs(wavData.get().getSamples()[i]);
                if (i % 100 == 0) {
                    // Only render every 100th sample to avoid overloading the frontend.
                    double ampValue = i % 200 == 0 ? ampSum / 100.0 : ampSum / -100.0;
                    wavSamples.add(new Data<>(currentTimeMs, ampValue));
                    ampSum = 0;
                }
                currentTimeMs += msPerSample;
            }
            // Preferred width is 800 pixels per second.
            chart.setPrefWidth(wavData.get().getLengthMs() * scaleX);
        } else {
            // Make chart invisible if wav file can't be read.
            chart.setOpacity(0);
        }

        // Populate frequency chart data.
        populateFrqValues(frqSamples, pathToWav, wavData);

        return wavData.isPresent() ? wavData.get().getLengthMs() : 0.0;
    }

    private void populateFrqValues(
            ObservableList<Data<Number, Number>> frqSamples,
            File wavFile,
            Optional<WavData> wavData) {
        if (!wavData.isPresent()) {
            return;
        }
        double msPerSample = wavData.get().getLengthMs() / wavData.get().getSamples().length;

        // Populate frequency chart data.
        String wavName = wavFile.getName();
        String frqName = wavFile.getName().substring(0, wavName.length() - 4) + "_wav.frq";
        File frqFile = wavFile.getParentFile().toPath().resolve(frqName).toFile();
        Optional<FrequencyData> frqData = soundFileReader.loadFrqData(frqFile);
        // Don't bother populating frq data if there is no wav data.
        if (wavData.isPresent() && frqData.isPresent()) {
            frqSamples.clear();
            double avgFreq = frqData.get().getAverageFreq();
            double msPerFrqValue = frqData.get().getSamplesPerFreqValue() * msPerSample;
            double currentTimeMs = msPerFrqValue / 2; // Data point is halfway through frq value.
            for (double frqValue : frqData.get().getFrequencies()) {
                // Scale to a value of [-10, 10] to make a good logistic function input.
                double scaledFrq = (frqValue - avgFreq) * 10 / avgFreq;
                // Apply logistic function to enhance central values.
                double squashedFrq = (maxAmplitude * 2 / (1 + Math.exp(-scaledFrq)));
                frqSamples.add(new Data<>(currentTimeMs, squashedFrq - maxAmplitude));
                currentTimeMs += msPerFrqValue;
            }
        }
    }
}
