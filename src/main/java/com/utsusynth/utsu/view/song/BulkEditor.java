package com.utsusynth.utsu.view.song;

import com.google.inject.Inject;
import com.utsusynth.utsu.common.data.EnvelopeData;
import com.utsusynth.utsu.view.song.note.envelope.Envelope;
import com.utsusynth.utsu.view.song.note.envelope.EnvelopeFactory;
import com.utsusynth.utsu.view.song.note.pitch.PitchbendFactory;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.DoubleBinding;
import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class BulkEditor {
    private final EnvelopeFactory envelopeFactory;

    private Envelope currentEnvelope;
    private ListView<EnvelopeData> envelopeList;

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

    public ListView<EnvelopeData> createEnvelopeList(ObservableList<EnvelopeData> envelopes) {
        envelopeList = new ListView<>();
        envelopeList.setPrefHeight(250);
        envelopeList.setCellFactory(source -> {
            return new ListCell<>() {
                @Override
                protected void updateItem(EnvelopeData item, boolean empty) {
                    super.updateItem(item, empty);

                    setText(null);
                    if (empty || item == null) {
                        setGraphic(null);
                    } else {
                        HBox graphic = new HBox(5);
                        Envelope envelope = envelopeFactory.createEnvelopeEditor(
                                150, 30, item, ((oldData, newData) -> {}));
                        Button closeButton = new Button("X");
                        graphic.getChildren().addAll(envelope.getElement(), closeButton);
                        setGraphic(graphic);
                    }
                }
            };
        });
        envelopeList.setItems(envelopes);
        return envelopeList;
    }
}
