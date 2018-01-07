package com.utsusynth.utsu.view.note;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.utsusynth.utsu.common.PitchUtils;
import com.utsusynth.utsu.common.data.NoteData;
import com.utsusynth.utsu.common.quantize.Quantizer;
import com.utsusynth.utsu.common.quantize.Scaler;
import com.utsusynth.utsu.view.note.portamento.CurveFactory;

import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

public class TrackNoteFactory {
	private final Scaler scaler;
	private final Quantizer quantizer;
	private final Provider<TrackLyric> lyricProvider;

	@Inject
	public TrackNoteFactory(
			Scaler scaler,
			Quantizer quantizer,
			Provider<TrackLyric> lyricProvider,
			CurveFactory curveFactory) {
		this.scaler = scaler;
		this.quantizer = quantizer;
		this.lyricProvider = lyricProvider;
	}

	public TrackNote createNote(NoteData note, TrackNoteCallback callback) {
		int absStart = note.getPosition();
		int absDuration = note.getDuration();
		Rectangle rect = new Rectangle();
		rect.setWidth(scaler.scaleX(absDuration) - 1);
		rect.setHeight(scaler.scaleY(Quantizer.ROW_HEIGHT) - 1);
		rect.getStyleClass().addAll("track-note", "valid-note", "not-highlighted");

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

		TrackLyric lyric = lyricProvider.get();
		TrackVibrato vibrato;
		if (note.getPitchbend().isPresent()) {
			vibrato = new TrackVibrato(Optional.of(note.getPitchbend().get().getVibrato()));
		} else {
			vibrato = new TrackVibrato(Optional.absent());
		}

		TrackNote trackNote = new TrackNote(
				rect,
				edge,
				overlap,
				lyric,
				vibrato,
				layout,
				callback,
				quantizer,
				scaler);
		lyric.setVisibleLyric(note.getLyric());
		if (note.getConfig().isPresent()) {
			lyric.setVisibleAlias(note.getConfig().get().getTrueLyric());
		}

		return trackNote;
	}

	public TrackNote createDefaultNote(int row, int column, TrackNoteCallback callback) {
		Rectangle note = new Rectangle();
		note.setWidth(scaler.scaleX(Quantizer.COL_WIDTH) - 1);
		note.setHeight(scaler.scaleY(Quantizer.ROW_HEIGHT) - 1);
		note.getStyleClass().addAll("track-note", "invalid-note", "not-highlighted");

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

		TrackLyric lyric = lyricProvider.get();
		TrackVibrato vibrato = new TrackVibrato(Optional.absent());

		TrackNote trackNote = new TrackNote(
				note,
				edge,
				overlap,
				lyric,
				vibrato,
				layout,
				callback,
				quantizer,
				scaler);
		lyric.registerLyric();

		return trackNote;
	}
}
