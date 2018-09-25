package com.utsusynth.utsu.view.song.note;

import com.utsusynth.utsu.common.quantize.Quantizer;
import com.utsusynth.utsu.common.quantize.Scaler;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.text.Font;

public class Lyric {
    private String lyric;
    private String alias; // Defaults to empty string if there is no alias.
    private LyricCallback trackNote;
    private Group activeNode;

    private final Label text;
    private final TextField textField;

    public Lyric(String defaultLyric, Scaler scaler) {
        this.lyric = defaultLyric;
        this.alias = "";
        this.text = new Label(defaultLyric);
        this.text.setMouseTransparent(true);
        this.textField = new TextField("mi");
        this.textField.setFont(Font.font(9));
        this.textField.setMaxHeight(scaler.scaleY(Quantizer.ROW_HEIGHT) - 2);
        this.textField.setMaxWidth(scaler.scaleX(Quantizer.COL_WIDTH) - 2);
        this.textField.setOnAction((event) -> {
            closeTextFieldIfNeeded();
        });
        this.textField.focusedProperty().addListener(event -> {
            if (!this.textField.isFocused()) {
                closeTextFieldIfNeeded();
            }
        });

        // Initialize with text active.
        activeNode = new Group();
        activeNode.getChildren().add(text);
    }

    /** Connect this lyric to a track note. */
    void initialize(LyricCallback callback) {
        this.trackNote = callback;
    }

    void setVisibleLyric(String newLyric) {
        if (!newLyric.equals(this.lyric)) {
            text.setText(alias.length() > 0 ? newLyric + " (" + alias + ")" : newLyric);
            textField.setText(newLyric);
            this.lyric = newLyric;
            this.trackNote.adjustColumnSpan();
        }
    }

    Group getElement() {
        return activeNode;
    }

    String getLyric() {
        return this.lyric;
    }

    double getWidth() {
        double width = Math.max(textField.getWidth(), text.getWidth());
        if (width <= 0) {
            // If width not calculated yet, infer from current text instead.
            return text.getText().length() * 10;
        }
        return width;
    }

    void setVisibleAlias(String newAlias) {
        text.setText(newAlias.length() > 0 ? lyric + " (" + newAlias + ")" : lyric);
        this.alias = newAlias;
        this.trackNote.adjustColumnSpan();
    }

    void openTextField() {
        this.activeNode.getChildren().clear();
        this.activeNode.getChildren().add(this.textField);
        this.textField.requestFocus();
        this.textField.selectAll();
    }

    boolean isTextFieldOpen() {
        return this.activeNode.getChildren().contains(this.textField);
    }

    void closeTextFieldIfNeeded() {
        if (isTextFieldOpen()) {
            this.activeNode.getChildren().clear();
            this.activeNode.getChildren().add(this.text);
            String newLyric = textField.getText();
            setVisibleLyric(newLyric);
            this.trackNote.setSongLyric(newLyric);
        }
    }

    void registerLyric() {
        trackNote.setSongLyric(lyric);
    }
}
