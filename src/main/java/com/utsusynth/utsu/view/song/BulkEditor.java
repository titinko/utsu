package com.utsusynth.utsu.view.song;

import com.google.inject.Inject;
import com.utsusynth.utsu.common.data.EnvelopeData;
import com.utsusynth.utsu.view.song.note.envelope.Envelope;
import com.utsusynth.utsu.view.song.note.envelope.EnvelopeFactory;
import com.utsusynth.utsu.view.song.note.pitch.PitchbendFactory;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.DoubleBinding;
import javafx.scene.Group;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;

public class BulkEditor {
    private final EnvelopeFactory envelopeFactory;

    private Envelope currentEnvelope;

    @Inject
    public BulkEditor(EnvelopeFactory envelopeFactory, PitchbendFactory pitchbendFactory) {
        this.envelopeFactory = envelopeFactory;
    }

    public Group createEnvelopeEditor(
            EnvelopeData envelopeData, DoubleBinding width, DoubleBinding height) {
        AnchorPane topCell = new AnchorPane();
        topCell.getStyleClass().add("dynamics-top-cell");
        topCell.prefWidthProperty().bind(width);
        topCell.prefHeightProperty().bind(height.divide(2.0));
        AnchorPane bottomCell = new AnchorPane();
        bottomCell.getStyleClass().add("dynamics-bottom-cell");
        bottomCell.prefWidthProperty().bind(width);
        bottomCell.prefHeightProperty().bind(height.divide(2.0));
        VBox background = new VBox(topCell, bottomCell);

        currentEnvelope = envelopeFactory.createEnvelopeEditor(
                width.get(), height.get(), envelopeData, ((oldData, newData) -> {}));
        Group display = new Group(background, currentEnvelope.getElement());

        // Updating size is still buggy so disabled for this commit.
        InvalidationListener updateSize = obs -> {
            EnvelopeData curData = currentEnvelope.getData();
            currentEnvelope = envelopeFactory.createEnvelopeEditor(
                    width.get(), height.get(), curData, ((oldData, newData) -> {}));
            display.getChildren().set(1, currentEnvelope.getElement());
        };
        width.addListener(updateSize);
        height.addListener(updateSize);
        return display;
    }

    public EnvelopeData getEnvelopeData() {
        return currentEnvelope.getData();
    }
}
