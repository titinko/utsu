package com.utsusynth.utsu.view;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.utsusynth.utsu.UtsuController.Mode;
import com.utsusynth.utsu.common.PitchUtils;
import com.utsusynth.utsu.common.QuantizedAddRequest;
import com.utsusynth.utsu.common.QuantizedAddResponse;
import com.utsusynth.utsu.common.QuantizedNeighbor;
import com.utsusynth.utsu.common.QuantizedNote;
import com.utsusynth.utsu.common.exception.NoteAlreadyExistsException;
import com.utsusynth.utsu.view.note.TrackCallback;
import com.utsusynth.utsu.view.note.TrackNote;
import com.utsusynth.utsu.view.note.TrackNoteFactory;

import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;

public class Track {
	private static final int ROW_HEIGHT = 20;
	private static final int COL_WIDTH = 96;

	private final Highlighter highlighter;
	private final TrackNoteFactory trackNoteFactory;

	private GridPane track;
	private int numMeasures;
	private ViewCallback model;

	// Maps absolute position to track note's data.
	private Map<Integer, TrackNote> childMap;

	@Inject
	public Track(Highlighter highlighter, TrackNoteFactory trackNoteFactory) {
		this.highlighter = highlighter;
		this.trackNoteFactory = trackNoteFactory;
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
			TrackNote newNote = trackNoteFactory.createNote(note, noteCallback);
			int position =
					note.getNote().getStart() * (COL_WIDTH / note.getNote().getQuantization());
			if (!childMap.containsKey(position)) {
				childMap.put(position, newNote);
			} else {
				// TODO: Throw an error here?
				System.out.println("UST read found two notes in the same place :(");
			}
			track.getChildren().add(newNote.getElement());
		}
		return track;
	}

	public GridPane getElement() {
		if (track == null) {
			// TODO: Handler this;
			System.out.println("Track element is empty!");
		}
		return track;
	}

	private void clearTrack() {
		// Remove current track.
		highlighter.clearHighlights();
		childMap = new HashMap<>();
		track = new GridPane();

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
							TrackNote newNote = trackNoteFactory
									.createDefaultNote(currentRowNum, currentColNum, noteCallback);
							track.getChildren().add(newNote.getElement());
						}
						// Clear highlights regardless of current mode.
						highlighter.clearHighlights();
					});
					track.add(newCell, colNum, rowNum);
				}
				rowNum++;
			}
		}
		numMeasures++;
	}

	private final TrackCallback noteCallback = new TrackCallback() {
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
				int rowNum,
				String lyric) throws NoteAlreadyExistsException {
			int position = toAdd.getStart() * (COL_WIDTH / toAdd.getQuantization());
			if (childMap.containsKey(position)) {
				throw new NoteAlreadyExistsException();
			} else {
				childMap.put(position, note);
				QuantizedAddRequest request = new QuantizedAddRequest(
						toAdd,
						PitchUtils.rowNumToPitch(rowNum),
						lyric,
						Optional.absent());
				QuantizedAddResponse response = model.addNote(request);
				if (response.getPrevNote().isPresent()) {
					QuantizedNeighbor prev = response.getPrevNote().get();
					int prevDelta = prev.getDelta() * (COL_WIDTH / prev.getQuantization());
					childMap.get(position - prevDelta).adjustForOverlap(prevDelta);
				}
				if (response.getNextNote().isPresent()) {
					QuantizedNeighbor next = response.getNextNote().get();
					int nextDelta = next.getDelta() * (COL_WIDTH / next.getQuantization());
					note.adjustForOverlap(nextDelta);
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
			if (childMap.containsKey(position)) {
				childMap.remove(position);
			} else {
				// TODO: Handle this better.
				System.out.println("Could not find note in map of track notes :(");
			}
			QuantizedAddResponse response = model.removeNote(toRemove);
			if (response.getPrevNote().isPresent()) {
				QuantizedNeighbor prev = response.getPrevNote().get();
				int prevDelta = prev.getDelta() * (COL_WIDTH / prev.getQuantization());
				TrackNote prevNode = childMap.get(position - prevDelta);
				if (response.getNextNote().isPresent()) {
					QuantizedNeighbor next = response.getNextNote().get();
					int nextDelta = next.getDelta() * (COL_WIDTH / next.getQuantization());
					prevNode.adjustForOverlap(prevDelta + nextDelta);
				} else {
					prevNode.adjustForOverlap(Integer.MAX_VALUE);
					// Remove measures until you have 4 measures + previous note.
					setNumMeasures(((position - prevDelta) / COL_WIDTH / 4) + 4);
				}
			}

			// Remove all measures if necessary.
			if (childMap.isEmpty()) {
				setNumMeasures(4);
			}
		}

		@Override
		public void removeTrackNote(TrackNote trackNote) {
			highlighter.clearHighlights();
			track.getChildren().remove(trackNote.getElement());
		}

		@Override
		public Mode getCurrentMode() {
			return model.getCurrentMode();
		}
	};
}
