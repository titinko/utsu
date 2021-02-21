package com.utsusynth.utsu.view.song;

import com.google.inject.Inject;
import com.utsusynth.utsu.common.quantize.Quantizer;
import com.utsusynth.utsu.common.quantize.Scaler;
import javafx.geometry.Orientation;
import javafx.scene.control.ListView;

/** The background track of the song editor. */
public class Track {
    private final Quantizer quantizer;
    private final Scaler scaler;

    private ListView<String> noteTrack;
    private ListView<String> dynamicsTrack;

    @Inject
    public Track(Quantizer quantizer, Scaler scaler) {
        this.quantizer = quantizer;
        this.scaler = scaler;
    }

    public void initialize() {
        double rowHeight = scaler.scaleY(Quantizer.ROW_HEIGHT).get();

        noteTrack = new ListView<>();
        noteTrack.setFixedCellSize(rowHeight);
        noteTrack.setOrientation(Orientation.HORIZONTAL);

        dynamicsTrack = new ListView<>();
        dynamicsTrack.setOrientation(Orientation.HORIZONTAL);
    }
}
