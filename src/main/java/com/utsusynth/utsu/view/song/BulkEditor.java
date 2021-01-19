package com.utsusynth.utsu.view.song;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.utsusynth.utsu.common.data.EnvelopeData;
import com.utsusynth.utsu.common.data.PitchbendData;
import com.utsusynth.utsu.common.enums.FilterType;
import com.utsusynth.utsu.common.quantize.Quantizer;
import com.utsusynth.utsu.common.quantize.Scaler;
import com.utsusynth.utsu.view.song.note.Note;
import com.utsusynth.utsu.view.song.note.NoteFactory;
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
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

public class BulkEditor {
    private final NoteFactory noteFactory;
    private final PitchbendFactory pitchbendFactory;
    private final EnvelopeFactory envelopeFactory;
    private final Scaler scaler;
    private final DoubleProperty editorWidth;
    private final DoubleProperty editorHeight;

    private Group portamentoGroup;
    private Portamento currentPortamento;
    private List<FilterType> currentFilters;
    private ListView<PitchbendData> portamentoList;

    private Group vibratoGroup;
    private Vibrato currentVibrato;
    private ListView<PitchbendData> vibratoList;

    private Group envelopeGroup;
    private Envelope currentEnvelope;
    private ListView<EnvelopeData> envelopeList;

    @Inject
    public BulkEditor(
            NoteFactory noteFactory,
            PitchbendFactory pitchbendFactory,
            EnvelopeFactory envelopeFactory,
            Scaler scaler) {
        this.noteFactory = noteFactory;
        this.pitchbendFactory = pitchbendFactory;
        this.envelopeFactory = envelopeFactory;
        this.scaler = scaler;
        editorWidth = new SimpleDoubleProperty(0);
        editorHeight = new SimpleDoubleProperty(0);
        currentFilters = ImmutableList.of();
    }

    public Group createPortamentoEditor(
            PitchbendData portamentoData,
            List<FilterType> filters,
            DoubleExpression width,
            DoubleExpression height) {
        editorWidth.bind(width);
        editorHeight.bind(height);
        currentFilters = filters;
        double rowHeight = scaler.scaleY(Quantizer.ROW_HEIGHT).get();
        ListView<String> background =
                createPitchbendBackground(editorWidth, editorHeight, rowHeight);
        portamentoGroup = new Group(background);

        // Create notes and portamento curve.
        portamentoGroup.getChildren().add(createNotesAndPortamento(portamentoData));
        InvalidationListener updateSize = obs -> {
            PitchbendData currentData = currentPortamento.getData();
            portamentoGroup.getChildren().set(1, createNotesAndPortamento(currentData));
        };
        editorWidth.addListener(updateSize);
        editorHeight.addListener(updateSize);
        return portamentoGroup;
    }

    private Group createNotesAndPortamento(PitchbendData portamentoData) {
        return createNotesAndPortamento(
                portamentoData, editorWidth, editorHeight, currentFilters, scaler, true);
    }

    private Group createNotesAndPortamento(
            PitchbendData portamentoData,
            DoubleExpression width,
            DoubleExpression height,
            List<FilterType> filters,
            Scaler noteScaler,
            boolean mainEditor) {
        Group notesAndPortamento = new Group();
        Portamento newPortamento;
        int noteWidth = (int) Math.max(1, width.get() / 2);
        int numRows = (int) (height.get() / noteScaler.scaleY(Quantizer.ROW_HEIGHT).get());
        if (filters.contains(FilterType.RISING_NOTE)) {
            Note first = noteFactory.createBackgroundNote(
                    numRows / 3 * 2, 0, noteWidth, noteScaler);
            Note second = noteFactory.createBackgroundNote(
                    numRows / 3, noteWidth, noteWidth, noteScaler);
            newPortamento = pitchbendFactory.createPortamentoEditor(
                    width.get(),
                    height.get(),
                    second,
                    numRows / 3 * 2,
                    portamentoData,
                    noteScaler,
                    !mainEditor);
            notesAndPortamento.getChildren().addAll(
                    first.getElement(), second.getElement(), newPortamento.getElement());
        } else if (filters.contains(FilterType.FALLING_NOTE)) {
            Note first = noteFactory.createBackgroundNote(
                    numRows / 3, 0, noteWidth, noteScaler);
            Note second = noteFactory.createBackgroundNote(
                    numRows / 3 * 2, noteWidth, noteWidth, noteScaler);
            newPortamento = pitchbendFactory.createPortamentoEditor(
                    width.get(),
                    height.get(),
                    second,
                    numRows / 3,
                    portamentoData,
                    noteScaler,
                    !mainEditor);
            notesAndPortamento.getChildren().addAll(
                    first.getElement(), second.getElement(), newPortamento.getElement());
        } else {
            Note note = noteFactory.createBackgroundNote(
                    numRows / 2, noteWidth, noteWidth, noteScaler);
            newPortamento = pitchbendFactory.createPortamentoEditor(
                    width.get(),
                    height.get(),
                    note,
                    numRows / 2,
                    portamentoData,
                    noteScaler,
                    !mainEditor);
            notesAndPortamento.getChildren().addAll(note.getElement(), newPortamento.getElement());
        }
        if (mainEditor) {
            currentPortamento = newPortamento;
        }
        return notesAndPortamento;
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
                        double rowHeight = 5;
                        DoubleExpression width = new SimpleDoubleProperty(150);
                        DoubleProperty height = new SimpleDoubleProperty(30);
                        ListView<String> background = createPitchbendBackground(
                                width, height, rowHeight);
                        double yScale = rowHeight / scaler.scaleY(Quantizer.ROW_HEIGHT).get();
                        double xScale = (yScale + 1) / 2.0;
                        Scaler miniScaler = scaler.derive(xScale, yScale);
                        Group notesAndPortamento = createNotesAndPortamento(
                                item, width, height, ImmutableList.of(), miniScaler, false);

                        Group portamentoGroup = new Group(background, notesAndPortamento);
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
                portamentoGroup.getChildren().set(1, createNotesAndPortamento(curData));
            });
            return listCell;
        });
        portamentoList.setItems(portamentoData);
        return portamentoList;
    }

    public void saveToPortamentoList() {
        portamentoList.getItems().add(getPortamentoData());
    }

    public void setCurrentFilters(List<FilterType> filters) {
        currentFilters = filters;
        portamentoGroup.getChildren().set(1, createNotesAndPortamento(currentPortamento.getData()));
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
                editorWidth.get(), editorHeight.get(), envelopeData, scaler,false);
        envelopeGroup = new Group(background, currentEnvelope.getElement());

        InvalidationListener updateSize = obs -> {
            EnvelopeData curData = currentEnvelope.getData();
            currentEnvelope = envelopeFactory.createEnvelopeEditor(
                    editorWidth.get(), editorHeight.get(), curData, scaler,false);
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
                        DoubleExpression width = new SimpleDoubleProperty(150);
                        DoubleProperty height = new SimpleDoubleProperty(30);
                        Scaler miniScaler = scaler.derive(0.5, 1);
                        VBox background = createEnvelopeBackground(width, height);
                        Envelope envelope = envelopeFactory.createEnvelopeEditor(
                                width.get(), height.get(), item, miniScaler, true);
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
                        editorWidth.get(), editorHeight.get(), curData, scaler,true);
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
