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

    private DoubleExpression editorWidth;
    private DoubleExpression editorHeight;
    private Group editorGroup;
    private Envelope currentEnvelope;
    private ListView<EnvelopeData> envelopeList;

    @Inject
    public BulkEditor(EnvelopeFactory envelopeFactory, PitchbendFactory pitchbendFactory) {
        this.envelopeFactory = envelopeFactory;
    }

    public Group createEnvelopeEditor(
            EnvelopeData envelopeData, DoubleExpression width, DoubleExpression height) {
        editorWidth = width;
        editorHeight = height;
        VBox background = createEnvelopeBackground(editorWidth, editorHeight);

        currentEnvelope = envelopeFactory.createEnvelopeEditor(
                editorWidth.get(), editorHeight.get(), envelopeData, ((oldData, newData) -> {}));
        editorGroup = new Group(background, currentEnvelope.getElement());

        InvalidationListener updateSize = obs -> {
            EnvelopeData curData = currentEnvelope.getData();
            currentEnvelope = envelopeFactory.createEnvelopeEditor(
                    editorWidth.get(), editorHeight.get(), curData, ((oldData, newData) -> {}));
            editorGroup.getChildren().set(1, currentEnvelope.getElement());
        };
        editorWidth.addListener(updateSize);
        editorHeight.addListener(updateSize);
        return editorGroup;
    }

    public EnvelopeData getEnvelopeData() {
        return currentEnvelope.getData();
    }

    public ListView<EnvelopeData> createEnvelopeList(ObservableList<EnvelopeData> envelopes) {
        envelopeList = new ListView<>();
        envelopeList.setPrefHeight(250);
        envelopeList.setCellFactory(source -> {
            ListCell<EnvelopeData> listCell = new ListCell<>() {
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
                        closeButton.setOnAction(event -> {
                            if (getIndex() != 0) {
                                getListView().getItems().remove(getIndex());
                            }
                        });
                        if (getIndex() == 0) {
                            closeButton.setDisable(true); // Can't remove default option.
                        }
                        graphic.getChildren().addAll(envelopeGroup, closeButton);
                        setGraphic(graphic);
                    }
                }
            };
            listCell.setOnMouseClicked(event -> {
                EnvelopeData curData = listCell.getItem();
                if (curData == null) {
                    return;
                }
                currentEnvelope = envelopeFactory.createEnvelopeEditor(
                        editorWidth.get(), editorHeight.get(), curData, ((oldData, newData) -> {}));
                editorGroup.getChildren().set(1, currentEnvelope.getElement());
            });
            return listCell;
        });
        envelopeList.setItems(envelopes);
        return envelopeList;
    }

    public void saveToEnvelopeList() {
        envelopeList.getItems().add(getEnvelopeData());
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
