package com.utsusynth.utsu.view.song.note;

import com.google.common.collect.ImmutableSet;
import com.utsusynth.utsu.common.quantize.Quantizer;
import com.utsusynth.utsu.common.quantize.Scaler;
import com.utsusynth.utsu.view.song.track.TrackItem;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;

import java.util.HashSet;

public class Lyric implements TrackItem {
    private final HashSet<Integer> drawnColumns;
    private final Scaler scaler;

    // UI-independent state.
    private final SimpleBooleanProperty editMode;
    private String lyric;
    private String alias;

    // UI-dependent state.
    private Group activeNode;
    private HBox lyricAndAlias;
    private Label lyricText;
    private Label aliasText; // Defaults to empty string if there is no alias.
    private TextField textField;

    private LyricCallback trackNote;
    private BooleanProperty showLyrics;
    private BooleanProperty showAliases;

    public Lyric(String defaultLyric, Scaler scaler) {
        this.scaler = scaler;
        editMode = new SimpleBooleanProperty(false);
        lyric = defaultLyric;
        alias = "";

        drawnColumns = new HashSet<>();
    }

    /** Connect this lyric to a track note. */
    void initialize(
            LyricCallback callback, BooleanProperty showLyrics, BooleanProperty showAliases) {
        trackNote = callback;
        this.showLyrics = showLyrics;
        this.showAliases = showAliases;
    }

    void setVisibleLyric(String newLyric) {
        if (!newLyric.equals(lyric)) {
            lyric = newLyric;
            if (lyricText != null) {
                lyricText.setText(newLyric);
            }
            if (textField != null) {
                textField.setText(newLyric);
            }
            adjustLyricAndAlias();
            trackNote.adjustColumnSpan();
        }
    }

    @Override
    public TrackItemType getType() {
        return TrackItemType.LYRIC;
    }

    @Override
    public double getStartX() {
        return 0;
    }

    @Override
    public double getWidth() {
        return 0;
    }

    @Override
    public Group redraw() {
        return redraw(0);
    }

    @Override
    public Group redraw(double offsetX) {
        lyricText = new Label(lyric);
        lyricText.getStyleClass().add("track-note-text");

        aliasText = new Label(alias);
        aliasText.getStyleClass().add("track-note-text");
        aliasText.visibleProperty().bind(showAliases);

        lyricAndAlias = new HBox(lyricText, aliasText);
        lyricAndAlias.setMouseTransparent(true);
        lyricAndAlias.visibleProperty().bind(showLyrics);

        textField = new TextField();
        textField.setFont(Font.font(9));
        textField.setMaxHeight(scaler.scaleY(Quantizer.ROW_HEIGHT).get() - 2);
        textField.setMaxWidth(scaler.scaleX(Quantizer.COL_WIDTH).get() - 2);
        textField.setText(lyric);
        textField.setOnAction((event) -> {
            closeTextFieldIfNeeded();
        });
        textField.focusedProperty().addListener(event -> {
            if (!textField.isFocused()) {
                closeTextFieldIfNeeded();
            }
        });

        // Initialize with text active.
        activeNode = new Group();
            activeNode.getChildren().add(editMode.get() ? textField : lyricAndAlias);
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
    }

    @Override
    public void removeAllColumns() {
        drawnColumns.clear();
    }

    String getLyric() {
        return lyric;
    }

    void setVisibleAlias(String newAlias) {
        if (!newAlias.equals(alias)) {
            if (newAlias.length() > 0) {
                alias = " (" + newAlias + ")";
            } else {
                alias = newAlias;
            }
            if (aliasText != null) {
                aliasText.setText(alias);
            }
            adjustLyricAndAlias();
            trackNote.adjustColumnSpan();
        }
    }

    void openTextField() {
        editMode.set(true);
        if (activeNode != null && textField != null) {
            activeNode.getChildren().clear();
            activeNode.getChildren().add(textField);
            textField.requestFocus();
            textField.selectAll();
        }
    }

    boolean isTextFieldOpen() {
        return editMode.get();
    }

    void closeTextFieldIfNeeded() {
        if (isTextFieldOpen() && activeNode != null && textField != null) {
            editMode.set(false);
            activeNode.getChildren().clear();
            activeNode.getChildren().add(lyricAndAlias);
            String oldLyric = lyric;
            String newLyric = textField.getText();
            setVisibleLyric(newLyric);
            trackNote.replaceSongLyric(oldLyric, newLyric);
        }
    }

    void registerLyric() {
        trackNote.setSongLyric(lyric);
    }

    private void adjustLyricAndAlias() {
        if (lyricAndAlias == null || lyricText == null || aliasText == null) {
            return;
        }
        lyricAndAlias.getChildren().clear();
        if (!lyricText.getText().isEmpty()) {
            lyricAndAlias.getChildren().add(lyricText);
        }
        if (!aliasText.getText().isEmpty()) {
            lyricAndAlias.getChildren().add(aliasText);
        }
    }
}
