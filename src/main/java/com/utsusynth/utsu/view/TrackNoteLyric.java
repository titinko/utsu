package com.utsusynth.utsu.view;

import com.google.common.base.Function;
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
	private Region activeNode;

	static TrackNoteLyric makeLyric(final Runnable callback) {
		TextField defaultTextField = new TextField("mi");
		TrackNoteLyric trackNoteLyric = new TrackNoteLyric(defaultTextField, "mi");
		defaultTextField.setFont(Font.font(9));
		defaultTextField.setMaxHeight(ROW_HEIGHT - 2);
		defaultTextField.setMaxWidth(COL_WIDTH - 2);
		defaultTextField.setOnAction((event) -> {
			callback.run();
		}); 
		return trackNoteLyric;
	}
	
	private TrackNoteLyric(TextField textField, String defaultLyric) {
		this.lyric = defaultLyric;
		this.alias = Optional.absent();
		this.text = new Label(defaultLyric);
		this.textField = textField;
	}
	
	void setLyric(String newLyric) {
		text.setText(alias.isPresent() ? newLyric + " (" + alias.get() + ")" : newLyric);
		textField.setText(newLyric);
		this.lyric = newLyric;
	}
	
	String getLyric() {
		return this.lyric;
	}
	
	double getWidth() {
		return activeNode.getWidth();
	}
	
	void setAlias(Optional<String> newAlias) {
		text.setText(newAlias.isPresent() ? lyric + " (" + newAlias.get() + ")" : this.lyric);
		this.alias = newAlias;
	}
	
	Region openTextElement() {
		this.activeNode = this.text;
		return this.text;
	}
	
	void openTextField(StackPane layout) {
		int index = layout.getChildren().indexOf(this.activeNode);
		this.activeNode = this.textField;
		layout.getChildren().set(index, this.textField);
		this.textField.requestFocus();
		this.textField.selectAll();
	}
	
	void closeTextFieldIfNeeded(Function<String, StackPane> callback) {
		if (this.activeNode == this.textField) {
			this.activeNode = this.text;
			String lyric = textField.getText();
			setLyric(lyric);
			StackPane layout = callback.apply(lyric);
			int index = layout.getChildren().indexOf(this.textField);
			layout.getChildren().set(index, this.text);
		}
	}

	void setLeftMargin(int newMargin) {
		StackPane.setMargin(text, new Insets(0, 0, 0, newMargin));
		StackPane.setMargin(textField, new Insets(0, 0, 0, newMargin));
	}
}
