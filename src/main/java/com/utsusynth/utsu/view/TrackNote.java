package com.utsusynth.utsu.view;

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
		CLICKING,
		DRAGGING,
		RESIZING,
	}
	private SubMode subMode;
	private int startQuant;
	private int startRow;
	
	static TrackNote createNote(
			QuantizedAddRequest request, TrackNoteCallback callback, Quantizer quantizer) {
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
		
		TrackNote trackNote = new TrackNote(defaultNote, noteEdge, overlap, layout, callback, quantizer);
		trackNote.lyric.setLyric(request.getLyric());
		trackNote.lyric.setAlias(request.getTrueLyric());
		
		trackNote.setLeftMargin(startMargin);
		trackNote.adjustColumnSpan();
		
		return trackNote;
	}

	static TrackNote createDefaultNote(
			int row, int column, TrackNoteCallback callback, Quantizer quantizer) {
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
		
		TrackNote trackNote = new TrackNote(defaultNote, noteEdge, overlap, layout, callback, quantizer);
		
		trackNote.adjustColumnSpan();
		int quant = column * quantizer.getQuant();
		int fullQuant = column * 32;
		trackNote.updateNote(
				fullQuant, COL_WIDTH, row, quant, COL_WIDTH, trackNote.lyric.getLyric());
		return trackNote;
	}
	private TrackNote(
			Rectangle note,
			Rectangle dragEdge,
			Rectangle overlap,
			StackPane layout,
			TrackNoteCallback
			callback,
			Quantizer quantizer) {
		this.note = note;
		this.dragEdge = dragEdge;
		this.overlap = overlap;
		this.track = callback;
		this.subMode = SubMode.CLICKING;
		this.quantizer = quantizer;
		this.lyric = TrackNoteLyric.makeLyric(() -> {
			this.track.setHighlighted(this, false);
		});
		this.layout = layout;
		this.layout.getChildren().addAll(
				this.note, this.overlap, this.lyric.openTextElement(), this.dragEdge);
		layout.setOnMouseClicked((event) -> {
			if (track.getCurrentMode() == Mode.DELETE) {
				if (note.getStyleClass().contains("valid-note")) {
					QuantizedNote request = new QuantizedNote(
							getQuantizedStart(32), getQuantizedDuration(32), 32);
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
			int oldRow = GridPane.getRowIndex(layout);
			int oldQuant = getQuantizedStart();
			int oldFullQuant = getQuantizedStart(32);
			int newRow = ((int) Math.floor(action.getY() / ROW_HEIGHT)) + oldRow;
			
			int quantWidth = COL_WIDTH / quantizer.getQuant();
			int truncatedAbsPosition = getAbsPositionInColumn() / quantWidth * quantWidth;
			double noteX = action.getX() - truncatedAbsPosition;
			int newQuant = ((int) Math.floor(noteX / quantWidth)) + oldQuant;
			if (subMode == SubMode.RESIZING) {
				int absDuration = getDuration();
				int quantizedEnd = oldQuant + getQuantizedDuration();
				if (newQuant > quantizedEnd) {
					resizeNote(absDuration, (getQuantizedDuration() * quantWidth) + quantWidth);
				} else if (getQuantizedDuration() > 1 && newQuant < quantizedEnd - 1) {
					resizeNote(absDuration, (getQuantizedDuration() * quantWidth) - quantWidth);
				}
			} else {
				int oldCol = GridPane.getColumnIndex(layout);
				int newCol = ((int) Math.floor(action.getX() / COL_WIDTH)) + oldCol;
				if (newRow != oldRow && track.isInBounds(newRow)) {
					GridPane.setRowIndex(layout, newRow);
				} else {
					newRow = oldRow;
				}
				if (newCol != oldCol) {
					if (newCol >= 0) {
						GridPane.setColumnIndex(layout, newCol);
					} else {
						newCol = oldCol;
						newQuant = oldQuant;
					}
				}
				if (oldRow != newRow || oldQuant != newQuant) {
					moveNote(oldFullQuant, newRow, newCol, newQuant);
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
				startRow = GridPane.getRowIndex(layout);
				startQuant = GridPane.getColumnIndex(layout) * quantizer.getQuant() +
						(int) event.getX() / (COL_WIDTH / quantizer.getQuant());
				//event.get
				subMode = SubMode.CLICKING;
			}
		});
	}

	StackPane getElement() {
		return layout;
	}
	
	/**
	 * Sets a note's highlighted state.  Should only be called from track.
	 * @param highlighted Whether the note should be highlighted.
	 */
	void setHighlighted(boolean highlighted) {
		note.getStyleClass().set(2, highlighted ? "highlighted" : "not-highlighted");
		
		if (!highlighted) {
			lyric.closeTextFieldIfNeeded((newLyric) -> {
				this.setSongLyric(newLyric);
				return layout;
			});
		}
	}
	
	void setValid(boolean isValid) {
		note.getStyleClass().set(1, isValid ? "valid-note" : "invalid-note");
		if (!isValid) {
			lyric.setAlias(Optional.absent());
			adjustForOverlap(Integer.MAX_VALUE);
		}
	}
	
	private void setSongLyric(String lyric) {
		int curDuration = getDuration();
		updateNote(
				getQuantizedStart(32),
				curDuration,
				GridPane.getRowIndex(layout),
				getQuantizedStart(),
				curDuration,
				lyric);
	}
	
	void adjustForOverlap(int distanceToNextNote) {
		int noteWidth = (int) this.note.getWidth();
		if (noteWidth > distanceToNextNote) {
			overlap.setWidth(noteWidth - distanceToNextNote);
		} else {
			overlap.setWidth(0);
		}
	}
	
	private void resizeNote(int oldDuration, int newDuration) {
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
		updateNote(
				getQuantizedStart(32),
				oldDuration,
				GridPane.getRowIndex(layout),
				getQuantizedStart(),
				newDuration,
				lyric.getLyric());
	}
	
	private void moveNote(int oldFullQuant, int newRow, int newCol, int newQuant) {
		int quantsIntoCol = newQuant - (newCol * quantizer.getQuant());
		int newMargin = quantsIntoCol * (COL_WIDTH / quantizer.getQuant());
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
		updateNote(oldFullQuant, curDuration, newRow, newQuant, curDuration, lyric.getLyric());
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
			int oldFullQuant,
			int oldDuration,
			int newRow,
			int newQuant,
			int newDuration,
			String newLyric) {
		//System.out.println("***");
		if (note.getStyleClass().contains("valid-note")) {
			//System.out.println(String.format(
			//		"Moving from valid %d, %s", oldQuant, lyric.getLyric()));
			int quantOldDuration = oldDuration / (COL_WIDTH / 32);
			QuantizedNote deleteThis = new QuantizedNote(
					oldFullQuant, quantOldDuration, 32);
			track.removeSongNote(deleteThis);
		} else {
			//System.out.println(String.format(
			//		"Moving from invalid %d, %s", oldQuant, lyric.getLyric()));
		}
		//System.out.println(String.format("Moving to %d, %d, %s", newRow, newQuant, newLyric));
		try {
			setValid(true);
			int quantNewDuration = newDuration / (COL_WIDTH / quantizer.getQuant());
			QuantizedNote addThis = new QuantizedNote(
					newQuant, quantNewDuration, quantizer.getQuant());
			Optional<String> trueLyric = 
					track.addSongNote(this, addThis, newRow, newLyric);
			this.lyric.setAlias(trueLyric);
		} catch (NoteAlreadyExistsException e) {
			setValid(false);
			//System.out.println("WARNING: New note is invalid!");
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
	
	private int getQuantizedDuration() {
		return getQuantizedDuration(quantizer.getQuant());
	}
	
	private int getQuantizedDuration(int quantization) {
		return getDuration() / (COL_WIDTH / quantization);
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
