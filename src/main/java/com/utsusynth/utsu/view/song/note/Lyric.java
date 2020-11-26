package com.utsusynth.utsu.view.song.note;

import com.utsusynth.utsu.common.quantize.Quantizer;
import com.utsusynth.utsu.common.quantize.Scaler;
import javafx.beans.property.BooleanProperty;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;

public class Lyric {
    private LyricCallback trackNote;
    private final Group activeNode;

    private final HBox lyricAndAlias;
    private final Label lyricText;
    private final Label aliasText; // Defaults to empty string if there is no alias.
    private final TextField textField;

    public Lyric(String defaultLyric, Scaler scaler) {
        lyricText = new Label(defaultLyric);
        lyricText.getStyleClass().add("track-note-text");

        aliasText = new Label("");
        aliasText.getStyleClass().add("track-note-text");

        lyricAndAlias = new HBox(lyricText, aliasText);
        lyricAndAlias.setMouseTransparent(true);

        textField = new TextField();
        textField.setFont(Font.font(9));
        textField.setMaxHeight(scaler.scaleY(Quantizer.ROW_HEIGHT) - 2);
        textField.setMaxWidth(scaler.scaleX(Quantizer.COL_WIDTH) - 2);
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
        activeNode.getChildren().add(lyricAndAlias);
    }

    /** Connect this lyric to a track note. */
    void initialize(
            LyricCallback callback, BooleanProperty showLyrics, BooleanProperty showAliases) {
        trackNote = callback;
        activeNode.visibleProperty().bind(showLyrics);
        aliasText.visibleProperty().bind(showAliases);
    }

    void setVisibleLyric(String newLyric) {
        String oldLyric = lyricText.getText();
        if (!newLyric.equals(oldLyric)) {
            lyricText.setText(newLyric);
            textField.setText(newLyric);
            trackNote.adjustColumnSpan();
        }
    }

    Group getElement() {
        return activeNode;
    }

    String getLyric() {
        return lyricText.getText();
    }

    void setVisibleAlias(String newAlias) {
        lyricAndAlias.getChildren().remove(aliasText); // Will re-add this if alias present.
        String oldAlias = aliasText.getText();
        if (!newAlias.equals(oldAlias)) {
            if (newAlias.length() > 0) {
                aliasText.setText(" (" + newAlias + ")");
                lyricAndAlias.getChildren().add(aliasText);
            } else {
                aliasText.setText(newAlias);
            }
            trackNote.adjustColumnSpan();
        }
    }

    void openTextField() {
        activeNode.getChildren().clear();
        activeNode.getChildren().add(textField);
        textField.requestFocus();
        textField.selectAll();
    }

    boolean isTextFieldOpen() {
        return activeNode.getChildren().contains(textField);
    }

    void closeTextFieldIfNeeded() {
        if (isTextFieldOpen()) {
            activeNode.getChildren().clear();
            activeNode.getChildren().add(lyricAndAlias);
            String oldLyric = lyricText.getText();
            String newLyric = textField.getText();
            setVisibleLyric(newLyric);
            trackNote.replaceSongLyric(oldLyric, newLyric);
        }
    }

    void registerLyric() {
        trackNote.setSongLyric(lyricText.getText());
    }
}
