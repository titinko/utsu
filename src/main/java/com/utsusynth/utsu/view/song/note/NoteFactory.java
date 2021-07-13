package com.utsusynth.utsu.view.song.note;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.utsusynth.utsu.common.data.NoteData;
import com.utsusynth.utsu.common.data.NoteUpdateData;
import com.utsusynth.utsu.common.i18n.Localizer;
import com.utsusynth.utsu.common.quantize.Quantizer;
import com.utsusynth.utsu.common.quantize.Scaler;
import com.utsusynth.utsu.common.utils.PitchUtils;
import com.utsusynth.utsu.view.song.note.lyric.Lyric;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

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

        Lyric lyric = lyricProvider.get();
        Note trackNote = new Note(
                PitchUtils.pitchToRowNum(note.getPitch()),
                scaler.scalePos(absStart).get(),
                scaler.scaleX(absDuration).get() - 1,
                lyric,
                callback,
                vibratoEditor,
                showLyrics,
                showAliases,
                localizer,
                quantizer,
                scaler);
        trackNote.setValid(true);
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
        Lyric lyric = lyricProvider.get();
        Note trackNote = new Note(
                row,
                scaler.scalePos(positionMs).get(),
                scaler.scaleX(durationMs).get() - 1,
                lyric,
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

    /** Create a note to appear in the background of the bulk editor. */
    public Note createBackgroundNote(int row, double startX, double widthX, Scaler noteScaler) {
        // Should remove drag edge.
        Note trackNote = new Note(
                row,
                startX,
                widthX,
                lyricProvider.get(),
                null,
                new SimpleBooleanProperty(false),
                new SimpleBooleanProperty(false),
                new SimpleBooleanProperty(false),
                localizer,
                quantizer,
                noteScaler);
        trackNote.setToDisplayOnly();
        trackNote.setCroppingEnabled(false);
        trackNote.setValid(true);
        return trackNote;
    }
}
