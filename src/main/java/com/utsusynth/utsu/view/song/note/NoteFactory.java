package com.utsusynth.utsu.view.song.note;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.utsusynth.utsu.common.PitchUtils;
import com.utsusynth.utsu.common.data.NoteData;
import com.utsusynth.utsu.common.quantize.Quantizer;
import com.utsusynth.utsu.common.quantize.Scaler;
import com.utsusynth.utsu.view.song.note.pitch.portamento.CurveFactory;
import javafx.beans.property.BooleanProperty;
import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

public class NoteFactory {
    private final Scaler scaler;
    private final Quantizer quantizer;
    private final Provider<Lyric> lyricProvider;

    @Inject
    public NoteFactory(
            Scaler scaler,
            Quantizer quantizer,
            Provider<Lyric> lyricProvider,
            CurveFactory curveFactory) {
        this.scaler = scaler;
        this.quantizer = quantizer;
        this.lyricProvider = lyricProvider;
    }

    public Note createNote(NoteData note, NoteCallback callback, BooleanProperty vibratoEditor) {
        int absStart = note.getPosition();
        int absDuration = note.getDuration();
        Rectangle rect = new Rectangle();
        rect.setWidth(scaler.scaleX(absDuration) - 1);
        rect.setHeight(scaler.scaleY(Quantizer.ROW_HEIGHT) - 1);
        rect.getStyleClass().addAll("track-note", "valid", "not-highlighted");

        Rectangle edge = new Rectangle();
        edge.setWidth(3);
        edge.setHeight(rect.getHeight());
        edge.setOpacity(0.0);

        Rectangle overlap = new Rectangle();
        overlap.setWidth(0);
        overlap.setHeight(rect.getHeight());
        overlap.getStyleClass().add("note-overlap");

        StackPane layout = new StackPane();
        layout.setPickOnBounds(false);
        layout.setAlignment(Pos.CENTER_LEFT);
        layout.setTranslateY(
                scaler.scaleY(PitchUtils.pitchToRowNum(note.getPitch()) * Quantizer.ROW_HEIGHT));
        layout.setTranslateX(scaler.scaleX(absStart));

        Lyric lyric = lyricProvider.get();
        Note trackNote = new Note(
                rect,
                edge,
                overlap,
                lyric,
                layout,
                callback,
                vibratoEditor,
                quantizer,
                scaler);
        lyric.setVisibleLyric(note.getLyric());
        if (note.getConfig().isPresent()) {
            lyric.setVisibleAlias(note.getConfig().get().getTrueLyric());
        }

        return trackNote;
    }

    public Note createDefaultNote(
            int row,
            int column,
            NoteCallback callback,
            BooleanProperty vibratoEditor) {
        Rectangle note = new Rectangle();
        note.setWidth(scaler.scaleX(Quantizer.COL_WIDTH) - 1);
        note.setHeight(scaler.scaleY(Quantizer.ROW_HEIGHT) - 1);
        note.getStyleClass().addAll("track-note", "invalid", "not-highlighted");

        Rectangle edge = new Rectangle();
        edge.setWidth(3);
        edge.setHeight(note.getHeight());
        edge.setOpacity(0.0);

        Rectangle overlap = new Rectangle();
        overlap.setWidth(0);
        overlap.setHeight(note.getHeight());
        overlap.getStyleClass().add("note-overlap");

        StackPane layout = new StackPane();
        layout.setPickOnBounds(false);
        layout.setAlignment(Pos.CENTER_LEFT);
        layout.setTranslateY(scaler.scaleY(row * Quantizer.ROW_HEIGHT));
        layout.setTranslateX(scaler.scaleX(column * Quantizer.COL_WIDTH));

        Lyric lyric = lyricProvider.get();
        Note trackNote = new Note(
                note,
                edge,
                overlap,
                lyric,
                layout,
                callback,
                vibratoEditor,
                quantizer,
                scaler);
        lyric.registerLyric();

        return trackNote;
    }
}
