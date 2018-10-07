package com.utsusynth.utsu.view.song.note;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.utsusynth.utsu.common.data.NoteData;
import com.utsusynth.utsu.common.data.NoteUpdateData;
import com.utsusynth.utsu.common.quantize.Quantizer;
import com.utsusynth.utsu.common.quantize.Scaler;
import com.utsusynth.utsu.common.utils.PitchUtils;
import javafx.beans.property.BooleanProperty;
import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

public class NoteFactory {
    private final Scaler scaler;
    private final Quantizer quantizer;
    private final Provider<Lyric> lyricProvider;

    @Inject
    public NoteFactory(Scaler scaler, Quantizer quantizer, Provider<Lyric> lyricProvider) {
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
        layout.setTranslateX(scaler.scalePos(absStart));

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
        if (note.getTrueLyric().isPresent()) {
            lyric.setVisibleAlias(note.getTrueLyric().get());
        }

        // Set backup data if applicable.
        if (note.getEnvelope().isPresent() && note.getPitchbend().isPresent()
                && note.getConfigData().isPresent()) {
            trackNote.setBackupData(
                    new NoteUpdateData(
                            note.getPosition(),
                            note.getLyric(),
                            note.getEnvelope().get(),
                            note.getPitchbend().get(),
                            note.getConfigData().get()));
        }
        return trackNote;
    }

    public Note createDefaultNote(
            int row,
            int positionMs,
            int durationMs,
            NoteCallback callback,
            BooleanProperty vibratoEditor) {
        Rectangle note = new Rectangle();
        note.setWidth(scaler.scaleX(durationMs) - 1);
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
        layout.setTranslateX(scaler.scalePos(positionMs));

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
