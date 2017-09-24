package com.utsusynth.utsu.view;

import com.utsusynth.utsu.common.PitchUtils;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;

public class Piano {
	private final GridPane pianoGrid;

	Piano() {
		pianoGrid = new GridPane();
		addPianoKeys();
	}

	public GridPane getElement() {
		return pianoGrid;
	}

	private void addPianoKeys() {
		int rowNum = 0;
		for (int octave = 7; octave > 0; octave--) {
			for (String pitch : PitchUtils.REVERSE_PITCHES) {
				Pane leftHalfOfKey = new Pane();
				leftHalfOfKey.getStyleClass()
						.add(pitch.endsWith("#") ? "piano-black-key" : "piano-white-key");
				leftHalfOfKey.setPrefSize(60, 20);
				leftHalfOfKey.getChildren().add(new Label(pitch + octave));

				Node rightHalfOfKey;
				if (pitch.endsWith("#")) {
					GridPane bisectedKey = new GridPane();
					Pane child1 = new Pane();
					child1.setPrefSize(40, 10);
					child1.getStyleClass().add("piano-white-key");
					Pane child2 = new Pane();
					child2.setPrefSize(40, 10);
					child2.getStyleClass().add("piano-no-border");
					bisectedKey.addColumn(0, child1, child2);
					rightHalfOfKey = bisectedKey;
				} else {
					Pane blankKey = new Pane();
					blankKey.setPrefSize(40, 20);
					if (pitch.startsWith("F") || pitch.startsWith("C")) {
						blankKey.getStyleClass().add("piano-white-key");
					} else {
						blankKey.getStyleClass().add("piano-no-border");
					}
					rightHalfOfKey = blankKey;
				}

				pianoGrid.addRow(rowNum, leftHalfOfKey, rightHalfOfKey);
				rowNum++;
			}
		}
		Pane extraSpace = new Pane();
		extraSpace.setPrefSize(40, 16);
		GridPane.setColumnSpan(extraSpace, 2);
		pianoGrid.addRow(rowNum, extraSpace);
	}
}
