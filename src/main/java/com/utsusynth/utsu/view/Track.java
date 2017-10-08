package com.utsusynth.utsu.view;

import java.util.List;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.utsusynth.utsu.UtsuController.Mode;
import com.utsusynth.utsu.common.PitchUtils;
import com.utsusynth.utsu.common.exception.NoteAlreadyExistsException;
import com.utsusynth.utsu.common.quantize.QuantizedAddRequest;
import com.utsusynth.utsu.common.quantize.QuantizedAddResponse;
import com.utsusynth.utsu.common.quantize.QuantizedEnvelope;
import com.utsusynth.utsu.common.quantize.QuantizedModifyRequest;
import com.utsusynth.utsu.common.quantize.QuantizedNeighbor;
import com.utsusynth.utsu.common.quantize.QuantizedNote;
import com.utsusynth.utsu.common.quantize.QuantizedPitchbend;
import com.utsusynth.utsu.view.note.TrackEnvelopeCallback;
import com.utsusynth.utsu.view.note.TrackNote;
import com.utsusynth.utsu.view.note.TrackNoteCallback;
import com.utsusynth.utsu.view.note.TrackNoteFactory;
import com.utsusynth.utsu.view.note.TrackPitchbendCallback;

import javafx.scene.Group;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;

public class Track {
	private static final int ROW_HEIGHT = 20;
	private static final int COL_WIDTH = 96;

	private final Highlighter highlighter;
	private final TrackNoteFactory noteFactory;
	private final TrackNoteMap noteMap;

	private GridPane track;
	private GridPane dynamics;
	private int numMeasures;
	private ViewCallback model;

	@Inject
	public Track(Highlighter highlighter, TrackNoteFactory trackNoteFactory, TrackNoteMap noteMap) {
		this.highlighter = highlighter;
		this.noteFactory = trackNoteFactory;
		this.noteMap = noteMap;
	}

	public void initialize(ViewCallback callback) {
		this.model = callback;
	}

	public GridPane createNewTrack(List<QuantizedAddRequest> notes) {
		clearTrack();
		if (notes.isEmpty()) {
			return track;
		}

		// Add as many octaves as needed.
		QuantizedNote lastNote = notes.get(notes.size() - 1).getNote();
		int finalPosition = lastNote.getStart() * (COL_WIDTH / lastNote.getQuantization());
		setNumMeasures((finalPosition / COL_WIDTH / 4) + 4);

		// Add all notes.
		for (QuantizedAddRequest note : notes) {
			TrackNote newNote = noteFactory.createNote(note, noteCallback);
			int position =
					note.getNote().getStart() * (COL_WIDTH / note.getNote().getQuantization());
			try {
				noteMap.putNote(position, newNote);
				if (note.getEnvelope().isPresent()) {
					noteMap.putEnvelope(
							position,
							note.getEnvelope().get(),
							getEnvelopeCallback(position));
				}
				if (note.getPitchbend().isPresent()) {
					noteMap.putPitchbend(
							position,
							note.getPitchbend().get(),
							getPitchbendCallback(position));
				}
			} catch (NoteAlreadyExistsException e) {
				// TODO: Throw an error here?
				System.out.println("UST read found two notes in the same place :(");
			}
			noteMap.addNoteElement(newNote);
		}
		return track;
	}

	public Group getNotesElement() {
		return noteMap.getNotesElement();
	}

	public GridPane getDynamicsElement() {
		if (dynamics == null) {
			// TODO: Handle this;
			System.out.println("Dynamics element is empty!");
		}
		return dynamics;
	}

	public Group getEnvelopesElement() {
		return noteMap.getEnvelopesElement();
	}

	public Group getPitchbendsElement() {
		return noteMap.getPitchbendsElement();
	}

	private void clearTrack() {
		// Remove current track.
		highlighter.clearHighlights();
		noteMap.clear();
		track = new GridPane();
		dynamics = new GridPane();

		numMeasures = 0;
		setNumMeasures(4);
	}

	private void setNumMeasures(int newNumMeasures) {
		if (newNumMeasures < 0) {
			return;
		} else if (newNumMeasures > numMeasures) {
			for (int i = numMeasures; i < newNumMeasures; i++) {
				addMeasure();
			}
		} else if (newNumMeasures == numMeasures) {
			// Nothing needs to be done.
			return;
		} else {
			// Remove measures.
			int desiredNumColumns = newNumMeasures * 4;
			track.getChildren().removeIf((child) -> {
				return GridPane.getColumnIndex(child) >= desiredNumColumns;
			});
			// Remove dynamics columns.
			dynamics.getChildren().removeIf((child) -> {
				return GridPane.getColumnIndex(child) >= desiredNumColumns;
			});
			numMeasures = newNumMeasures;
		}
	}

	private void addMeasure() {
		int rowNum = 0;
		int numColumns = numMeasures * 4;
		for (int octave = 7; octave > 0; octave--) {
			for (String pitch : PitchUtils.REVERSE_PITCHES) {
				// Add row to track.
				for (int colNum = numColumns; colNum < numColumns + 4; colNum++) {
					AnchorPane newCell = new AnchorPane();
					newCell.setPrefSize(COL_WIDTH, ROW_HEIGHT);
					newCell.getStyleClass().add("track-cell");
					newCell.getStyleClass().add(pitch.endsWith("#") ? "black-key" : "white-key");
					if (colNum % 4 == 0) {
						newCell.getStyleClass().add("measure-start");
					} else if ((colNum + 1) % 4 == 0) {
						newCell.getStyleClass().add("measure-end");
					}

					final int currentRowNum = rowNum;
					final int currentColNum = colNum;
					newCell.setOnMouseClicked((event) -> {
						Mode currentMode = model.getCurrentMode();
						if (currentMode == Mode.ADD) {
							// Create note.
							TrackNote newNote = noteFactory
									.createDefaultNote(currentRowNum, currentColNum, noteCallback);
							noteMap.addNoteElement(newNote);
						}
						// Clear highlights regardless of current mode.
						highlighter.clearHighlights();
					});
					track.add(newCell, colNum, rowNum);
				}
				rowNum++;
			}
		}

		// Add new columns to dynamics.
		for (int colNum = numColumns; colNum < numColumns + 4; colNum++) {
			AnchorPane topCell = new AnchorPane();
			topCell.setPrefSize(COL_WIDTH, 50);
			topCell.getStyleClass().add("dynamics-top-cell");
			if (colNum % 4 == 0) {
				topCell.getStyleClass().add("measure-start");
			}
			dynamics.add(topCell, colNum, 0);
			AnchorPane bottomCell = new AnchorPane();
			bottomCell.setPrefSize(COL_WIDTH, 50);
			bottomCell.getStyleClass().add("dynamics-bottom-cell");
			if (colNum % 4 == 0) {
				bottomCell.getStyleClass().add("measure-start");
			}
			dynamics.add(bottomCell, colNum, 1);
		}
		numMeasures++;
	}

	private final TrackNoteCallback noteCallback = new TrackNoteCallback() {
		@Override
		public void setHighlighted(TrackNote note, boolean highlighted) {
			highlighter.clearHighlights();
			if (highlighted) {
				highlighter.addHighlight(note);
			}
		}

		@Override
		public boolean isHighlighted(TrackNote note) {
			return highlighter.isHighlighted(note);
		}

		@Override
		public boolean isInBounds(int rowNum) {
			return rowNum >= 0 && rowNum < 7 * PitchUtils.PITCHES.size();
		}

		@Override
		public Optional<String> addSongNote(
				TrackNote note,
				QuantizedNote toAdd,
				Optional<QuantizedEnvelope> envelope,
				Optional<QuantizedPitchbend> pitchbend,
				int rowNum,
				String lyric) throws NoteAlreadyExistsException {
			int position = toAdd.getStart() * (COL_WIDTH / toAdd.getQuantization());
			if (noteMap.hasNote(position)) {
				throw new NoteAlreadyExistsException();
			} else {
				QuantizedAddRequest request = new QuantizedAddRequest(
						toAdd,
						envelope,
						pitchbend,
						PitchUtils.rowNumToPitch(rowNum),
						lyric,
						Optional.absent());
				QuantizedAddResponse response = model.addNote(request);

				noteMap.putNote(position, note);
				if (response.getPrevNote().isPresent()) {
					QuantizedNeighbor prev = response.getPrevNote().get();
					int prevDelta = prev.getDelta() * (COL_WIDTH / prev.getQuantization());
					noteMap.getNote(position - prevDelta).adjustForOverlap(prevDelta);
					noteMap.putEnvelope(
							position - prevDelta,
							prev.getEnvelope(),
							getEnvelopeCallback(position - prevDelta));
				}
				if (response.getNextNote().isPresent()) {
					QuantizedNeighbor next = response.getNextNote().get();
					int nextDelta = next.getDelta() * (COL_WIDTH / next.getQuantization());
					note.adjustForOverlap(nextDelta);
					noteMap.putEnvelope(
							position + nextDelta,
							next.getEnvelope(),
							getEnvelopeCallback(position + nextDelta));
					if (next.getPitchbend().isPresent()) {
						noteMap.putPitchbend(
								position + nextDelta,
								next.getPitchbend().get(),
								getPitchbendCallback(position + nextDelta));
					}
				}
				// Add envelope after adjusting note for overlap.
				Optional<QuantizedEnvelope> newEnvelope = response.getEnvelope();
				if (newEnvelope.isPresent()) {
					noteMap.putEnvelope(position, newEnvelope.get(), getEnvelopeCallback(position));
				}
				if (response.getPitchbend().isPresent()) {
					noteMap.putPitchbend(
							position,
							response.getPitchbend().get(),
							getPitchbendCallback(position));
				}

				// Add measures if necessary.
				if (!response.getNextNote().isPresent()) {
					setNumMeasures((position / COL_WIDTH / 4) + 4);
				}

				return response.getTrueLyric();
			}
		}

		@Override
		public void removeSongNote(QuantizedNote toRemove) {
			int position = toRemove.getStart() * (COL_WIDTH / toRemove.getQuantization());
			noteMap.removeFullNote(position);
			QuantizedAddResponse response = model.removeNote(toRemove);
			if (response.getPrevNote().isPresent()) {
				QuantizedNeighbor prev = response.getPrevNote().get();
				int prevDelta = prev.getDelta() * (COL_WIDTH / prev.getQuantization());
				TrackNote prevNode = noteMap.getNote(position - prevDelta);
				if (response.getNextNote().isPresent()) {
					QuantizedNeighbor next = response.getNextNote().get();
					int nextDelta = next.getDelta() * (COL_WIDTH / next.getQuantization());
					prevNode.adjustForOverlap(prevDelta + nextDelta);
					noteMap.putEnvelope(
							position + nextDelta,
							next.getEnvelope(),
							getEnvelopeCallback(position + nextDelta));
					if (next.getPitchbend().isPresent()) {
						noteMap.putPitchbend(
								position + nextDelta,
								next.getPitchbend().get(),
								getPitchbendCallback(position + nextDelta));
					}
				} else {
					prevNode.adjustForOverlap(Integer.MAX_VALUE);
					// Remove measures until you have 4 measures + previous note.
					setNumMeasures(((position - prevDelta) / COL_WIDTH / 4) + 4);
				}
				noteMap.putEnvelope(
						position - prevDelta,
						prev.getEnvelope(),
						getEnvelopeCallback(position - prevDelta));
			}

			// Remove all measures if necessary.
			if (noteMap.isEmpty()) {
				setNumMeasures(4);
			}
		}

		@Override
		public void removeTrackNote(TrackNote trackNote) {
			highlighter.clearHighlights();
			noteMap.removeNoteElement(trackNote);
		}

		@Override
		public Mode getCurrentMode() {
			return model.getCurrentMode();
		}

		@Override
		public Optional<QuantizedEnvelope> getEnvelope(QuantizedNote note) {
			int position = note.getStart() * (COL_WIDTH / note.getQuantization());
			if (noteMap.hasEnvelope(position)) {
				return Optional.of(noteMap.getEnvelope(position).getQuantizedEnvelope());
			}
			return Optional.absent();
		}

		@Override
		public Optional<QuantizedPitchbend> getPitchbend(QuantizedNote note) {
			int position = note.getStart() * (COL_WIDTH / note.getQuantization());
			if (noteMap.hasPitchbend(position)) {
				return Optional.of(noteMap.getPitchbend(position).getQuantizedPitchbend(position));
			}
			return Optional.absent();
		}
	};

	private TrackEnvelopeCallback getEnvelopeCallback(final int position) {
		return new TrackEnvelopeCallback() {
			@Override
			public void modifySongEnvelope(QuantizedEnvelope envelope) {
				QuantizedNote note = noteMap.getNote(position).getQuantizedNote();
				model.modifyNote(new QuantizedModifyRequest(note, envelope));
			}
		};
	}

	private TrackPitchbendCallback getPitchbendCallback(final int position) {
		return new TrackPitchbendCallback() {
			@Override
			public void modifySongPitchbend() {
				QuantizedNote note = noteMap.getNote(position).getQuantizedNote();
				QuantizedPitchbend qPitchbend =
						noteMap.getPitchbend(position).getQuantizedPitchbend(position);
				model.modifyNote(new QuantizedModifyRequest(note, qPitchbend));
			}
		};
	}
}
