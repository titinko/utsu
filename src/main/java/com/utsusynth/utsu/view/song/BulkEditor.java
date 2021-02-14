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
import com.utsusynth.utsu.view.song.note.pitch.PitchbendCallback;
import com.utsusynth.utsu.view.song.note.pitch.PitchbendFactory;
import com.utsusynth.utsu.view.song.note.pitch.Vibrato;
import com.utsusynth.utsu.view.song.note.pitch.portamento.Portamento;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.DoubleExpression;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.prefs.Preferences;

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

    private Group vibratoGroupUpper;
    private Group vibratoGroupLower;
    private Vibrato currentVibrato;
    private Runnable vibratoCallback;
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

    /** Call this when first initializing BulkEditor class from parent. */
    public void initialize(DoubleExpression width, DoubleExpression height) {
        editorWidth.bind(width);
        editorHeight.bind(height);
    }

    public Group createPortamentoEditor(PitchbendData portamentoData, List<FilterType> filters) {
        currentFilters = filters;
        double rowHeight = scaler.scaleY(Quantizer.ROW_HEIGHT).get();
        ListView<String> background =
                createPitchbendBackground(editorWidth, editorHeight, rowHeight);
        portamentoGroup = new Group(background);

        // Create notes and portamento curve.
        portamentoGroup.getChildren().add(createNotesAndPortamento(portamentoData));
        InvalidationListener updateSize = obs -> {
            if (!(editorWidth.get() > 0) || !(editorHeight.get() > 0)) {
                return;
            }
            portamentoGroup.getChildren().set(1, createNotesAndPortamento(getPortamentoData()));
        };
        editorWidth.addListener(updateSize);
        editorHeight.addListener(updateSize);
        return portamentoGroup;
    }

    private Group createNotesAndPortamento(PitchbendData portamentoData) {
        Group notesAndPortamento = new Group();
        int noteWidth = (int) Math.max(1, editorWidth.get() / 2);
        int numRows = (int) (editorHeight.get() / scaler.scaleY(Quantizer.ROW_HEIGHT).get());
        if (currentFilters.contains(FilterType.RISING_NOTE)) {
            Note first = noteFactory.createBackgroundNote(
                    numRows / 3 * 2, 0, noteWidth, scaler);
            Note second = noteFactory.createBackgroundNote(
                    numRows / 3, noteWidth, noteWidth, scaler);
            currentPortamento = pitchbendFactory.createPortamentoEditor(
                    editorWidth.get(),
                    editorHeight.get(),
                    second,
                    numRows / 3 * 2,
                    portamentoData,
                    scaler,
                    false);
            notesAndPortamento.getChildren().addAll(
                    first.getElement(), second.getElement(), currentPortamento.getElement());
        } else if (currentFilters.contains(FilterType.FALLING_NOTE)) {
            Note first = noteFactory.createBackgroundNote(
                    numRows / 3, 0, noteWidth, scaler);
            Note second = noteFactory.createBackgroundNote(
                    numRows / 3 * 2, noteWidth, noteWidth, scaler);
            currentPortamento = pitchbendFactory.createPortamentoEditor(
                    editorWidth.get(),
                    editorHeight.get(),
                    second,
                    numRows / 3,
                    portamentoData,
                    scaler,
                    false);
            notesAndPortamento.getChildren().addAll(
                    first.getElement(), second.getElement(), currentPortamento.getElement());
        } else {
            Note note = noteFactory.createBackgroundNote(
                    numRows / 2, noteWidth, noteWidth, scaler);
            currentPortamento = pitchbendFactory.createPortamentoEditor(
                    editorWidth.get(),
                    editorHeight.get(),
                    note,
                    numRows / 2,
                    portamentoData,
                    scaler,
                    false);
            notesAndPortamento.getChildren().addAll(
                    note.getElement(), currentPortamento.getElement());
        }
        return notesAndPortamento;
    }

    /** Mini portamento view to be used in portamento config list. */
    private Group createMiniNotesAndPortamento(
            PitchbendData portamentoData, double width, double height, double rowHeight) {
        double yScale = rowHeight / scaler.scaleY(Quantizer.ROW_HEIGHT).get();
        double xScale = (yScale + 1) / 2.0;
        Scaler miniScaler = scaler.derive(xScale, yScale);

        int noteWidth = (int) Math.max(1, width / 2);
        int numRows = (int) (height / miniScaler.scaleY(Quantizer.ROW_HEIGHT).get());

        Note note = noteFactory.createBackgroundNote(
                numRows / 2, noteWidth, noteWidth, miniScaler);
        Portamento newPortamento = pitchbendFactory.createPortamentoEditor(
                width,
                height,
                note,
                numRows / 2,
                portamentoData,
                miniScaler,
                true);
        return new Group(note.getElement(), newPortamento.getElement());
    }

    public PitchbendData getPortamentoData() {
        return currentPortamento.getData();
    }

    public ListView<PitchbendData> createPortamentoList(
            ObservableList<PitchbendData> portamentoData, DoubleExpression height) {
        Preferences portamentoPreferences =
                Preferences.userRoot().node("utsu/bulkEditor/portamento");
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
                        DoubleExpression width = new SimpleDoubleProperty(150);
                        DoubleProperty height = new SimpleDoubleProperty(30);
                        double rowHeight = 5;
                        ListView<String> background = createPitchbendBackground(
                                width, height, rowHeight);
                        Group notesAndPortamento = createMiniNotesAndPortamento(
                                item, width.get(), height.get(), rowHeight);

                        Group portamentoGroup = new Group(background, notesAndPortamento);
                        portamentoGroup.setMouseTransparent(true);
                        Button closeButton = new Button("X");
                        closeButton.setOnAction(event -> {
                            if (getIndex() != 0) {
                                getListView().getItems().remove(getIndex());
                                portamentoPreferences.putInt("listIndex", 0); // Reset cache.
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
                int index = listCell.getIndex();
                if (selectFromPortamentoList(index)) {
                    // Cache selected index.
                    portamentoPreferences.putInt("listIndex", listCell.getIndex());
                }
            });
            return listCell;
        });
        portamentoList.setItems(portamentoData);
        // Select cached index if possible.
        int cachedIndex = portamentoPreferences.getInt("listIndex", 0);
        if (selectFromPortamentoList(cachedIndex)) {
            portamentoList.getSelectionModel().select(cachedIndex);
        }
        return portamentoList;
    }

    public void saveToPortamentoList() {
        portamentoList.getItems().add(getPortamentoData());
    }

    // Returns whether the selection succeeded.
    private boolean selectFromPortamentoList(int index) {
        if (portamentoList == null || portamentoList.getItems().size() <= index) {
            return false;
        }
        PitchbendData newData = portamentoList.getItems().get(index);
        if (newData == null) {
            return false;
        }
        portamentoGroup.getChildren().set(1, createNotesAndPortamento(newData));
        return true;
    }

    public void setCurrentFilters(List<FilterType> filters) {
        currentFilters = filters;
        portamentoGroup.getChildren().set(1, createNotesAndPortamento(currentPortamento.getData()));
    }

    public VBox createVibratoEditor(PitchbendData vibratoData, Runnable vibratoCallback) {
        this.vibratoCallback = vibratoCallback;
        double rowHeight = scaler.scaleY(Quantizer.ROW_HEIGHT).get();
        ListView<String> backgroundUpper =
                createPitchbendBackground(editorWidth, editorHeight.divide(2), rowHeight);
        vibratoGroupUpper = new Group(backgroundUpper, createNoteAndVibrato(vibratoData));
        AnchorPane backgroundLower = new AnchorPane();
        backgroundLower.getStyleClass().add("vibrato-background");
        backgroundLower.prefWidthProperty().bind(editorWidth);
        backgroundLower.prefHeightProperty().bind(editorHeight.divide(2));
        vibratoGroupLower = new Group(backgroundLower, currentVibrato.getEditorElement());

        InvalidationListener updateSize = obs -> {
            if (!(editorWidth.get() > 0) || !(editorHeight.get() > 0)) {
                return;
            }
            vibratoGroupUpper.getChildren().set(1, createNoteAndVibrato(getVibratoData()));
            vibratoGroupLower.getChildren().set(1, currentVibrato.getEditorElement());
        };
        editorWidth.addListener(updateSize);
        editorHeight.addListener(updateSize);

        vibratoCallback.run(); // Initialize with starting values.
        return new VBox(vibratoGroupUpper, vibratoGroupLower);
    }

    private Group createNoteAndVibrato(PitchbendData vibratoData) {
        int noteStart = (int) Math.max(1, editorWidth.get() / 4);
        int numRows = (int) (editorHeight.get() * .5 / scaler.scaleY(Quantizer.ROW_HEIGHT).get());
        Note note = noteFactory.createBackgroundNote(
                numRows / 2, noteStart, editorWidth.get() - noteStart, scaler);
        currentVibrato = pitchbendFactory.createVibrato(
                note, vibratoData, new PitchbendCallback() {
                    @Override
                    public void modifySongPitchbend(PitchbendData oldData, PitchbendData newData) {
                        // Do nothing.
                    }

                    @Override
                    public void modifySongVibrato(int[] oldVibrato, int[] newVibrato) {
                        if (vibratoCallback != null) {
                            vibratoCallback.run();
                        }
                    }
                }, new SimpleBooleanProperty(true));
        return new Group(note.getElement(), currentVibrato.getVibratoElement());
    }

    private Group createMiniNoteAndVibrato(
            PitchbendData vibratoData, double width, double height, double rowHeight) {
        double yScale = rowHeight / scaler.scaleY(Quantizer.ROW_HEIGHT).get();
        double xScale = (yScale + 1) / 2.0;
        Scaler miniScaler = scaler.derive(xScale, yScale);

        int noteStart = (int) Math.max(1, width / 4);
        int numRows = (int) (height / miniScaler.scaleY(Quantizer.ROW_HEIGHT).get());

        Note note = noteFactory.createBackgroundNote(
                numRows / 2, noteStart, width - noteStart, miniScaler);
        Vibrato newVibrato = pitchbendFactory.createViewOnlyVibrato(note, vibratoData, miniScaler);
        return new Group(note.getElement(), newVibrato.getElement());
    }

    public void toggleVibrato(boolean hasVibrato) {
        if (currentVibrato == null) {
            return;
        }
        if (hasVibrato && currentVibrato.getVibrato().isEmpty()) {
            currentVibrato.addDefaultVibrato();
            vibratoCallback.run();
        } else if (!hasVibrato && currentVibrato.getVibrato().isPresent()) {
            currentVibrato.clearVibrato();
            vibratoCallback.run();
        }
    }

    public void setVibratoValue(int index, int newValue) {
        if (currentVibrato == null) {
            return;
        }
        currentVibrato.adjustVibrato(index, newValue);
        currentVibrato.redrawEditor();
    }

    public PitchbendData getVibratoData() {
        PitchbendData empty = new PitchbendData(
                ImmutableList.of(), ImmutableList.of(), ImmutableList.of(), ImmutableList.of());
        return empty.withVibrato(currentVibrato.getVibrato());
    }

    public ListView<PitchbendData> createVibratoList(
            ObservableList<PitchbendData> vibratoData, DoubleExpression height) {
        Preferences vibratoPreferences = Preferences.userRoot().node("/utsu/bulkEditor/vibrato");
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
                        DoubleExpression width = new SimpleDoubleProperty(150);
                        DoubleProperty height = new SimpleDoubleProperty(30);
                        double rowHeight = 5;
                        ListView<String> background = createPitchbendBackground(
                                width, height, rowHeight);
                        Group noteAndVibrato = createMiniNoteAndVibrato(
                                item, width.get(), height.get(), rowHeight);

                        Group vibratoGroup = new Group(background, noteAndVibrato);
                        vibratoGroup.setMouseTransparent(true);
                        Button closeButton = new Button("X");
                        closeButton.setOnAction(event -> {
                            if (getIndex() != 0) {
                                getListView().getItems().remove(getIndex());
                                vibratoPreferences.putInt("listIndex", 0); // Reset cache.
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
                // Cache selected index.
                int index = listCell.getIndex();
                if (selectFromVibratoList(index)) {
                    vibratoPreferences.putInt("listIndex", listCell.getIndex());
                }
            });
            return listCell;
        });
        vibratoList.setItems(vibratoData);
        // Select cached index if possible.
        int cachedIndex = vibratoPreferences.getInt("listIndex", 0);
        if (selectFromVibratoList(cachedIndex)) {
            vibratoList.getSelectionModel().select(cachedIndex);
        }
        return vibratoList;
    }

    public void saveToVibratoList() {
        vibratoList.getItems().add(getVibratoData());
    }

    // Returns whether the selection succeeded.
    private boolean selectFromVibratoList(int index) {
        if (vibratoList == null || vibratoList.getItems().size() <= index) {
            return false;
        }
        PitchbendData newData = vibratoList.getItems().get(index);
        if (newData == null) {
            return false;
        }
        PitchbendData clonedData = newData.withVibrato(
                Optional.of(Arrays.copyOf(newData.getVibrato(), newData.getVibrato().length)));
        vibratoGroupUpper.getChildren().set(1, createNoteAndVibrato(clonedData));
        vibratoGroupLower.getChildren().set(1, currentVibrato.getEditorElement());
        vibratoCallback.run();
        return true;
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

    public Group createEnvelopeEditor(EnvelopeData envelopeData) {
        VBox background = createEnvelopeBackground(editorWidth, editorHeight);
        envelopeGroup = new Group(background);

        currentEnvelope = envelopeFactory.createEnvelopeEditor(
                editorWidth.get(), editorHeight.get(), envelopeData, scaler,false);
        envelopeGroup = new Group(background, currentEnvelope.getElement());

        InvalidationListener updateSize = obs -> {
            EnvelopeData curData = currentEnvelope.getData();
            if (!(editorWidth.get() > 0) || !(editorHeight.get() > 0)) {
                return;
            }
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
        Preferences envelopePreferences = Preferences.userRoot().node("/utsu/bulkEditor/envelope");
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
                                envelopePreferences.putInt("listIndex", 0); // Reset cache.
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
                // Cache selected index.
                int index = listCell.getIndex();
                if (selectFromEnvelopeList(index)) {
                    envelopePreferences.putInt("listIndex", listCell.getIndex());
                }
            });
            return listCell;
        });
        envelopeList.setItems(envelopeData);
        // Select cached index if possible.
        int cachedIndex = envelopePreferences.getInt("listIndex", 0);
        if (selectFromEnvelopeList(cachedIndex)) {
            envelopeList.getSelectionModel().select(cachedIndex);
        }
        return envelopeList;
    }

    public void saveToEnvelopeList() {
        envelopeList.getItems().add(getEnvelopeData());
    }

    // Returns whether the selection succeeded.
    private boolean selectFromEnvelopeList(int index) {
        if (envelopeList == null || envelopeList.getItems().size() <= index) {
            return false;
        }
        EnvelopeData newData = envelopeList.getItems().get(index);
        if (newData == null) {
            return false;
        }
        currentEnvelope = envelopeFactory.createEnvelopeEditor(
                editorWidth.get(), editorHeight.get(), newData, scaler,false);
        envelopeGroup.getChildren().set(1, currentEnvelope.getElement());
        return true;
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
