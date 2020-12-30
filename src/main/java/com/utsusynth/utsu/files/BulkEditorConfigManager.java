package com.utsusynth.utsu.files;

import com.utsusynth.utsu.common.data.EnvelopeData;
import com.utsusynth.utsu.common.data.PitchbendData;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class BulkEditorConfigManager {
    private final EnvelopeData defaultEnvelope;

    public BulkEditorConfigManager(EnvelopeData defaultEnvelope) {
        this.defaultEnvelope = defaultEnvelope;
    }
    public ObservableList<PitchbendData> readPortamentoData() {
        return FXCollections.observableArrayList();
    }

    public void writePortamentoData(ObservableList<PitchbendData> portamentoData) {
        // Write portamento data to file.
    }

    public ObservableList<PitchbendData> readVibratoData() {
        return FXCollections.observableArrayList();
    }

    public void writeVibratoData(ObservableList<PitchbendData> vibratoData) {
        for (int i = 1; i < vibratoData.size(); i++) {
            StringBuilder stringBuilder = new StringBuilder();
            for (int param : vibratoData.get(i).getVibrato()) {
                stringBuilder.append(param).append(",");
            }
            String asString = stringBuilder.toString();
        }
        // Write vibrato data to file.
    }

    public ObservableList<EnvelopeData> readEnvelopeData() {
        return FXCollections.observableArrayList(defaultEnvelope);
    }

    public void writeEnvelopeData(ObservableList<EnvelopeData> envelopeData) {
        for (int i = 1; i < envelopeData.size(); i++) {
            StringBuilder stringBuilder = new StringBuilder();
            for (double width : envelopeData.get(i).getWidths()) {
                stringBuilder.append(width).append(",");
            }
            for (double height : envelopeData.get(i).getHeights()) {
                // Don't worry about trailing comma.
                stringBuilder.append(height).append(",");
            }
            String asString = stringBuilder.toString();
        }
        // Write envelope data to file.
    }
}
