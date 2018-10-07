package com.utsusynth.utsu.view.song;

import com.google.inject.Inject;
import com.utsusynth.utsu.common.quantize.Quantizer;
import com.utsusynth.utsu.common.quantize.Scaler;
import com.utsusynth.utsu.common.utils.PitchUtils;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;

public class Piano {
    private final Scaler scaler;

    private GridPane pianoGrid;

    @Inject
    public Piano(Scaler scaler) {
        this.scaler = scaler;
    }

    public GridPane initPiano() {
        this.pianoGrid = new GridPane();
        addPianoKeys();
        return this.pianoGrid;
    }

    private void addPianoKeys() {
        int rowNum = 0;
        for (int octave = 7; octave > 0; octave--) {
            for (String pitch : PitchUtils.REVERSE_PITCHES) {
                Pane leftHalfOfKey = new Pane();
                leftHalfOfKey.getStyleClass()
                        .add(pitch.endsWith("#") ? "piano-black-key" : "piano-white-key");
                leftHalfOfKey.setPrefSize(60, scaler.scaleY(Quantizer.ROW_HEIGHT));
                leftHalfOfKey.getChildren().add(new Label(pitch + octave));

                Node rightHalfOfKey;
                if (pitch.endsWith("#")) {
                    GridPane bisectedKey = new GridPane();
                    Pane child1 = new Pane();
                    child1.setPrefSize(40, scaler.scaleY(Quantizer.ROW_HEIGHT / 2));
                    child1.getStyleClass().add("piano-white-key");
                    Pane child2 = new Pane();
                    child2.setPrefSize(40, scaler.scaleY(Quantizer.ROW_HEIGHT / 2));
                    child2.getStyleClass().add("piano-no-border");
                    bisectedKey.addColumn(0, child1, child2);
                    rightHalfOfKey = bisectedKey;
                } else {
                    Pane blankKey = new Pane();
                    blankKey.setPrefSize(40, scaler.scaleY(Quantizer.ROW_HEIGHT));
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
    }
}
