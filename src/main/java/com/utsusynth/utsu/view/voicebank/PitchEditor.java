package com.utsusynth.utsu.view.voicebank;

import java.util.Iterator;
import com.google.common.collect.ImmutableList;
import com.utsusynth.utsu.common.data.PitchMapData;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.text.Font;
import javafx.util.StringConverter;

public class PitchEditor {
    private TableView<PitchMapData> table;
    private PitchCallback model;

    /** Initialize editor with data from the controller. */
    public void initialize(PitchCallback callback) {
        this.model = callback;
    }

    public TableView<PitchMapData> createPitchView(Iterator<PitchMapData> pitchIterator) {
        ObservableList<PitchMapData> pitches = FXCollections.observableArrayList();
        table = new TableView<>(pitches);
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        table.setEditable(true);

        // Add columns.
        TableColumn<PitchMapData, String> pitchCol = new TableColumn<>("Pitch");
        pitchCol.setCellValueFactory(data -> data.getValue().pitchProperty());
        pitchCol.prefWidthProperty().bind(table.widthProperty().multiply(0.3));
        pitchCol.setResizable(false);
        pitchCol.setSortable(false);
        TableColumn<PitchMapData, String> suffixCol = new TableColumn<>("Suffix");
        suffixCol.setCellValueFactory(data -> data.getValue().suffixProperty());
        suffixCol.setCellFactory(col -> new EditableCell<>(stringToString));
        suffixCol.prefWidthProperty().bind(table.widthProperty().multiply(0.63));
        suffixCol.setResizable(false);
        suffixCol.setSortable(false);
        table.getColumns().setAll(ImmutableList.of(pitchCol, suffixCol));

        // Populate with pitch data.
        while (pitchIterator.hasNext()) {
            PitchMapData data = pitchIterator.next();
            pitches.add(data);
            data.suffixProperty().addListener(event -> {
                model.setPitch(data);
            });
        }

        return table;
    }

    public void setSelected(String suffix) {
        for (PitchMapData data : table.getSelectionModel().getSelectedItems()) {
            data.suffixProperty().set(suffix);
        }
    }

    public void selectAll() {
        table.getSelectionModel().selectAll();
    }

    private class EditableCell<T> extends TableCell<PitchMapData, T> {
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
}
