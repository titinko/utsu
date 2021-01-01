package com.utsusynth.utsu.files;

import com.google.common.collect.ImmutableList;
import com.utsusynth.utsu.UtsuModule.SettingsPath;
import com.utsusynth.utsu.common.data.EnvelopeData;
import com.utsusynth.utsu.common.data.PitchbendData;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class BulkEditorConfigManager {
    private final File configPath;
    private final File portamentoConfigPath;
    private final File vibratoConfigPath;
    private final File envelopeConfigPath;
    private final PitchbendData defaultPitchbend;
    private final EnvelopeData defaultEnvelope;

    public BulkEditorConfigManager(
            @SettingsPath File settingsPath,
            PitchbendData defaultPitchbend,
            EnvelopeData defaultEnvelope) {
        configPath = new File(settingsPath, "config");
        portamentoConfigPath = new File(configPath, "portamento_config.txt");
        vibratoConfigPath = new File(configPath, "vibrato_config.txt");
        envelopeConfigPath = new File(configPath, "envelope_config.txt");
        this.defaultPitchbend = defaultPitchbend;
        this.defaultEnvelope = defaultEnvelope;
    }

    public ObservableList<PitchbendData> readPortamentoData() {
        ObservableList<PitchbendData> portamentoData =
                FXCollections.observableArrayList(defaultPitchbend);
        if (!portamentoConfigPath.canRead()) {
            return portamentoData;
        }
        try {
            for (String line : FileUtils.readLines(portamentoConfigPath, StandardCharsets.UTF_8)) {
                String[] splitLine = line.split(" ");
                if (splitLine.length < 2) {
                    System.out.println("Error: Could not parse portamento config: " + line);
                    continue;
                }
                ImmutableList.Builder<Double> pbsBuilder = ImmutableList.builder();
                for (String pbsData : splitLine[0].split(",")) {
                    pbsBuilder.add(Double.parseDouble(pbsData));
                }
                ImmutableList.Builder<Double> pbwBuilder = ImmutableList.builder();
                for (String pbwData : splitLine[1].split(",")) {
                    pbwBuilder.add(Double.parseDouble(pbwData));
                }
                ImmutableList.Builder<Double> pbyBuilder = ImmutableList.builder();
                if (splitLine.length >= 3) {
                    for (String pbyData : splitLine[2].split(",")) {
                        pbyBuilder.add(Double.parseDouble(pbyData));
                    }
                }
                ImmutableList.Builder<String> pbmBuilder = ImmutableList.builder();
                if (splitLine.length >= 4) {
                    for (String pbmData : splitLine[3].split(",")) {
                        pbmBuilder.add(pbmData);
                    }
                }
                portamentoData.add(new PitchbendData(
                        pbsBuilder.build(),
                        pbwBuilder.build(),
                        pbyBuilder.build(),
                        pbmBuilder.build()));
            }
        } catch (IOException | NumberFormatException e) {
            System.out.println("Error: Unable to read portamento config.");
        }
        return portamentoData;
    }

    public void writePortamentoData(ObservableList<PitchbendData> portamentoData) {
        // Initialize config file.
        if (!configPath.exists() && !configPath.mkdirs()) {
            System.out.println("Error: Failed to create config path.");
            return;
        }
        if (portamentoData.size() <= 1) {
            return; // Only write non-default data to file.
        }
        try (PrintStream ps = new PrintStream(portamentoConfigPath)) {
            for (int i = 1; i < portamentoData.size(); i++) {
                StringBuilder stringBuilder = new StringBuilder();
                for (double pbs : portamentoData.get(i).getPBS()) {
                    stringBuilder.append(pbs).append(",");
                }
                stringBuilder.append(" ");
                for (double pbw : portamentoData.get(i).getPBW()) {
                    stringBuilder.append(pbw).append(",");
                }
                stringBuilder.append(" ");
                for (double pby : portamentoData.get(i).getPBY()) {
                    stringBuilder.append(pby).append(",");
                }
                stringBuilder.append(" ");
                for (String pbm : portamentoData.get(i).getPBM()) {
                    stringBuilder.append(pbm).append(",");
                }
                ps.println(stringBuilder.toString());
            }
            ps.flush();
        } catch (FileNotFoundException e) {
            System.out.println("Error: Failed to create config file.");
        }
    }

    public ObservableList<PitchbendData> readVibratoData() {
        ObservableList<PitchbendData> vibratoData =
                FXCollections.observableArrayList(defaultPitchbend);
        if (!vibratoConfigPath.canRead()) {
            return vibratoData;
        }
        try {
            for (String line : FileUtils.readLines(vibratoConfigPath, StandardCharsets.UTF_8)) {
                int[] vibrato = new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
                String[] splitLine = line.split(",");
                if (splitLine.length != vibrato.length) {
                    System.out.println("Error: Could not parse vibrato config:" + line);
                    continue;
                }
                for (int i = 0; i < splitLine.length; i++) {
                    vibrato[i] = Integer.parseInt(splitLine[i]);
                }
                vibratoData.add(defaultPitchbend.withVibrato(Optional.of(vibrato)));
            }
        } catch (IOException | NumberFormatException e) {
            System.out.println("Error: Unable to read vibrato config.");
        }
        return vibratoData;
    }

    public void writeVibratoData(ObservableList<PitchbendData> vibratoData) {
        // Initialize config file.
        if (!configPath.exists() && !configPath.mkdirs()) {
            System.out.println("Error: Failed to create config path.");
            return;
        }
        if (vibratoData.size() <= 1) {
            return; // Only write non-default data to file.
        }
        try (PrintStream ps = new PrintStream(vibratoConfigPath)) {
            for (int i = 1; i < vibratoData.size(); i++) {
                StringBuilder stringBuilder = new StringBuilder();
                for (int param : vibratoData.get(i).getVibrato()) {
                    stringBuilder.append(param).append(",");
                }
                ps.println(stringBuilder.toString());
            }
            ps.flush();
        } catch (FileNotFoundException e) {
            System.out.println("Error: Failed to create config file.");
        }
    }

    public ObservableList<EnvelopeData> readEnvelopeData() {
        ObservableList<EnvelopeData> envelopeData =
                FXCollections.observableArrayList(defaultEnvelope);
        if (!envelopeConfigPath.canRead()) {
            return envelopeData;
        }
        try {
            for (String line : FileUtils.readLines(envelopeConfigPath, StandardCharsets.UTF_8)) {
                if (line.split(" ").length != 2) {
                    System.out.println("Error: Could not parse envelope config:" + line);
                    continue;
                }
                double[] widths = new double[5];
                String[] widthLine = line.split(" ")[0].split(",");
                if (widthLine.length != widths.length) {
                    System.out.println("Error: Could not parse envelope config:" + line);
                    continue;
                }
                for (int i = 0; i < widthLine.length; i++) {
                    widths[i] = Double.parseDouble(widthLine[i]);
                }
                double[] heights = new double[5];
                String[] heightLine = line.split(" ")[1].split(",");
                for (int i = 0; i < heightLine.length; i++) {
                    heights[i] = Double.parseDouble(heightLine[i]);
                }
                envelopeData.add(new EnvelopeData(widths, heights));
            }
        } catch (IOException | NumberFormatException e) {
            System.out.println("Error: Unable to read envelope config.");
        }
        return envelopeData;
    }

    public void writeEnvelopeData(ObservableList<EnvelopeData> envelopeData) {
        // Initialize config file.
        if (!configPath.exists() && !configPath.mkdirs()) {
            System.out.println("Error: Failed to create config path.");
            return;
        }
        if (envelopeData.size() <= 1) {
            return; // Only write non-default data to file.
        }
        try (PrintStream ps = new PrintStream(envelopeConfigPath)) {
            for (int i = 1; i < envelopeData.size(); i++) {
                StringBuilder stringBuilder = new StringBuilder();
                for (double width : envelopeData.get(i).getWidths()) {
                    stringBuilder.append(width).append(",");
                }
                stringBuilder.append(" ");
                for (double height : envelopeData.get(i).getHeights()) {
                    // Don't worry about trailing comma.
                    stringBuilder.append(height).append(",");
                }
                ps.println(stringBuilder.toString());
            }
            ps.flush();
        } catch (FileNotFoundException e) {
            System.out.println("Error: Failed to create config file.");
        }
    }
}
