package com.utsusynth.utsu.view.voicebank;

import java.util.Iterator;
import java.util.Set;
import com.google.common.collect.ImmutableList;
import com.utsusynth.utsu.common.data.LyricConfigData;
import javafx.beans.property.Property;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.text.Font;
import javafx.util.StringConverter;

public class VoicebankEditor {
    private TabPane tabPane;
    private VoicebankCallback model;

    /** Initialize editor with data from the controller. */
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
            return newTable(model.getLyricData(category));
        }

        // Create tabs to open other categories.
        for (String category : categories) {
            Tab tab = new Tab(category);
            tab.setOnSelectionChanged(event -> {
                if (tab.isSelected()) {
                    if (tab.getContent() == null) {
                        tab.setContent(newTable(model.getLyricData(category)));
                    }
                }
            });
            tabPane.getTabs().add(tab);
        }
        return tabPane;
    }

    private ScrollPane newTable(Iterator<LyricConfigData> lyricIterator) {
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
        scrollPane.setContent(table);
        table.setEditable(true);

        // Add a context menu.
        table.setRowFactory(source -> {
            TableRow<LyricConfigData> row = new TableRow<>();
            ContextMenu contextMenu = new ContextMenu();
            MenuItem addAliasItem = new MenuItem("Add Alias");
            addAliasItem.setOnAction(event -> {
                // blah
            });
            MenuItem deleteItem = new MenuItem("Delete");
            deleteItem.setOnAction(event -> {
                table.getItems().remove(row.getItem());
            });
            contextMenu.getItems().addAll(addAliasItem, deleteItem);
            row.setOnContextMenuRequested(event -> {
                // Don't show context menu for empty rows.
                if (row.getItem() != null) {
                    contextMenu.hide();
                    contextMenu.show(row, event.getScreenX(), event.getScreenY());
                }
            });
            return row;
        });

        // Add columns.
        TableColumn<LyricConfigData, String> lyricCol = new TableColumn<>("Lyric");
        lyricCol.setCellValueFactory(data -> data.getValue().lyricProperty());
        lyricCol.setCellFactory(col -> new EditableCell<>(stringToString));
        TableColumn<LyricConfigData, String> fileCol = new TableColumn<>("File");
        fileCol.setCellValueFactory(data -> data.getValue().fileNameProperty());
        TableColumn<LyricConfigData, Double> offsetCol = new TableColumn<>("Offset");
        offsetCol.setCellValueFactory(data -> data.getValue().offsetProperty().asObject());
        offsetCol.setCellFactory(col -> new EditableCell<>(stringToDouble));
        offsetCol.setSortable(false);
        TableColumn<LyricConfigData, Double> consonantCol = new TableColumn<>("Consonant");
        consonantCol.setCellValueFactory(data -> data.getValue().consonantProperty().asObject());
        consonantCol.setCellFactory(col -> new EditableCell<>(stringToDouble));
        consonantCol.setSortable(false);
        TableColumn<LyricConfigData, Double> cutoffCol = new TableColumn<>("Cutoff");
        cutoffCol.setCellValueFactory(data -> data.getValue().cutoffProperty().asObject());
        cutoffCol.setCellFactory(col -> new EditableCell<>(stringToDouble));
        cutoffCol.setSortable(false);
        TableColumn<LyricConfigData, Double> preutterCol = new TableColumn<>("Preutter");
        preutterCol.setCellValueFactory(data -> data.getValue().preutterProperty().asObject());
        preutterCol.setCellFactory(col -> new EditableCell<>(stringToDouble));
        preutterCol.setSortable(false);
        TableColumn<LyricConfigData, Double> overlapCol = new TableColumn<>("Overlap");
        overlapCol.setCellValueFactory(data -> data.getValue().overlapProperty().asObject());
        overlapCol.setCellFactory(col -> new EditableCell<>(stringToDouble));
        overlapCol.setSortable(false);
        table.getColumns().setAll(
                ImmutableList.of(
                        lyricCol,
                        fileCol,
                        offsetCol,
                        consonantCol,
                        cutoffCol,
                        preutterCol,
                        overlapCol));

        // Populate with lyrics.
        while (lyricIterator.hasNext()) {
            LyricConfigData data = lyricIterator.next();
            lyrics.add(data);
            data.lyricProperty().addListener((observable, oldLyric, newLyric) -> {
                // Set old lyric to new lyric and set whether new lyric is valid.
                // TODO: Don't remove if old lyric is invalid.
                model.removeLyric(oldLyric);
                model.addLyric(data);
                // TODO: Mark invalid if current lyric isn't valid.
            });
            for (Property<?> property : data.mutableProperties()) {
                property.addListener(event -> {
                    // Mutate lyric with new data.
                    model.modifyLyric(data);
                });
            }
        }
        return scrollPane;
    }

    private void clear() {
        // Remove current lyric configs.
        tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
        HBox.setHgrow(tabPane, Priority.ALWAYS);
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
