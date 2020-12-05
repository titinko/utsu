package com.utsusynth.utsu.view.voicebank;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.utsusynth.utsu.common.data.LyricConfigData;
import com.utsusynth.utsu.common.data.LyricConfigData.FrqStatus;
import com.utsusynth.utsu.common.i18n.Localizable;
import com.utsusynth.utsu.common.i18n.Localizer;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.Property;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.text.Font;
import javafx.util.StringConverter;

import java.io.File;
import java.util.*;

public class VoicebankEditor implements Localizable {
    private final Localizer localizer;
    private final Map<String, TableView<LyricConfigData>> tables = new HashMap<>();

    private TabPane tabPane;
    private VoicebankCallback model;

    @Inject
    public VoicebankEditor(Localizer localizer) {
        this.localizer = localizer;
    }

    /**
     * Initialize editor with data from the controller.
     */
    public void initialize(VoicebankCallback callback) {
        this.model = callback;
    }

    public Region createNew(Set<String> categories) {
        clear();
        if (categories.isEmpty()) {
            return new ScrollPane();
        }

        if (categories.size() == 1) {
            String category = categories.iterator().next();
            ScrollPane table = newTable(category, model.getLyricData(category));
            localizer.localize(this);
            return table;
        }

        // Create tabs to open other categories.
        for (String category : categories) {
            Tab tab = new Tab(category);
            tab.setOnSelectionChanged(event -> {
                if (tab.isSelected()) {
                    // Generate new tab if not already present.
                    if (tab.getContent() == null) {
                        tab.setContent(newTable(category, model.getLyricData(category)));
                    }
                }
            });
            tabPane.getTabs().add(tab);
        }
        localizer.localize(this);
        return tabPane;
    }

    public void selectLyric(String category, String lyric) {
        // Navigate to appropriate tab if necessary.
        for (Tab tab : tabPane.getTabs()) {
            if (tab.getText().equals(category)) {
                tabPane.getSelectionModel().select(tab);
                break;
            }
        }
        // Highlight matching lyric if present.
        if (!tables.containsKey(category)) {
            return;
        }
        TableView<LyricConfigData> table = tables.get(category);
        table.getItems().forEach(lyricData -> {
            if (lyricData.getLyric().equals(lyric)) {
                table.getSelectionModel().select(lyricData);
                table.scrollTo(lyricData);
                model.displayLyric(lyricData);
            }
        });
    }

    private ScrollPane newTable(String category, Iterator<LyricConfigData> lyricIterator) {
        ScrollPane scrollPane = new ScrollPane();
        // TODO: Add css for this.
        scrollPane.setFitToHeight(true);
        scrollPane.setFitToWidth(true);
        scrollPane.setMinViewportHeight(300);
        scrollPane.setMinViewportWidth(300);
        HBox.setHgrow(scrollPane, Priority.ALWAYS);

        // Create table.
        ObservableList<LyricConfigData> lyrics = FXCollections.observableArrayList();
        TableView<LyricConfigData> table = new TableView<>(lyrics);
        tables.put(category, table);
        scrollPane.setContent(table);
        table.setEditable(true);

        // Add a context menu.
        table.setRowFactory(source -> {
            TableRow<LyricConfigData> row = new TableRow<>();
            ContextMenu contextMenu = new ContextMenu();
            MenuItem addAliasItem = new MenuItem("Add Alias");
            addAliasItem.setOnAction(event -> {
                LyricConfigData newData = row.getItem().deepCopy();
                int indexToAdd = row.getIndex() + 1;
                lyrics.add(indexToAdd, newData);
                newData.lyricProperty().set("?");
                initializeLyric(newData);
                // Set up undo/redo.
                model.recordAction(
                        () -> lyrics.add(indexToAdd, newData),
                        () -> lyrics.remove(newData));
            });
            MenuItem genFrqItem = new MenuItem("Generate .frq File");
            genFrqItem.setOnAction(event -> {
                File wavFile = row.getItem().getPathToFile();
                model.generateFrqFiles(
                        lyrics.filtered(data -> data.getPathToFile().equals(wavFile)).iterator());
            });
            MenuItem deleteItem = new MenuItem("Delete");
            deleteItem.setOnAction(event -> {
                LyricConfigData toRemove = row.getItem();
                int index = row.getIndex();
                table.getItems().remove(toRemove);
                model.removeLyric(toRemove.getLyric());
                // Set up undo/redo.
                model.recordAction(() -> {
                    table.getItems().remove(toRemove);
                    model.removeLyric(toRemove.getLyric());
                }, () -> {
                    table.getItems().add(index, toRemove);
                    model.addLyric(toRemove);
                });
            });
            contextMenu.getItems().addAll(addAliasItem, genFrqItem, deleteItem);
            Runnable localizeRowContextMenu = () -> {
                addAliasItem.setText(localizer.getMessage("voice.addAlias"));
                genFrqItem.setText(localizer.getMessage("voice.generateFrqFile"));
                deleteItem.setText(localizer.getMessage("menu.edit.delete"));
            };
            row.setOnContextMenuRequested(event -> {
                // Don't show context menu for empty rows.
                if (row.getItem() != null) {
                    localizeRowContextMenu.run();
                    contextMenu.show(row, event.getScreenX(), event.getScreenY());
                }
            });
            row.setOnMousePressed(event -> {
                contextMenu.hide();
            });
            row.setOnMouseClicked(event -> {
                if (event.getButton().equals(MouseButton.PRIMARY)) {
                    model.displayLyric(row.getItem());
                }
            });
            return row;
        });

        // Add columns.
        TableColumn<LyricConfigData, String> lyricCol = createLyricCol();
        TableColumn<LyricConfigData, String> fileCol = new TableColumn<>("File");
        fileCol.setCellValueFactory(data -> data.getValue().fileNameProperty());
        TableColumn<LyricConfigData, String> frqStatusCol = createFrqStatusCol(lyrics);
        TableColumn<LyricConfigData, Double> offsetCol =
                createNumberCol("Offset", LyricConfigData::offsetProperty);
        TableColumn<LyricConfigData, Double> consonantCol =
                createNumberCol("Consonant", LyricConfigData::consonantProperty);
        TableColumn<LyricConfigData, Double> cutoffCol =
                createNumberCol("Cutoff", LyricConfigData::cutoffProperty);
        TableColumn<LyricConfigData, Double> preutterCol =
                createNumberCol("Preutter", LyricConfigData::preutterProperty);
        TableColumn<LyricConfigData, Double> overlapCol =
                createNumberCol("Overlap", LyricConfigData::overlapProperty);
        table.getColumns().setAll(
                ImmutableList.of(
                        lyricCol,
                        fileCol,
                        frqStatusCol,
                        offsetCol,
                        consonantCol,
                        cutoffCol,
                        preutterCol,
                        overlapCol));

        // Populate with lyrics.
        while (lyricIterator.hasNext()) {
            LyricConfigData data = lyricIterator.next();
            lyrics.add(data);
            initializeLyric(data);
        }
        return scrollPane;
    }

    private TableColumn<LyricConfigData, String> createLyricCol() {
        TableColumn<LyricConfigData, String> lyricCol = new TableColumn<>("Lyric");
        lyricCol.setCellValueFactory(data -> data.getValue().lyricProperty());
        lyricCol.setCellFactory(col -> new EditableCell<>(stringToString));
        lyricCol.setOnEditCommit(event -> {
            final LyricConfigData config = event.getRowValue();
            final String oldLyric = config.lyricProperty().get();
            final String newLyric = event.getNewValue();
            if (!oldLyric.equals(newLyric)) {
                model.recordAction(
                        () -> config.lyricProperty().set(newLyric),
                        () -> config.lyricProperty().set(oldLyric));
                config.lyricProperty().set(newLyric);
            }
        });
        return lyricCol;
    }

    private TableColumn<LyricConfigData, String> createFrqStatusCol(
            ObservableList<LyricConfigData> lyrics) {
        TableColumn<LyricConfigData, String> col = new TableColumn<>("Frq");
        col.setCellValueFactory(data -> data.getValue().frqStatusProperty());
        col.setCellFactory(source -> new FrqStatusCell());
        col.setSortable(false);
        ContextMenu contextMenu = new ContextMenu();
        MenuItem generateAllFrqItem = new MenuItem("Generate Missing .frq Files");
        generateAllFrqItem.setOnAction(event -> {
            model.generateFrqFiles(
                    lyrics.filtered(
                            data -> !data.frqStatusProperty().get()
                                    .equals(FrqStatus.VALID.toString()))
                            .iterator());
        });
        MenuItem regenerateAllFrqItem = new MenuItem("Replace all .frq Files");
        regenerateAllFrqItem.setOnAction(event -> model.generateFrqFiles(lyrics.iterator()));
        contextMenu.getItems().addAll(generateAllFrqItem, regenerateAllFrqItem);
        contextMenu.setOnShowing(event -> {
            generateAllFrqItem.setText(localizer.getMessage("voice.generateMissingFrqFiles"));
            regenerateAllFrqItem.setText(localizer.getMessage("voice.regenerateFrqFiles"));
        });
        col.setContextMenu(contextMenu);
        return col;
    }

    private TableColumn<LyricConfigData, Double> createNumberCol(
            String title,
            Function<LyricConfigData, DoubleProperty> property) {
        TableColumn<LyricConfigData, Double> col = new TableColumn<>(title);
        col.setCellValueFactory(data -> property.apply(data.getValue()).asObject());
        col.setCellFactory(source -> new EditableCell<>(stringToDouble));
        col.setSortable(false);
        col.setOnEditCommit(event -> {
            final LyricConfigData config = event.getRowValue();
            final double oldValue = property.apply(config).get();
            final double newValue = event.getNewValue();
            model.recordAction(() -> {
                property.apply(config).set(newValue);
                model.displayLyric(config);
            }, () -> {
                property.apply(config).set(oldValue);
                model.displayLyric(config);
            });
            property.apply(config).set(newValue);
            model.displayLyric(config); // Re-create config editor if necessary.
        });
        return col;
    }

    private void initializeLyric(LyricConfigData data) {
        data.lyricProperty().addListener((observable, oldLyric, newLyric) -> {
            // Do nothing if lyric doesn't change.
            if (oldLyric.equals(newLyric)) {
                return;
            }
            // Remove old lyric.
            model.removeLyric(oldLyric);
            if (newLyric.equals("?")) {
                return; // Don't try to modify model if new lyric is a question mark.
            }
            // Immediately revert change if new lyric would be a repeat.
            if (!model.addLyric(data)) {
                data.lyricProperty().set(oldLyric);
                model.addLyric(data);
            }
        });
        for (Property<?> property : data.mutableProperties()) {
            property.addListener(event -> {
                // Mutate lyric with new data.
                model.modifyLyric(data);
            });
        }
    }

    private void clear() {
        // Remove current lyric configs.
        tables.clear();
        tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
        HBox.setHgrow(tabPane, Priority.ALWAYS);
    }

    @Override
    public void localize(ResourceBundle bundle) {
        for (TableView<LyricConfigData> table : tables.values()) {
            table.getColumns().get(0).setText(bundle.getString("voice.lyric"));
            table.getColumns().get(1).setText(bundle.getString("voice.lyricFile"));
            table.getColumns().get(2).setText(bundle.getString("voice.frq"));
            table.getColumns().get(3).setText(bundle.getString("voice.offset"));
            table.getColumns().get(4).setText(bundle.getString("voice.consonant"));
            table.getColumns().get(5).setText(bundle.getString("voice.cutoff"));
            table.getColumns().get(6).setText(bundle.getString("voice.preutteranceShort"));
            table.getColumns().get(7).setText(bundle.getString("voice.overlap"));
        }
    }

    private class FrqStatusCell extends TableCell<LyricConfigData, String> {
        @Override
        public void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                getStyleClass().clear();
            } else {
                FrqStatus status = null;
                try {
                    status = FrqStatus.valueOf(item);
                } catch (Exception e) {
                    setText(null);
                    getStyleClass().clear();
                }
                switch (status) {
                    case INVALID:
                        setText("N");
                        getStyleClass().setAll("frq-cell", "invalid");
                        break;
                    case LOADING:
                        setText("L");
                        getStyleClass().setAll("frq-cell", "loading");
                        break;
                    case VALID:
                        setText("Y");
                        getStyleClass().setAll("frq-cell", "valid");
                        break;
                }
            }
        }
    }

    private class EditableCell<T> extends TableCell<LyricConfigData, T> {
        private final TextField textField;
        private final StringConverter<T> converter;

        private EditableCell(StringConverter<T> converter) {
            this.converter = converter;
            textField = new TextField();
            textField.setFont(Font.font(9));
            textField.setOnAction(event -> commitEdit(converter.fromString(textField.getText())));
            textField.focusedProperty().addListener((observable, wasFocused, focused) -> {
                if (wasFocused && !focused) {
                    cancelEdit();
                }
            });
        }

        @Override
        public void startEdit() {
            if (!isEmpty()) {
                super.startEdit();
                setText(null);
                setGraphic(textField);
                textField.setText(converter.toString(getItem()));
                textField.requestFocus();
            }
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();
            setText(converter.toString(getItem()));
            setGraphic(null);
        }

        @Override
        public void updateItem(T item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                if (isEditing()) {
                    textField.setText(converter.toString(getItem()));
                    setText(null);
                    setGraphic(textField);
                } else {
                    setText(converter.toString(getItem()));
                    setGraphic(null);
                }
            }
        }
    }

    private final StringConverter<String> stringToString = new StringConverter<String>() {
        @Override
        public String toString(String object) {
            return object == null ? "" : object;
        }

        @Override
        public String fromString(String string) {
            return string == null ? "" : string;
        }
    };

    private final StringConverter<Double> stringToDouble = new StringConverter<Double>() {
        @Override
        public String toString(Double object) {
            return object == null ? "" : String.valueOf(object);
        }

        @Override
        public Double fromString(String string) {
            try {
                return Double.parseDouble(string);
            } catch (Exception e) {
                return 0.0;
            }
        }
    };
}
