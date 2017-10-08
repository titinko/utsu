package com.utsusynth.utsu.view.note;

import com.google.common.base.Optional;
import com.utsusynth.utsu.UtsuController.Mode;
import com.utsusynth.utsu.common.exception.NoteAlreadyExistsException;
import com.utsusynth.utsu.common.quantize.QuantizedEnvelope;
import com.utsusynth.utsu.common.quantize.QuantizedNote;
import com.utsusynth.utsu.common.quantize.QuantizedPitchbend;
import com.utsusynth.utsu.common.quantize.Quantizer;

import javafx.geometry.Insets;
import javafx.scene.Cursor;
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
	private final TrackLyric lyric;
	private final Quantizer quantizer;

	// Temporary cache values.
	private enum SubMode {
		CLICKING, DRAGGING, RESIZING,
	}

	private SubMode subMode;
	private int quantInNote; // Always uses Quantizer.MAX

	TrackNote(
			Rectangle note,
			Rectangle dragEdge,
			Rectangle overlap,
			TrackLyric lyric,
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
		this.lyric = lyric;
		this.layout = layout;
		this.layout.getChildren().addAll(
				this.note,
				this.overlap,
				this.lyric.getElement(),
				this.dragEdge);

		TrackNote thisNote = this;
		lyric.initialize(new TrackLyricCallback() {
			@Override
			public void setHighlighted(boolean highlighted) {
				callback.setHighlighted(thisNote, false);
			}

			@Override
			public void setSongLyric(String newLyric) {
				thisNote.updateNote(
						thisNote.getQuantizedStart(Quantizer.SMALLEST),
						thisNote.getQuantizedStart(Quantizer.SMALLEST),
						Quantizer.SMALLEST,
						thisNote.getDuration(),
						thisNote.getRow(),
						thisNote.getDuration(),
						newLyric);
			}

			@Override
			public void adjustColumnSpan() {
				// TODO: Factor lyric width into this.
				thisNote.adjustDragEdge(thisNote.getDuration());
			}
		});
		layout.setOnMouseClicked((event) -> {
			if (track.getCurrentMode() == Mode.DELETE) {
				if (note.getStyleClass().contains("valid-note")) {
					track.removeSongNote(getQuantizedNote());
				}
				track.removeTrackNote(this);
			} else if (subMode == SubMode.CLICKING) {
				if (this.track.isHighlighted(this)) {
					this.lyric.openTextField();
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
				int newQuant = (int) Math.floor((action.getX() + getAbsPosition()) / quantSize);

				// Find what to compare quantized mouse position to.
				int oldEndPos = getAbsPosition() + getDuration();
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
				int oldRow = getRow();
				int newRow = ((int) Math.floor(action.getY() / ROW_HEIGHT)) + oldRow;
				if (!track.isInBounds(newRow)) {
					newRow = oldRow;
				}

				// Handle horizontal movement.
				int curQuant = quantizer.getQuant(); // Ensure constant quantization.
				int curQuantSize = COL_WIDTH / curQuant;
				// Determine whether a note is aligned with the current quantization.
				boolean aligned = getAbsPosition() % curQuantSize == 0;
				int oldQuantInNote = quantInNote / (Quantizer.SMALLEST / curQuant);
				int newQuantInNote = (int) Math.floor(action.getX() / curQuantSize);
				int quantChange = newQuantInNote - oldQuantInNote;
				if (!aligned) {
					// Possibly increase quantChange by 1.
					int minBound = getDuration();
					int ceilQuantDur = (int) Math.ceil(getDuration() * 1.0 / curQuantSize);
					if (action.getX() > minBound && newQuantInNote < ceilQuantDur) {
						quantChange++;
					}
					// Convert to smallest quantization.
					quantChange *= (Quantizer.SMALLEST / curQuant);
					// Both values are in the smallest quantization.
					int truncatedStart = getAbsPosition() / curQuantSize * (32 / curQuant);
					int actualStart = getAbsPosition() / (COL_WIDTH / 32);
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
				if (newQuant < 0) {
					newQuant = oldQuant;
				}

				// Actual movement.
				if (oldRow != newRow || oldQuant != newQuant) {
					moveNote(oldQuant, newQuant, curQuant, newRow);
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
				quantInNote = (int) event.getX() / (COL_WIDTH / 32);
				subMode = SubMode.CLICKING;
			}
		});
	}

	public StackPane getElement() {
		return layout;
	}

	public QuantizedNote getQuantizedNote() {
		int quantization = Quantizer.SMALLEST;
		int quantizedDuration =
				(int) ((getDuration() - overlap.getWidth()) / (COL_WIDTH / quantization));
		return new QuantizedNote(getQuantizedStart(quantization), quantizedDuration, quantization);
	}

	public int getRow() {
		return (int) layout.getTranslateY() / ROW_HEIGHT;
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
			lyric.closeTextFieldIfNeeded();
		}
	}

	public void setValid(boolean isValid) {
		note.getStyleClass().set(1, isValid ? "valid-note" : "invalid-note");
		if (!isValid) {
			lyric.setVisibleAlias(Optional.absent());
			adjustForOverlap(Integer.MAX_VALUE);
		}
	}

	public void adjustForOverlap(int distanceToNextNote) {
		int noteWidth = (int) this.note.getWidth();
		if (noteWidth > distanceToNextNote) {
			overlap.setWidth(noteWidth - distanceToNextNote);
		} else {
			overlap.setWidth(0);
		}
		adjustDragEdge(getDuration());
	}

	private void resizeNote(int newDuration) {
		note.setWidth(newDuration - 1);
		adjustDragEdge(newDuration);
		updateNote(
				getQuantizedStart(32),
				getQuantizedStart(32),
				32,
				getDuration(),
				getRow(),
				newDuration,
				lyric.getLyric());
	}

	private void moveNote(int oldQuant, int newQuant, int quantization, int newRow) {
		layout.setTranslateX(newQuant * (COL_WIDTH / quantization));
		layout.setTranslateY(newRow * ROW_HEIGHT);
		int curDuration = getDuration();
		adjustDragEdge(curDuration);
		updateNote(
				oldQuant,
				newQuant,
				quantization,
				curDuration,
				newRow,
				curDuration,
				lyric.getLyric());
	}

	private void adjustDragEdge(double newDuration) {
		StackPane.setMargin(dragEdge, new Insets(0, 0, 0, newDuration - dragEdge.getWidth() - 1));
		StackPane.setMargin(overlap, new Insets(0, 0, 0, newDuration - overlap.getWidth() - 1));
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
		Optional<QuantizedEnvelope> envelope = Optional.absent();
		Optional<QuantizedPitchbend> pitchbend = Optional.absent();
		if (note.getStyleClass().contains("valid-note")) {
			// System.out.println(String.format(
			// "Moving from valid %d, %s", oldQuant, lyric.getLyric()));
			int quantOldDuration = oldDuration / (COL_WIDTH / quantization);
			QuantizedNote deleteThis = new QuantizedNote(oldQuant, quantOldDuration, quantization);
			envelope = track.getEnvelope(deleteThis);
			pitchbend = track.getPitchbend(deleteThis);
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
			Optional<String> trueLyric =
					track.addSongNote(this, addThis, envelope, pitchbend, newRow, newLyric);
			this.lyric.setVisibleAlias(trueLyric);
		} catch (NoteAlreadyExistsException e) {
			setValid(false);
			// System.out.println("WARNING: New note is invalid!");
		}
	}

	private int getQuantizedStart() {
		return getQuantizedStart(quantizer.getQuant());
	}

	private int getQuantizedStart(int quantization) {
		return getAbsPosition() / (COL_WIDTH / quantization);
	}

	private int getAbsPosition() {
		return (int) layout.getTranslateX();
	}

	private int getDuration() {
		return (int) note.getWidth() + 1;
	}
}
