package com.utsusynth.utsu.view.song;

import com.google.inject.Inject;
import com.utsusynth.utsu.common.data.EnvelopeData;
import com.utsusynth.utsu.view.song.note.envelope.Envelope;
import com.utsusynth.utsu.view.song.note.envelope.EnvelopeFactory;
import com.utsusynth.utsu.view.song.note.pitch.PitchbendFactory;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.DoubleExpression;
import javafx.beans.property.SimpleDoubleProperty;
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
            EnvelopeData envelopeData, DoubleExpression width, DoubleExpression height) {
        VBox background = createEnvelopeBackground(width, height);

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
        envelopeList.setCellFactory(source -> new ListCell<>() {
                @Override
                protected void updateItem(EnvelopeData item, boolean empty) {
                    super.updateItem(item, empty);

                    setText(null);
                    if (empty || item == null) {
                        setGraphic(null);
                    } else {
                        HBox graphic = new HBox(5);
                        VBox background = createEnvelopeBackground(
                                new SimpleDoubleProperty(150), new SimpleDoubleProperty(30));
                        // TODO: Scale envelope to fit.
                        Envelope envelope = envelopeFactory.createEnvelopeEditor(
                                150, 30, item, ((oldData, newData) -> {}));
                        Group envelopeGroup = new Group(background, envelope.getElement());
                        envelopeGroup.setMouseTransparent(true);
                        Button closeButton = new Button("X");
                        if (getIndex() == 0) {
                            closeButton.setDisable(true); // Can't remove default option.
                        }
                        // TODO: Make X button remove envelope.
                        graphic.getChildren().addAll(envelopeGroup, closeButton);
                        setGraphic(graphic);
                    }
                }
            });
        envelopeList.setItems(envelopes);
        return envelopeList;
    }

    private VBox createEnvelopeBackground(DoubleExpression width, DoubleExpression height) {
        AnchorPane topCell = new AnchorPane();
        topCell.getStyleClass().add("dynamics-top-cell");
        topCell.prefWidthProperty().bind(width);
        topCell.prefHeightProperty().bind(height.divide(2.0));
        AnchorPane bottomCell = new AnchorPane();
        bottomCell.getStyleClass().add("dynamics-bottom-cell");
        bottomCell.prefWidthProperty().bind(width);
        bottomCell.prefHeightProperty().bind(height.divide(2.0));
        return new VBox(topCell, bottomCell);
    }
}
