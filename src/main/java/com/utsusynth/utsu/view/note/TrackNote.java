package com.utsusynth.utsu.view.note;

import com.google.common.base.Optional;
import com.utsusynth.utsu.UtsuController.Mode;
import com.utsusynth.utsu.common.PitchUtils;
import com.utsusynth.utsu.common.QuantizedAddRequest;
import com.utsusynth.utsu.common.QuantizedNote;
import com.utsusynth.utsu.common.Quantizer;
import com.utsusynth.utsu.common.exception.NoteAlreadyExistsException;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

public class TrackNote {
	private static final int ROW_HEIGHT = 20;
	private static final int COL_WIDTH = 96;

	private final StackPane layout;
	private final Rectangle note;
	private final Rectangle dragEdge;
	private final Rectangle overlap;
	private final TrackNoteCallback track;
	private final TrackNoteLyric lyric;
	private final Quantizer quantizer;

	// Temporary cache values.
	private enum SubMode {
		CLICKING, DRAGGING, RESIZING,
	}

	private SubMode subMode;
	private int quantInNote; // Always uses Quantizer.MAX

	public static TrackNote createNote(
			QuantizedAddRequest request,
			TrackNoteCallback callback,
			Quantizer quantizer) {
		QuantizedNote qNote = request.getNote();
		int absStart = qNote.getStart() * (COL_WIDTH / qNote.getQuantization());
		int startCol = absStart / COL_WIDTH;
		int startMargin = absStart % COL_WIDTH;
		int absDuration = qNote.getDuration() * (COL_WIDTH / qNote.getQuantization());
		Rectangle defaultNote = new Rectangle();
		defaultNote.setWidth(absDuration - 1);
		defaultNote.setHeight(ROW_HEIGHT - 1);
		defaultNote.getStyleClass().addAll("track-note", "valid-note", "not-highlighted");

		Rectangle noteEdge = new Rectangle();
		StackPane.setAlignment(noteEdge, Pos.CENTER_RIGHT);
		noteEdge.setWidth(2);
		noteEdge.setHeight(defaultNote.getHeight());
		noteEdge.getStyleClass().add("drag-edge");

		Rectangle overlap = new Rectangle();
		StackPane.setAlignment(overlap, Pos.CENTER_RIGHT);
		overlap.setWidth(0);
		overlap.setHeight(defaultNote.getHeight());
		overlap.getStyleClass().add("drag-edge");

		StackPane layout = new StackPane();
		layout.setPickOnBounds(false);
		layout.setAlignment(Pos.CENTER_LEFT);
		GridPane.setRowIndex(layout, PitchUtils.pitchToRowNum(request.getPitch()));
		GridPane.setColumnIndex(layout, startCol);

		TrackNote trackNote =
				new TrackNote(defaultNote, noteEdge, overlap, layout, callback, quantizer);
		trackNote.lyric.setLyric(request.getLyric());
		trackNote.lyric.setAlias(request.getTrueLyric());

		trackNote.setLeftMargin(startMargin);
		trackNote.adjustColumnSpan();

		return trackNote;
	}

	public static TrackNote createDefaultNote(
			int row,
			int column,
			TrackNoteCallback callback,
			Quantizer quantizer) {
		Rectangle defaultNote = new Rectangle();
		defaultNote.setWidth(COL_WIDTH - 1);
		defaultNote.setHeight(ROW_HEIGHT - 1);
		defaultNote.getStyleClass().addAll("track-note", "invalid-note", "not-highlighted");

		Rectangle noteEdge = new Rectangle();
		StackPane.setAlignment(noteEdge, Pos.CENTER_RIGHT);
		noteEdge.setWidth(2);
		noteEdge.setHeight(defaultNote.getHeight());
		noteEdge.getStyleClass().add("drag-edge");

		Rectangle overlap = new Rectangle();
		StackPane.setAlignment(overlap, Pos.CENTER_RIGHT);
		overlap.setWidth(0);
		overlap.setHeight(defaultNote.getHeight());
		overlap.getStyleClass().add("drag-edge");

		StackPane layout = new StackPane();
		layout.setPickOnBounds(false);
		layout.setAlignment(Pos.CENTER_LEFT);
		GridPane.setRowIndex(layout, row);
		GridPane.setColumnIndex(layout, column);

		TrackNote trackNote =
				new TrackNote(defaultNote, noteEdge, overlap, layout, callback, quantizer);

		trackNote.adjustColumnSpan();
		int quant = column * quantizer.getQuant();
		trackNote.updateNote(quant, quant, quantizer.getQuant(), COL_WIDTH, row, COL_WIDTH,
				trackNote.lyric.getLyric());
		return trackNote;
	}

	private TrackNote(
			Rectangle note,
			Rectangle dragEdge,
			Rectangle overlap,
			StackPane layout,
			TrackNoteCallback callback,
			Quantizer quantizer) {
		this.note = note;
		this.dragEdge = dragEdge;
		this.overlap = overlap;
		this.track = callback;
		this.subMode = SubMode.CLICKING;
		this.quantInNote = 0;
		this.quantizer = quantizer;
		this.lyric = TrackNoteLyric.makeLyric(() -> {
			this.track.setHighlighted(this, false);
		});
		this.layout = layout;
		this.layout.getChildren().addAll(this.note, this.overlap, this.lyric.openTextElement(),
				this.dragEdge);
		layout.setOnMouseClicked((event) -> {
			if (track.getCurrentMode() == Mode.DELETE) {
				if (note.getStyleClass().contains("valid-note")) {
					int quantization = Quantizer.SMALLEST;
					int quantizedDuration = getDuration() / (COL_WIDTH / quantization);
					QuantizedNote request = new QuantizedNote(getQuantizedStart(quantization),
							quantizedDuration, quantization);
					track.removeSongNote(request);
				}
				track.removeTrackNote(this);
			} else if (subMode == SubMode.CLICKING) {
				if (this.track.isHighlighted(this)) {
					this.lyric.openTextField(this.layout);
				} else {
					this.track.setHighlighted(this, true);
				}
			}
			subMode = SubMode.CLICKING;
		});
		layout.setOnMouseDragged((action) -> {
			if (subMode == SubMode.RESIZING) {
				// Find quantized mouse position.
				int quantSize = COL_WIDTH / quantizer.getQuant();
				int absStart = GridPane.getColumnIndex(layout) * COL_WIDTH;
				int newQuant = (int) Math.floor((action.getX() + absStart) / quantSize);

				// Find what to compare quantized mouse position to.
				int oldEndPos = absStart + getAbsPositionInColumn() + getDuration();
				int increasingQuantEnd = (int) Math.floor(oldEndPos * 1.0 / quantSize);
				int decreasingQuantEnd = (int) (Math.ceil(oldEndPos * 1.0 / quantSize)) - 1;

				// Use smallest quantization to calculate actual duration change.
				int oldSmallQuant = getQuantizedStart(Quantizer.SMALLEST);
				int newSmallQuant = newQuant * (Quantizer.SMALLEST / quantizer.getQuant());
				int quantChange = newSmallQuant - oldSmallQuant;
				int smallQuantSize = COL_WIDTH / Quantizer.SMALLEST;

				// Increase or decrease duration.
				if (newQuant > increasingQuantEnd) {
					resizeNote(quantChange * smallQuantSize);
				} else if (newQuant >= getQuantizedStart() && newQuant < decreasingQuantEnd) {
					resizeNote(quantChange * smallQuantSize + quantSize);
				}
			} else {
				// Handle vertical movement and check against row bounds.
				int oldRow = GridPane.getRowIndex(layout);
				int newRow = ((int) Math.floor(action.getY() / ROW_HEIGHT)) + oldRow;
				if (!track.isInBounds(newRow)) {
					newRow = oldRow;
				}

				// Handle horizontal movement.
				int curQuant = quantizer.getQuant(); // Ensure constant quantization.
				int curQuantSize = COL_WIDTH / curQuant;
				// Determine whether a note is aligned with the current quantization.
				boolean aligned = getAbsPositionInColumn() % curQuantSize == 0;
				int oldQuantInNote = quantInNote / (Quantizer.SMALLEST / curQuant);
				int newQuantInNote =
						(int) Math.floor((action.getX() - getAbsPositionInColumn()) / curQuantSize);
				int quantChange = newQuantInNote - oldQuantInNote;
				if (!aligned) {
					// Possibly increase quantChange by 1.
					int minBound = getAbsPositionInColumn() + getDuration();
					int ceilQuantDur = (int) Math.ceil(getDuration() * 1.0 / curQuantSize);
					if (action.getX() > minBound && newQuantInNote < ceilQuantDur) {
						quantChange++;
					}
					// Convert to smallest quantization.
					quantChange *= (Quantizer.SMALLEST / curQuant);
					// Both values are in the smallest quantization.
					int truncatedStart = getAbsPositionInColumn() / curQuantSize * (32 / curQuant);
					int actualStart = getAbsPositionInColumn() / (COL_WIDTH / 32);
					// Align start quant with true quantization.
					if (quantChange > 0) {
						// Subtract from quantChange.
						quantChange -= (actualStart - truncatedStart);
					} else if (quantChange < 0) {
						// Add to quantChange.
						quantChange += (truncatedStart + Quantizer.SMALLEST - actualStart);
					}
					// Adjust curQuant now that quantChange has been corrected.
					curQuant = Quantizer.SMALLEST;
					curQuantSize = COL_WIDTH / Quantizer.SMALLEST;
				}
				int oldQuant = getQuantizedStart(curQuant);
				int newQuant = oldQuant + quantChange;

				// Check column bounds.
				int newCol = (int) Math.floor(newQuant * curQuantSize * 1.0 / COL_WIDTH);
				if (newCol < 0) {
					newCol = GridPane.getColumnIndex(layout);
					newQuant = oldQuant;
				}

				// Actual movement.
				if (oldRow != newRow || oldQuant != newQuant) {
					moveNote(oldQuant, newQuant, curQuant, newRow, newCol);
				}
				subMode = SubMode.DRAGGING;
			}
		});
		dragEdge.setOnMouseEntered((event) -> {
			dragEdge.getScene().setCursor(Cursor.W_RESIZE);
		});
		dragEdge.setOnMouseExited((event) -> {
			dragEdge.getScene().setCursor(Cursor.DEFAULT);
		});
		layout.setOnMousePressed((event) -> {
			if (layout.getScene().getCursor() == Cursor.W_RESIZE) {
				subMode = SubMode.RESIZING;
			} else {
				// Note that this may become dragging in the future.
				quantInNote = GridPane.getColumnIndex(layout) * 32
						+ (int) event.getX() / (COL_WIDTH / 32) - getQuantizedStart(32);
				subMode = SubMode.CLICKING;
			}
		});
	}

	public StackPane getElement() {
		return layout;
	}

	/**
	 * Sets a note's highlighted state. Should only be called from track.
	 * 
	 * @param highlighted
	 *            Whether the note should be highlighted.
	 */
	public void setHighlighted(boolean highlighted) {
		note.getStyleClass().set(2, highlighted ? "highlighted" : "not-highlighted");

		if (!highlighted) {
			lyric.closeTextFieldIfNeeded((newLyric) -> {
				this.setSongLyric(newLyric);
				return layout;
			});
		}
	}

	public void setValid(boolean isValid) {
		note.getStyleClass().set(1, isValid ? "valid-note" : "invalid-note");
		if (!isValid) {
			lyric.setAlias(Optional.absent());
			adjustForOverlap(Integer.MAX_VALUE);
		}
	}

	private void setSongLyric(String lyric) {
		int curDuration = getDuration();
		updateNote(getQuantizedStart(), getQuantizedStart(), quantizer.getQuant(), curDuration,
				GridPane.getRowIndex(layout), curDuration, lyric);
	}

	public void adjustForOverlap(int distanceToNextNote) {
		int noteWidth = (int) this.note.getWidth();
		if (noteWidth > distanceToNextNote) {
			overlap.setWidth(noteWidth - distanceToNextNote);
		} else {
			overlap.setWidth(0);
		}
	}

	private void resizeNote(int newDuration) {
		note.setWidth(newDuration - 1);
		double totalWidth = getAbsPositionInColumn() + newDuration;
		double lyricWidth = getAbsPositionInColumn() + lyric.getWidth();
		int newColumnSpan = (int) (Math.ceil(totalWidth / COL_WIDTH));
		if (lyricWidth > newColumnSpan * COL_WIDTH) {
			// Fixes bug where hanging text extends the width of a column.
			newColumnSpan = (int) Math.ceil((lyricWidth / COL_WIDTH));
		}
		GridPane.setColumnSpan(layout, newColumnSpan);
		adjustDragEdge(totalWidth, newColumnSpan);
		updateNote(getQuantizedStart(32), getQuantizedStart(32), 32, getDuration(),
				GridPane.getRowIndex(layout), newDuration, lyric.getLyric());
	}

	private void moveNote(int oldQuant, int newQuant, int quantization, int newRow, int newCol) {
		GridPane.setRowIndex(layout, newRow);
		GridPane.setColumnIndex(layout, newCol);
		int quantsIntoCol = newQuant - (newCol * quantization);
		int newMargin = quantsIntoCol * (COL_WIDTH / quantization);
		setLeftMargin(newMargin);
		int curDuration = getDuration();
		double totalWidth = newMargin + curDuration;
		double lyricWidth = newMargin + lyric.getWidth();
		int newColumnSpan = (int) (Math.ceil(totalWidth / COL_WIDTH));
		if (lyricWidth > newColumnSpan * COL_WIDTH) {
			// Fixes bug where hanging text extends the width of a column.
			newColumnSpan = (int) Math.ceil((lyricWidth / COL_WIDTH));
		}
		GridPane.setColumnSpan(layout, newColumnSpan);
		adjustDragEdge(totalWidth, newColumnSpan);
		updateNote(oldQuant, newQuant, quantization, curDuration, newRow, curDuration,
				lyric.getLyric());
	}

	private void setLeftMargin(int newMargin) {
		StackPane.setMargin(note, new Insets(0, 0, 0, newMargin));
		lyric.setLeftMargin(newMargin);
	}

	private void adjustColumnSpan() {
		adjustColumnSpan(getAbsPositionInColumn(), getDuration());
	}

	private void adjustColumnSpan(double newMargin, double newDuration) {
		double totalWidth = newMargin + newDuration;
		double lyricWidth = newMargin + lyric.getWidth();
		int newColumnSpan = (int) (Math.ceil(totalWidth / COL_WIDTH));
		if (lyric.getWidth() <= 0) {
			newColumnSpan += 3;
		} else if (lyricWidth > newColumnSpan * COL_WIDTH) {
			// Corrects for case where hanging text extends the width of a column.
			newColumnSpan = (int) Math.ceil((lyricWidth / COL_WIDTH));
		}
		GridPane.setColumnSpan(layout, newColumnSpan);
		adjustDragEdge(totalWidth, newColumnSpan);
	}

	private void adjustDragEdge(double totalWidth, int columnSpan) {
		int amountToAdjust = (columnSpan * COL_WIDTH) - (int) totalWidth;
		StackPane.setMargin(dragEdge, new Insets(0, amountToAdjust, 0, 0));
		StackPane.setMargin(overlap, new Insets(0, amountToAdjust, 0, 0));
	}

	private void updateNote(
			int oldQuant,
			int newQuant,
			int quantization,
			int oldDuration,
			int newRow,
			int newDuration,
			String newLyric) {
		// System.out.println("***");
		if (note.getStyleClass().contains("valid-note")) {
			// System.out.println(String.format(
			// "Moving from valid %d, %s", oldQuant, lyric.getLyric()));
			int quantOldDuration = oldDuration / (COL_WIDTH / quantization);
			QuantizedNote deleteThis = new QuantizedNote(oldQuant, quantOldDuration, quantization);
			track.removeSongNote(deleteThis);
		} else {
			// System.out.println(String.format(
			// "Moving from invalid %d, %s", oldQuant, lyric.getLyric()));
		}
		// System.out.println(String.format("Moving to %d, %d, %s", newRow, newQuant, newLyric));
		try {
			setValid(true);
			int quantNewDuration = newDuration / (COL_WIDTH / quantization);
			QuantizedNote addThis = new QuantizedNote(newQuant, quantNewDuration, quantization);
			Optional<String> trueLyric = track.addSongNote(this, addThis, newRow, newLyric);
			this.lyric.setAlias(trueLyric);
		} catch (NoteAlreadyExistsException e) {
			setValid(false);
			// System.out.println("WARNING: New note is invalid!");
		}
	}

	private int getQuantizedStart() {
		return getQuantizedStart(quantizer.getQuant());
	}

	private int getQuantizedStart(int quantization) {
		int absColStart = GridPane.getColumnIndex(layout) * COL_WIDTH;
		int absPosition = absColStart + getAbsPositionInColumn();
		return absPosition / (COL_WIDTH / quantization);
	}

	private int getDuration() {
		return (int) note.getWidth() + 1;
	}

	private int getAbsPositionInColumn() {
		Insets curMargin = StackPane.getMargin(note);
		int absPositionInColumn = 0;
		if (curMargin != null) {
			absPositionInColumn = (int) curMargin.getLeft();
		}
		return absPositionInColumn;
	}
}
