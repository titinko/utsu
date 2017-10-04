package com.utsusynth.utsu.view.note;

import com.google.common.base.Optional;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;

public class TrackNoteLyric {
	private static final int ROW_HEIGHT = 20;
	private static final int COL_WIDTH = 80;

	private String lyric;
	private Optional<String> alias;

	private final Label text;
	private final TextField textField;
	private TrackNoteCallback trackNote;
	private Region activeNode;

	public TrackNoteLyric(String defaultLyric) {
		this.lyric = defaultLyric;
		this.alias = Optional.absent();
		this.text = new Label(defaultLyric);
		this.textField = new TextField("mi");
		this.textField.setFont(Font.font(9));
		this.textField.setMaxHeight(ROW_HEIGHT - 2);
		this.textField.setMaxWidth(COL_WIDTH - 2);
	}

	/** Connect this lyric to a track note. */
	void initialize(TrackNoteCallback callback) {
		this.textField.setOnAction((event) -> {
			callback.setHighlighted(false);
		});
		this.trackNote = callback;
	}

	void setVisibleLyric(String newLyric) {
		if (!newLyric.equals(this.lyric)) {
			text.setText(alias.isPresent() ? newLyric + " (" + alias.get() + ")" : newLyric);
			textField.setText(newLyric);
			this.lyric = newLyric;
			this.trackNote.adjustColumnSpan();
		}
	}

	String getLyric() {
		return this.lyric;
	}

	double getWidth() {
		// TODO: Infer width from current text instead.
		double width = activeNode.getWidth();
		if (width <= 0) {
			return text.getText().length() * 10;
		}
		return width;
	}

	void setVisibleAlias(Optional<String> newAlias) {
		if (!newAlias.equals(this.alias)) {
			text.setText(newAlias.isPresent() ? lyric + " (" + newAlias.get() + ")" : lyric);
			this.alias = newAlias;
			this.trackNote.adjustColumnSpan();
		}
	}

	Region openTextElement() {
		this.activeNode = this.text;
		return this.text;
	}

	void openTextField() {
		this.activeNode = this.textField;
		this.trackNote.setLyricElement(this.textField);
		this.textField.requestFocus();
		this.textField.selectAll();
	}

	void closeTextFieldIfNeeded() {
		if (this.activeNode == this.textField) {
			this.activeNode = this.text;
			String lyric = textField.getText();
			setVisibleLyric(lyric);
			this.trackNote.setLyricElement(this.text);
			this.trackNote.setSongLyric(lyric);
		}
	}

	void setSongLyric(String lyric) {
		trackNote.setSongLyric(lyric);
	}

	void setLeftMargin(int newMargin) {
		StackPane.setMargin(text, new Insets(0, 0, 0, newMargin));
		StackPane.setMargin(textField, new Insets(0, 0, 0, newMargin));
	}
}
