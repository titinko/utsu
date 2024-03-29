package com.utsusynth.utsu.view.voicebank;

import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.utsusynth.utsu.common.data.PitchMapData;
import com.utsusynth.utsu.common.i18n.Localizable;
import com.utsusynth.utsu.common.i18n.Localizer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.text.Font;
import javafx.util.StringConverter;

public class PitchEditor implements Localizable {
    private final Localizer localizer;

    private TableView<PitchMapData> table;
    private PitchCallback model;

    @Inject
    public PitchEditor(Localizer localizer) {
        this.localizer = localizer;
    }

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
        TableColumn<PitchMapData, String> prefixCol = new TableColumn<>("Prefix");
        prefixCol.setCellValueFactory(data -> data.getValue().prefixProperty());
        prefixCol.setCellFactory(col -> new EditableCell<>(stringToString));
        prefixCol.prefWidthProperty().bind(table.widthProperty().multiply(0.3));
        prefixCol.setResizable(false);
        prefixCol.setSortable(false);
        prefixCol.setOnEditCommit(event -> {
            PitchMapData pitchData = event.getRowValue();
            String oldPrefix = event.getOldValue();
            String newPrefix = event.getNewValue();
            model.recordAction(
                    () -> pitchData.prefixProperty().set(newPrefix),
                    () -> pitchData.prefixProperty().set(oldPrefix));
            pitchData.prefixProperty().set(newPrefix);
        });
        TableColumn<PitchMapData, String> suffixCol = new TableColumn<>("Suffix");
        suffixCol.setCellValueFactory(data -> data.getValue().suffixProperty());
        suffixCol.setCellFactory(col -> new EditableCell<>(stringToString));
        suffixCol.prefWidthProperty().bind(table.widthProperty().multiply(0.33));
        suffixCol.setResizable(false);
        suffixCol.setSortable(false);
        suffixCol.setOnEditCommit(event -> {
            PitchMapData pitchData = event.getRowValue();
            String oldSuffix = event.getOldValue();
            String newSuffix = event.getNewValue();
            model.recordAction(
                    () -> pitchData.suffixProperty().set(newSuffix),
                    () -> pitchData.suffixProperty().set(oldSuffix));
            pitchData.suffixProperty().set(newSuffix);
        });
        table.getColumns().setAll(ImmutableList.of(pitchCol, prefixCol, suffixCol));

        // Populate with pitch data.
        while (pitchIterator.hasNext()) {
            PitchMapData data = pitchIterator.next();
            pitches.add(data);
            data.prefixProperty().addListener(event -> {
                model.setPitch(data);
            });
            data.suffixProperty().addListener(event -> {
                model.setPitch(data);
            });
        }

        localizer.localize(this);
        return table;
    }

    public void setPrefixForSelected(String prefix) {
        List<String> oldPrefixes = table.getSelectionModel().getSelectedItems().stream()
                .map(PitchMapData::getPrefix).collect(Collectors.toList());
        List<PitchMapData> selected =
                ImmutableList.copyOf(table.getSelectionModel().getSelectedItems());
        Runnable redoAction = () -> {
            for (PitchMapData data : selected) {
                data.prefixProperty().set(prefix);
            }
        };
        Runnable undoAction = () -> {
            for (int i = 0; i < selected.size(); i++) {
                selected.get(i).prefixProperty().set(oldPrefixes.get(i));
            }
        };
        model.recordAction(redoAction, undoAction);
        redoAction.run();
    }

    public void setSuffixForSelected(String suffix) {
        List<String> oldSuffixes = table.getSelectionModel().getSelectedItems().stream()
                .map(PitchMapData::getSuffix).collect(Collectors.toList());
        List<PitchMapData> selected =
                ImmutableList.copyOf(table.getSelectionModel().getSelectedItems());
        Runnable redoAction = () -> {
            for (PitchMapData data : selected) {
                data.suffixProperty().set(suffix);
            }
        };
        Runnable undoAction = () -> {
            for (int i = 0; i < selected.size(); i++) {
                selected.get(i).suffixProperty().set(oldSuffixes.get(i));
            }
        };
        model.recordAction(redoAction, undoAction);
        redoAction.run();
    }

    public void selectAll() {
        table.getSelectionModel().selectAll();
    }

    @Override
    public void localize(ResourceBundle bundle) {
        if (table != null) {
            table.getColumns().get(0).setText(bundle.getString("voice.pitch"));
            table.getColumns().get(1).setText(bundle.getString("voice.prefix"));
            table.getColumns().get(2).setText(bundle.getString("voice.suffix"));
        }
    }

    private static class EditableCell<T> extends TableCell<PitchMapData, T> {
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

    private final StringConverter<String> stringToString = new StringConverter<>() {
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
