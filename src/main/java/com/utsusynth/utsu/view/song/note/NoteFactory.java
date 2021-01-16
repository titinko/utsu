package com.utsusynth.utsu.view.song.note;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.utsusynth.utsu.common.data.NoteData;
import com.utsusynth.utsu.common.data.NoteUpdateData;
import com.utsusynth.utsu.common.i18n.Localizer;
import com.utsusynth.utsu.common.quantize.ContinuousScaler;
import com.utsusynth.utsu.common.quantize.Quantizer;
import com.utsusynth.utsu.common.quantize.Scaler;
import com.utsusynth.utsu.common.utils.PitchUtils;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

public class NoteFactory {
    private final Localizer localizer;
    private final Scaler scaler;
    private final Quantizer quantizer;
    private final Provider<Lyric> lyricProvider;

    @Inject
    public NoteFactory(
            Localizer localizer,
            Scaler scaler,
            Quantizer quantizer,
            Provider<Lyric> lyricProvider) {
        this.localizer = localizer;
        this.scaler = scaler;
        this.quantizer = quantizer;
        this.lyricProvider = lyricProvider;
    }

    public Note createNote(
            NoteData note,
            NoteCallback callback,
            BooleanProperty vibratoEditor,
            BooleanProperty showLyrics,
            BooleanProperty showAliases) {
        int absStart = note.getPosition();
        int absDuration = note.getDuration();
        Rectangle rect = new Rectangle();
        rect.setWidth(scaler.scaleX(absDuration).get() - 1);
        rect.setHeight(scaler.scaleY(Quantizer.ROW_HEIGHT).get() - 1);
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
        layout.setTranslateY(scaler.scaleY(
                PitchUtils.pitchToRowNum(note.getPitch()) * Quantizer.ROW_HEIGHT).get());
        layout.setTranslateX(scaler.scalePos(absStart).get());

        Lyric lyric = lyricProvider.get();
        Note trackNote = new Note(
                rect,
                edge,
                overlap,
                lyric,
                layout,
                callback,
                vibratoEditor,
                showLyrics,
                showAliases,
                localizer,
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
            BooleanProperty vibratoEditor,
            BooleanProperty showLyrics,
            BooleanProperty showAliases) {
        Rectangle note = new Rectangle();
        note.setWidth(scaler.scaleX(durationMs).get() - 1);
        note.setHeight(scaler.scaleY(Quantizer.ROW_HEIGHT).get() - 1);
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
        layout.setTranslateY(scaler.scaleY(row * Quantizer.ROW_HEIGHT).get());
        layout.setTranslateX(scaler.scalePos(positionMs).get());

        Lyric lyric = lyricProvider.get();
        Note trackNote = new Note(
                note,
                edge,
                overlap,
                lyric,
                layout,
                callback,
                vibratoEditor,
                showLyrics,
                showAliases,
                localizer,
                quantizer,
                scaler);
        lyric.registerLyric();

        return trackNote;
    }

    public Note createBackgroundNote(int row, double startX, double widthX, Scaler noteScaler) {
        Rectangle note = new Rectangle();
        note.setWidth(widthX - 1);
        note.setHeight(noteScaler.scaleY(Quantizer.ROW_HEIGHT).get() - 1);
        note.getStyleClass().addAll("track-note", "valid", "not-highlighted");

        StackPane layout = new StackPane();
        layout.setPickOnBounds(false);
        layout.setAlignment(Pos.CENTER_LEFT);
        layout.setTranslateY(noteScaler.scaleY(row * Quantizer.ROW_HEIGHT).get());
        layout.setTranslateX(startX);

        Note trackNote = new Note(
                note,
                new Rectangle(0, 0),
                new Rectangle(0, 0),
                lyricProvider.get(),
                layout,
                null,
                new SimpleBooleanProperty(false),
                new SimpleBooleanProperty(false),
                new SimpleBooleanProperty(false),
                localizer,
                quantizer,
                noteScaler);
        trackNote.getElement().setMouseTransparent(true);
        return trackNote;
    }
}
