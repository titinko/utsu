package com.utsusynth.utsu.view.song;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.utsusynth.utsu.common.data.EnvelopeData;
import com.utsusynth.utsu.common.data.PitchbendData;
import com.utsusynth.utsu.common.quantize.Quantizer;
import com.utsusynth.utsu.common.quantize.Scaler;
import com.utsusynth.utsu.view.song.note.envelope.Envelope;
import com.utsusynth.utsu.view.song.note.envelope.EnvelopeFactory;
import com.utsusynth.utsu.view.song.note.pitch.PitchbendFactory;
import com.utsusynth.utsu.view.song.note.pitch.Vibrato;
import com.utsusynth.utsu.view.song.note.pitch.portamento.Portamento;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.DoubleExpression;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class BulkEditor {
    private final PitchbendFactory pitchbendFactory;
    private final EnvelopeFactory envelopeFactory;
    private final Scaler scaler;
    private final DoubleProperty editorWidth;
    private final DoubleProperty editorHeight;

    private Group portamentoGroup;
    private Portamento currentPortamento;
    private ListView<PitchbendData> portamentoList;

    private Group vibratoGroup;
    private Vibrato currentVibrato;
    private ListView<PitchbendData> vibratoList;

    private Group envelopeGroup;
    private Envelope currentEnvelope;
    private ListView<EnvelopeData> envelopeList;

    @Inject
    public BulkEditor(
            PitchbendFactory pitchbendFactory, EnvelopeFactory envelopeFactory, Scaler scaler) {
        this.pitchbendFactory = pitchbendFactory;
        this.envelopeFactory = envelopeFactory;
        this.scaler = scaler;
        editorWidth = new SimpleDoubleProperty(0);
        editorHeight = new SimpleDoubleProperty(0);
    }

    public Group createPortamentoEditor(
            PitchbendData portamentoData, DoubleExpression width, DoubleExpression height) {
        editorWidth.bind(width);
        editorHeight.bind(height);
        double rowHeight = scaler.scaleY(Quantizer.ROW_HEIGHT).get();
        ListView<String> background =
                createPitchbendBackground(editorWidth, editorHeight, rowHeight);
        portamentoGroup = new Group(background);
        return portamentoGroup;
    }

    public PitchbendData getPortamentoData() {
        return currentPortamento.getData();
    }

    public ListView<PitchbendData> createPortamentoList(
            ObservableList<PitchbendData> portamentoData, DoubleExpression height) {
        portamentoList = new ListView<>();
        portamentoList.prefHeightProperty().bind(height);
        portamentoList.setPrefWidth(220);
        portamentoList.setCellFactory(source -> {
            ListCell<PitchbendData> listCell = new ListCell<>() {
                @Override
                protected void updateItem(PitchbendData item, boolean empty) {
                    super.updateItem(item, empty);

                    setText(null);
                    if (empty || item == null) {
                        setGraphic(null);
                    } else {
                        HBox graphic = new HBox(5);
                        ListView<String> background = createPitchbendBackground(
                                new SimpleDoubleProperty(150),
                                new SimpleDoubleProperty(30),
                                5);
                        // TODO: Draw pitchbend.
                        Group portamentoGroup = new Group(background);
                        portamentoGroup.setMouseTransparent(true);
                        Button closeButton = new Button("X");
                        closeButton.setOnAction(event -> {
                            if (getIndex() != 0) {
                                getListView().getItems().remove(getIndex());
                            }
                        });
                        if (getIndex() == 0) {
                            closeButton.setDisable(true); // Can't remove default option.
                        }
                        graphic.getChildren().addAll(portamentoGroup, closeButton);
                        setGraphic(graphic);
                    }
                }
            };
            listCell.setOnMouseClicked(event -> {
                PitchbendData curData = listCell.getItem();
                if (curData == null) {
                    return;
                }
                // Populate group with current data.
                // portamentoGroup.getChildren().set(1, ...);
            });
            return listCell;
        });
        portamentoList.setItems(portamentoData);
        return portamentoList;
    }

    public void saveToPortamentoList() {
        portamentoList.getItems().add(getPortamentoData());
    }

    public Group createVibratoEditor(
            PitchbendData vibratoData, DoubleExpression width, DoubleExpression height) {
        editorWidth.bind(width);
        editorHeight.bind(height);
        double rowHeight = scaler.scaleY(Quantizer.ROW_HEIGHT).get();
        ListView<String> background =
                createPitchbendBackground(editorWidth, editorHeight.divide(2), rowHeight);
        vibratoGroup = new Group(background);
        return vibratoGroup;
    }

    public PitchbendData getVibratoData() {
        PitchbendData empty = new PitchbendData(
                ImmutableList.of(), ImmutableList.of(), ImmutableList.of(), ImmutableList.of());
        return empty.withVibrato(currentVibrato.getVibrato());
    }

    public ListView<PitchbendData> createVibratoList(
            ObservableList<PitchbendData> vibratoData, DoubleExpression height) {
        vibratoList = new ListView<>();
        vibratoList.prefHeightProperty().bind(height);
        vibratoList.setPrefWidth(220);
        vibratoList.setCellFactory(source -> {
            ListCell<PitchbendData> listCell = new ListCell<>() {
                @Override
                protected void updateItem(PitchbendData item, boolean empty) {
                    super.updateItem(item, empty);

                    setText(null);
                    if (empty || item == null) {
                        setGraphic(null);
                    } else {
                        HBox graphic = new HBox(5);
                        ListView<String> background = createPitchbendBackground(
                                new SimpleDoubleProperty(150),
                                new SimpleDoubleProperty(30),
                                5);
                        // TODO: Add vibrato visualization.
                        Group vibratoGroup = new Group(background);
                        vibratoGroup.setMouseTransparent(true);
                        Button closeButton = new Button("X");
                        closeButton.setOnAction(event -> {
                            if (getIndex() != 0) {
                                getListView().getItems().remove(getIndex());
                            }
                        });
                        if (getIndex() == 0) {
                            closeButton.setDisable(true); // Can't remove default option.
                        }
                        graphic.getChildren().addAll(vibratoGroup, closeButton);
                        setGraphic(graphic);
                    }
                }
            };
            listCell.setOnMouseClicked(event -> {
                PitchbendData curData = listCell.getItem();
                if (curData == null) {
                    return;
                }
                // Populate group with current data.
                // vibratoGroup.getChildren().set(1, ...);
            });
            return listCell;
        });
        vibratoList.setItems(vibratoData);
        return vibratoList;
    }

    public void saveToVibratoList() {
        vibratoList.getItems().add(getVibratoData());
    }

    private ListView<String> createPitchbendBackground(
            DoubleExpression width, DoubleExpression height, double rowHeight) {
        ListView<String> background = new ListView<>();
        background.prefWidthProperty().bind(width);
        background.prefHeightProperty().bind(height);
        background.setCellFactory(source -> new TextFieldListCell<>());
        background.setFixedCellSize(rowHeight);
        background.setMouseTransparent(true);
        background.setFocusTraversable(false);
        background.setItems(FXCollections.observableArrayList("")); // Force generation.
        return background;
    }

    public Group createEnvelopeEditor(
            EnvelopeData envelopeData, DoubleExpression width, DoubleExpression height) {
        editorWidth.bind(width);
        editorHeight.bind(height);
        VBox background = createEnvelopeBackground(editorWidth, editorHeight);

        currentEnvelope = envelopeFactory.createEnvelopeEditor(
                editorWidth.get(),
                editorHeight.get(),
                envelopeData,
                ((oldData, newData) -> {}),
                false);
        envelopeGroup = new Group(background, currentEnvelope.getElement());

        InvalidationListener updateSize = obs -> {
            EnvelopeData curData = currentEnvelope.getData();
            currentEnvelope = envelopeFactory.createEnvelopeEditor(
                    editorWidth.get(),
                    editorHeight.get(),
                    curData,
                    ((oldData, newData) -> {}),
                    false);
            envelopeGroup.getChildren().set(1, currentEnvelope.getElement());
        };
        editorWidth.addListener(updateSize);
        editorHeight.addListener(updateSize);
        return envelopeGroup;
    }

    public EnvelopeData getEnvelopeData() {
        return currentEnvelope.getData();
    }

    public ListView<EnvelopeData> createEnvelopeList(
            ObservableList<EnvelopeData> envelopeData, DoubleExpression height) {
        envelopeList = new ListView<>();
        envelopeList.prefHeightProperty().bind(height);
        envelopeList.setPrefWidth(220);
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
                        Envelope envelope = envelopeFactory.createEnvelopeEditor(
                                150,
                                30,
                                item,
                                ((oldData, newData) -> {}),
                                true);
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
                        editorWidth.get(),
                        editorHeight.get(),
                        curData,
                        ((oldData, newData) -> {}),
                        true);
                envelopeGroup.getChildren().set(1, currentEnvelope.getElement());
            });
            return listCell;
        });
        envelopeList.setItems(envelopeData);
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
