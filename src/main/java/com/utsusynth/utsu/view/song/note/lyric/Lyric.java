package com.utsusynth.utsu.view.song.note.lyric;

import com.google.common.collect.ImmutableSet;
import com.utsusynth.utsu.common.quantize.Quantizer;
import com.utsusynth.utsu.common.quantize.Scaler;
import com.utsusynth.utsu.view.song.track.TrackItem;
import javafx.beans.property.*;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;

import java.util.HashMap;
import java.util.HashSet;

public class Lyric implements TrackItem {
    private final HashMap<Double, HBox> drawnHBoxes;
    private final HashMap<Double, Label> drawnLyrics;
    private final HashMap<Double, Label> drawnAliases;
    private final HashMap<Double, TextField> drawnTextFields;
    private final HashMap<Double, Group> drawnActiveNodes;
    private final HashSet<Integer> drawnColumns;
    private final Scaler scaler;

    // UI-independent state.
    private final DoubleProperty startX;
    private final DoubleProperty noteWidth;
    private final DoubleProperty currentY;
    private final BooleanProperty editMode;
    private final StringProperty lyric;
    private final StringProperty alias;

    private LyricCallback trackNote;
    private BooleanProperty showLyrics;
    private BooleanProperty showAliases;

    public Lyric(String defaultLyric, Scaler scaler) {
        this.scaler = scaler;
        startX = new SimpleDoubleProperty(0);
        noteWidth = new SimpleDoubleProperty(Quantizer.TEXT_FIELD_WIDTH);
        currentY = new SimpleDoubleProperty(0);
        editMode = new SimpleBooleanProperty(false);
        lyric = new SimpleStringProperty(defaultLyric);
        alias = new SimpleStringProperty("");

        drawnHBoxes = new HashMap<>();
        drawnLyrics = new HashMap<>();
        drawnAliases = new HashMap<>();
        drawnTextFields = new HashMap<>();
        drawnActiveNodes = new HashMap<>();
        drawnColumns = new HashSet<>();
    }

    /** Connect this lyric to a track note. */
    public void initialize(
            LyricCallback callback,
            DoubleProperty startX,
            DoubleProperty noteWidth,
            DoubleProperty currentY,
            BooleanProperty showLyrics,
            BooleanProperty showAliases) {
        trackNote = callback;
        this.startX.bind(startX);
        this.noteWidth.bind(noteWidth);
        this.currentY.bind(currentY);
        this.showLyrics = showLyrics;
        this.showAliases = showAliases;
    }

    public void setVisibleLyric(String newLyric) {
        if (!newLyric.equals(lyric.get())) {
            lyric.set(newLyric);
            adjustLyricAndAlias();
        }
    }

    @Override
    public TrackItemType getType() {
        return TrackItemType.LYRIC;
    }

    @Override
    public double getStartX() {
        return startX.get();
    }

    @Override
    public double getWidth() {
        return Math.max(noteWidth.get(), Quantizer.TEXT_FIELD_WIDTH * 2);
    }

    @Override
    public Group redraw() {
        return redraw(0);
    }

    @Override
    public Group redraw(double offsetX) {
        Label lyricText = new Label();
        lyricText.textProperty().bind(lyric);
        lyricText.getStyleClass().add("track-note-text");
        lyricText.setMaxWidth(Math.max(noteWidth.get() / 2, Quantizer.TEXT_FIELD_WIDTH));
        drawnLyrics.put(offsetX, lyricText);

        Label aliasText = new Label();
        aliasText.textProperty().bind(alias);
        aliasText.getStyleClass().add("track-note-text");
        aliasText.visibleProperty().bind(showAliases);
        aliasText.setMaxWidth(Math.max(noteWidth.get() / 2, Quantizer.TEXT_FIELD_WIDTH));
        drawnAliases.put(offsetX, aliasText);

        HBox lyricAndAlias = new HBox(lyricText, aliasText);
        lyricAndAlias.setMouseTransparent(true);
        lyricAndAlias.visibleProperty().bind(showLyrics);
        drawnHBoxes.put(offsetX, lyricAndAlias);

        TextField textField = new TextField();
        textField.setFont(Font.font(9));
        textField.setMaxHeight(scaler.scaleY(Quantizer.ROW_HEIGHT).get() - 2);
        textField.setMaxWidth(Quantizer.TEXT_FIELD_WIDTH);
        textField.setOnAction(event -> closeTextFieldIfNeeded());
        textField.focusedProperty().addListener(event -> {
            for (TextField tf : drawnTextFields.values()) {
                if (tf.isFocused()) {
                    return;
                }
            }
            closeTextFieldIfNeeded();
        });
        drawnTextFields.put(offsetX, textField);

        // Initialize with text active.
        Group activeNode = new Group();
        activeNode.getChildren().add(editMode.get() ? textField : lyricAndAlias);
        activeNode.translateXProperty().bind(startX.subtract(offsetX));
        activeNode.translateYProperty().bind(currentY);
        drawnActiveNodes.put(offsetX, activeNode);

        Rectangle clip = new Rectangle(scaler.scaleX(Quantizer.TRACK_COL_WIDTH).get(),
                scaler.scaleY(Quantizer.ROW_HEIGHT).get());
        clip.xProperty().bind(startX.subtract(offsetX).negate());
        activeNode.setClip(clip);
        return activeNode;
    }

    @Override
    public ImmutableSet<Integer> getColumns() {
        return ImmutableSet.copyOf(drawnColumns);
    }

    @Override
    public void addColumn(int colNum) {
        drawnColumns.add(colNum);
    }

    @Override
    public void removeColumn(int colNum) {
        drawnColumns.remove(colNum);
        double offsetX = colNum * scaler.scaleX(Quantizer.TRACK_COL_WIDTH).get();
        drawnHBoxes.remove(offsetX);
        drawnLyrics.remove(offsetX);
        drawnAliases.remove(offsetX);
        drawnTextFields.remove(offsetX);
        drawnActiveNodes.remove(offsetX);
    }

    @Override
    public void removeAllColumns() {
        drawnColumns.clear();
    }

    public String getLyric() {
        return lyric.get();
    }

    public void setVisibleAlias(String newAlias) {
        if (!newAlias.equals(alias.get())) {
            if (newAlias.length() > 0) {
                alias.set(" (" + newAlias + ")");
            } else {
                alias.set(newAlias);
            }
            adjustLyricAndAlias();
        }
    }

    public void openTextField() {
        editMode.set(true);
        for (double offsetX : drawnActiveNodes.keySet()) {
            Group activeNode = drawnActiveNodes.get(offsetX);
            TextField textField = drawnTextFields.get(offsetX);

            activeNode.getChildren().clear();
            activeNode.getChildren().add(textField);
            textField.setText(lyric.get());
            textField.requestFocus();
            textField.selectAll();
        }
    }

    public boolean isTextFieldOpen() {
        return editMode.get();
    }

    public void closeTextFieldIfNeeded() {
        if (isTextFieldOpen()) {
            editMode.set(false);
            String oldLyric = lyric.get();
            String newLyric = oldLyric;
            for (double offsetX : drawnActiveNodes.keySet()) {
                Group activeNode = drawnActiveNodes.get(offsetX);
                activeNode.getChildren().clear();
                activeNode.getChildren().add(drawnHBoxes.get(offsetX));

                TextField textField = drawnTextFields.get(offsetX);
                if (!textField.getText().equals(oldLyric)) {
                    newLyric = textField.getText();
                }
            }
            setVisibleLyric(newLyric);
            trackNote.replaceSongLyric(oldLyric, newLyric);
        }
    }

    public void registerLyric() {
        trackNote.setSongLyric(lyric.get());
    }

    private void adjustLyricAndAlias() {
        if (editMode.get()) {
            return;
        }
        for (double offsetX : drawnHBoxes.keySet()) {
            HBox hBox = drawnHBoxes.get(offsetX);
            hBox.getChildren().clear();
            if (!lyric.get().isEmpty()) {
                hBox.getChildren().add(drawnLyrics.get(offsetX));
            }
            if (!alias.get().isEmpty()) {
                hBox.getChildren().add(drawnAliases.get(offsetX));
            }
        }
    }
}
