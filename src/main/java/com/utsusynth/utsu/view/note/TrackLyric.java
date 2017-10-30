package com.utsusynth.utsu.view.note;

import com.google.common.base.Optional;
import com.utsusynth.utsu.common.quantize.Quantizer;

import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.text.Font;

public class TrackLyric {
	private String lyric;
	private Optional<String> alias;
	private TrackLyricCallback trackNote;
	private Group activeNode;

	private final Label text;
	private final TextField textField;

	public TrackLyric(String defaultLyric, Quantizer quantizer) {
		this.lyric = defaultLyric;
		this.alias = Optional.absent();
		this.text = new Label(defaultLyric);
		this.text.setMouseTransparent(true);
		this.textField = new TextField("mi");
		this.textField.setFont(Font.font(9));
		this.textField.setMaxHeight(Quantizer.ROW_HEIGHT - 2);
		this.textField.setMaxWidth(quantizer.getColWidth() - 2);

		// Initialize with text active.
		activeNode = new Group();
		activeNode.getChildren().add(text);
	}

	/** Connect this lyric to a track note. */
	void initialize(TrackLyricCallback callback) {
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

	void setVisibleAlias(Optional<String> newAlias) {
		if (!newAlias.equals(this.alias)) {
			text.setText(newAlias.isPresent() ? lyric + " (" + newAlias.get() + ")" : lyric);
			this.alias = newAlias;
			this.trackNote.adjustColumnSpan();
		}
	}

	void openTextElement() {
		this.activeNode.getChildren().clear();
		this.activeNode.getChildren().add(this.text);
	}

	void openTextField() {
		this.activeNode.getChildren().clear();
		this.activeNode.getChildren().add(this.textField);
		this.textField.requestFocus();
		this.textField.selectAll();
	}

	void closeTextFieldIfNeeded() {
		if (this.activeNode.getChildren().contains(this.textField)) {
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
