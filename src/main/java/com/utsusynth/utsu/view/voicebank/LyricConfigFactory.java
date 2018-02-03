package com.utsusynth.utsu.view.voicebank;

import com.google.inject.Inject;
import com.utsusynth.utsu.common.data.LyricConfigData;
import com.utsusynth.utsu.common.quantize.Quantizer;
import com.utsusynth.utsu.common.quantize.Scaler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;

public class LyricConfigFactory {
    private final Scaler scaler;

    @Inject
    public LyricConfigFactory(Scaler scaler) {
        this.scaler = scaler;
    }

    public LyricConfig createLyricConfig(LyricConfigCallback callback, LyricConfigData data) {
        return new LyricConfig(callback);
        // createCell(data.getLyric()),
        // createCell(data.getFileName()),
        // createCell(String.valueOf(data.getOffset())),
        // createCell(String.valueOf(data.getConsonant())),
        // createCell(String.valueOf(data.getCutoff())),
        // createCell(String.valueOf(data.getPreutter())),
        // createCell(String.valueOf(data.getOverlap())));
    }

    public StackPane createCell(String content) {
        StackPane cell = new StackPane();
        cell.setPrefHeight(Math.round(scaler.scaleY(Quantizer.ROW_HEIGHT)));
        cell.getStyleClass().addAll("text-cell", "valid", "not-highlighted");
        Text text = new Text(content);
        StackPane.setAlignment(text, Pos.CENTER_LEFT);
        StackPane.setMargin(text, new Insets(2));
        cell.getChildren().add(text);
        return cell;
    }
}
