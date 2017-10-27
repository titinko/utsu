package com.utsusynth.utsu.view.note;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.utsusynth.utsu.common.PitchUtils;
import com.utsusynth.utsu.common.quantize.QuantizedAddRequest;
import com.utsusynth.utsu.common.quantize.QuantizedNote;
import com.utsusynth.utsu.common.quantize.Quantizer;
import com.utsusynth.utsu.view.note.portamento.CurveFactory;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

public class TrackNoteFactory {
	private final Quantizer quantizer;
	private final Provider<TrackLyric> lyricProvider;

	@Inject
	public TrackNoteFactory(
			Quantizer quantizer,
			Provider<TrackLyric> lyricProvider,
			CurveFactory curveFactory) {
		this.quantizer = quantizer;
		this.lyricProvider = lyricProvider;
	}

	public TrackNote createNote(QuantizedAddRequest request, TrackNoteCallback callback) {
		QuantizedNote qNote = request.getNote();
		int absStart = qNote.getStart() * (Quantizer.COL_WIDTH / qNote.getQuantization());
		int absDuration = qNote.getDuration() * (Quantizer.COL_WIDTH / qNote.getQuantization());
		Rectangle note = new Rectangle();
		note.setWidth(absDuration - 1);
		note.setHeight(Quantizer.ROW_HEIGHT - 1);
		note.getStyleClass().addAll("track-note", "valid-note", "not-highlighted");

		Rectangle edge = new Rectangle();
		edge.setWidth(2);
		edge.setHeight(note.getHeight());
		edge.getStyleClass().add("drag-edge");

		Rectangle overlap = new Rectangle();
		overlap.setWidth(0);
		overlap.setHeight(note.getHeight());
		overlap.getStyleClass().add("note-overlap");

		StackPane layout = new StackPane();
		layout.setPickOnBounds(false);
		layout.setAlignment(Pos.CENTER_LEFT);
		layout.setTranslateY(PitchUtils.pitchToRowNum(request.getPitch()) * Quantizer.ROW_HEIGHT);
		layout.setTranslateX(absStart);

		TrackLyric lyric = lyricProvider.get();
		TrackVibrato vibrato;
		if (request.getPitchbend().isPresent()) {
			vibrato = new TrackVibrato(request.getPitchbend().get().getVibrato());
		} else {
			vibrato = new TrackVibrato(Optional.absent());
		}

		TrackNote trackNote =
				new TrackNote(note, edge, overlap, lyric, vibrato, layout, callback, quantizer);
		lyric.setVisibleLyric(request.getLyric());
		lyric.setVisibleAlias(request.getTrueLyric());

		return trackNote;
	}

	public TrackNote createDefaultNote(int row, int column, TrackNoteCallback callback) {
		Rectangle note = new Rectangle();
		note.setWidth(Quantizer.COL_WIDTH - 1);
		note.setHeight(Quantizer.ROW_HEIGHT - 1);
		note.getStyleClass().addAll("track-note", "invalid-note", "not-highlighted");

		Rectangle edge = new Rectangle();
		edge.setWidth(2);
		edge.setHeight(note.getHeight());
		edge.getStyleClass().add("drag-edge");
		StackPane.setMargin(edge, new Insets(0, 0, 0, Quantizer.COL_WIDTH - 3));

		Rectangle overlap = new Rectangle();
		overlap.setWidth(0);
		overlap.setHeight(note.getHeight());
		overlap.getStyleClass().add("note-overlap");
		StackPane.setMargin(overlap, new Insets(0, 0, 0, Quantizer.COL_WIDTH - 1));

		StackPane layout = new StackPane();
		layout.setPickOnBounds(false);
		layout.setAlignment(Pos.CENTER_LEFT);
		layout.setTranslateY(row * Quantizer.ROW_HEIGHT);
		layout.setTranslateX(column * Quantizer.COL_WIDTH);

		TrackLyric lyric = lyricProvider.get();
		TrackVibrato vibrato = new TrackVibrato(Optional.absent());

		TrackNote trackNote =
				new TrackNote(note, edge, overlap, lyric, vibrato, layout, callback, quantizer);
		lyric.registerLyric();

		return trackNote;
	}
}
