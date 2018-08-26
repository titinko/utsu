package com.utsusynth.utsu.view.song;

import java.util.List;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.utsusynth.utsu.common.PitchUtils;
import com.utsusynth.utsu.common.RegionBounds;
import com.utsusynth.utsu.common.data.AddResponse;
import com.utsusynth.utsu.common.data.EnvelopeData;
import com.utsusynth.utsu.common.data.NeighborData;
import com.utsusynth.utsu.common.data.NoteData;
import com.utsusynth.utsu.common.data.PitchbendData;
import com.utsusynth.utsu.common.data.RemoveResponse;
import com.utsusynth.utsu.common.exception.NoteAlreadyExistsException;
import com.utsusynth.utsu.common.quantize.Quantizer;
import com.utsusynth.utsu.common.quantize.Scaler;
import com.utsusynth.utsu.controller.SongController.Mode;
import com.utsusynth.utsu.view.song.note.Note;
import com.utsusynth.utsu.view.song.note.NoteCallback;
import com.utsusynth.utsu.view.song.note.NoteFactory;
import com.utsusynth.utsu.view.song.note.envelope.EnvelopeCallback;
import com.utsusynth.utsu.view.song.note.pitch.PitchbendCallback;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Group;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

public class SongEditor {
    private final PlaybackBarManager playbackManager;
    private final NoteFactory noteFactory;
    private final NoteMap noteMap;
    private final Scaler scaler;

    // Whether the vibrato editor is active for this song editor.
    private final BooleanProperty vibratoEditor;

    private HBox measures;
    private HBox dynamics;
    private int numMeasures;
    private SongCallback model;

    @Inject
    public SongEditor(
            PlaybackBarManager playbackManager,
            NoteFactory trackNoteFactory,
            NoteMap noteMap,
            Scaler scaler) {
        this.playbackManager = playbackManager;
        this.noteFactory = trackNoteFactory;
        this.noteMap = noteMap;
        this.scaler = scaler;

        vibratoEditor = new SimpleBooleanProperty(false);
    }

    /** Initialize track with data from the controller. Not song-specific. */
    public void initialize(SongCallback callback) {
        this.model = callback;
    }

    /** Initialize track with data for a specific song. */
    public HBox createNewTrack(List<NoteData> notes) {
        clearTrack();
        if (notes.isEmpty()) {
            return measures;
        }

        // Add as many octaves as needed.
        NoteData lastNote = notes.get(notes.size() - 1);
        setNumMeasures((lastNote.getPosition() / Quantizer.COL_WIDTH / 4) + 4);

        // Add all notes.
        NoteData prevNote = notes.get(0);
        for (NoteData note : notes) {
            Note newNote = noteFactory.createNote(note, noteCallback, vibratoEditor);
            int position = note.getPosition();
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
                            prevNote.getPitch(),
                            note.getPitchbend().get(),
                            getPitchbendCallback(position),
                            vibratoEditor);
                }
            } catch (NoteAlreadyExistsException e) {
                // TODO: Throw an error here?
                System.out.println("UST read found two notes in the same place :(");
            }
            noteMap.addNoteElement(newNote);
            prevNote = note;
        }
        return measures;
    }

    public Group getNotesElement() {
        return noteMap.getNotesElement();
    }

    public HBox getDynamicsElement() {
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

    public Group getPlaybackElement() {
        return playbackManager.getElement();
    }

    /** Start the playback bar animation. It will end on its own. */
    public Void startPlayback(Duration duration, double tempo) {
        playbackManager.startPlayback(duration, tempo);
        return null;
    }

    public RegionBounds getSelectedTrack() {
        return playbackManager.getRegionBounds();
    }

    public void selectAll() {
        int firstMs = noteMap.getFirstNoteMs();
        int lastMs = noteMap.getLastNoteMs();
        if (noteMap.hasNote(firstMs) && noteMap.hasNote(lastMs)) {
            playbackManager.highlightTo(noteMap.getNote(firstMs), noteMap.getAllValidNotes());
            playbackManager.highlightTo(noteMap.getNote(lastMs), noteMap.getAllValidNotes());
        }
    }

    public void selectRegion(RegionBounds region) {
        playbackManager.highlightRegion(region, noteMap.getAllValidNotes());
    }

    public void selectivelyShowRegion(double centerPercent, double margin) {
        int measureWidthMs = 4 * Quantizer.COL_WIDTH;
        int marginMeasures = ((int) (margin / Math.round(scaler.scaleX(measureWidthMs)))) + 3;
        int centerMeasure = (int) Math.round((numMeasures - 1) * centerPercent);
        int clampedStartMeasure =
                Math.min(Math.max(centerMeasure - marginMeasures, 0), numMeasures - 1);
        int clampedEndMeasure =
                Math.min(Math.max(centerMeasure + marginMeasures, 0), numMeasures - 1);
        // Use measures to we don't have to redraw the visible region too much.
        noteMap.setVisibleRegion(
                new RegionBounds(
                        clampedStartMeasure * measureWidthMs,
                        (clampedEndMeasure + 1) * measureWidthMs));
    }

    private void clearTrack() {
        // Remove current track.
        playbackManager.clear();
        noteMap.clear();
        measures = new HBox();
        dynamics = new HBox();

        numMeasures = 0;
        setNumMeasures(4);
    }

    private void setNumMeasures(int newNumMeasures) {
        // Adjust the scrollbar to be in the same place when size of the grid changes.
        double measureWidth = 4 * Math.round(scaler.scaleX(Quantizer.COL_WIDTH));
        model.adjustScrollbar(measureWidth * numMeasures, measureWidth * newNumMeasures);

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
            int maxWidth = (int) measureWidth * newNumMeasures;
            // Remove measures.
            measures.getChildren().removeIf((child) -> {
                return (int) Math.round(child.getLayoutX()) >= maxWidth;
            });
            // Remove dynamics columns.
            dynamics.getChildren().removeIf((child) -> {
                return (int) Math.round(child.getLayoutX()) >= maxWidth;
            });
            numMeasures = newNumMeasures;
        }
    }

    private void addMeasure() {
        GridPane newMeasure = new GridPane();
        int rowNum = 0;
        for (int octave = 7; octave > 0; octave--) {
            for (String pitch : PitchUtils.REVERSE_PITCHES) {
                // Add row to track.
                for (int colNum = 0; colNum < 4; colNum++) {
                    Pane newCell = new Pane();
                    newCell.setPrefSize(
                            Math.round(scaler.scaleX(Quantizer.COL_WIDTH)),
                            Math.round(scaler.scaleY(Quantizer.ROW_HEIGHT)));
                    newCell.getStyleClass().add("track-cell");
                    newCell.getStyleClass().add(pitch.endsWith("#") ? "black-key" : "white-key");
                    if (colNum == 0) {
                        newCell.getStyleClass().add("measure-start");
                    } else if (colNum == 3) {
                        newCell.getStyleClass().add("measure-end");
                    }

                    final int currentRowNum = rowNum;
                    final int currentColNum = colNum + (numMeasures * 4);
                    newCell.setOnMouseClicked((event) -> {
                        // Clear highlights regardless of current button or current mode.
                        playbackManager.clearHighlights();
                        if (event.getButton() != MouseButton.PRIMARY) {
                            return;
                        }
                        Mode currentMode = model.getCurrentMode();
                        if (currentMode == Mode.ADD) {
                            // Create note.
                            Note newNote = noteFactory.createDefaultNote(
                                    currentRowNum,
                                    currentColNum,
                                    noteCallback,
                                    vibratoEditor);
                            noteMap.addNoteElement(newNote);
                        }
                    });
                    newMeasure.add(newCell, colNum, rowNum);
                }
                rowNum++;
            }
        }
        measures.getChildren().add(newMeasure);

        // Add new columns to dynamics.
        GridPane newDynamics = new GridPane();
        for (int colNum = 0; colNum < 4; colNum++) {
            AnchorPane topCell = new AnchorPane();
            topCell.setPrefSize(scaler.scaleX(Quantizer.COL_WIDTH), 50);
            topCell.getStyleClass().add("dynamics-top-cell");
            if (colNum == 0) {
                topCell.getStyleClass().add("measure-start");
            }
            newDynamics.add(topCell, colNum, 0);
            AnchorPane bottomCell = new AnchorPane();
            bottomCell.setPrefSize(scaler.scaleX(Quantizer.COL_WIDTH), 50);
            bottomCell.getStyleClass().add("dynamics-bottom-cell");
            if (colNum == 0) {
                bottomCell.getStyleClass().add("measure-start");
            }
            newDynamics.add(bottomCell, colNum, 1);
        }
        dynamics.getChildren().add(newDynamics);

        numMeasures++;
    }

    private final NoteCallback noteCallback = new NoteCallback() {
        @Override
        public void highlightExclusive(Note note) {
            playbackManager.clearHighlights();
            playbackManager.highlightTo(note, noteMap.getAllValidNotes());
        }

        @Override
        public void highlightInclusive(Note note) {
            playbackManager.highlightTo(note, noteMap.getAllValidNotes());
        }

        @Override
        public boolean isExclusivelyHighlighted(Note note) {
            return playbackManager.isExclusivelyHighlighted(note);
        }

        @Override
        public void addSongNote(Note note, NoteData toAdd) throws NoteAlreadyExistsException {
            int position = toAdd.getPosition();
            if (noteMap.hasNote(position)) {
                throw new NoteAlreadyExistsException();
            } else {
                AddResponse response = model.addNote(toAdd);
                noteMap.putNote(position, note);

                String curPitch = response.getNote().getPitch();
                String prevPitch = curPitch;
                if (response.getPrev().isPresent()) {
                    NeighborData prev = response.getPrev().get();
                    int prevDelta = prev.getDelta();
                    Note prevTrackNote = noteMap.getNote(position - prevDelta);
                    prevTrackNote.adjustForOverlap(prevDelta);
                    prevPitch = PitchUtils.rowNumToPitch(prevTrackNote.getRow());
                    noteMap.putEnvelope(
                            position - prevDelta,
                            prev.getEnvelope(),
                            getEnvelopeCallback(position - prevDelta));
                }
                if (response.getNext().isPresent()) {
                    NeighborData next = response.getNext().get();
                    int nextDelta = next.getDelta();
                    note.adjustForOverlap(nextDelta);
                    noteMap.getNote(position + nextDelta).setTrueLyric(next.getTrueLyric());
                    noteMap.putEnvelope(
                            position + nextDelta,
                            next.getEnvelope(),
                            getEnvelopeCallback(position + nextDelta));
                    noteMap.putPitchbend(
                            position + nextDelta,
                            curPitch,
                            next.getPitchbend(),
                            getPitchbendCallback(position + nextDelta),
                            vibratoEditor);
                }
                // Refresh whether note is highlighted, must be after adjusting for overlap.
                playbackManager.refreshHighlights(note);

                // Add envelope, must be after adjusting note for overlap.
                Optional<EnvelopeData> newEnvelope = response.getNote().getEnvelope();
                if (newEnvelope.isPresent()) {
                    noteMap.putEnvelope(position, newEnvelope.get(), getEnvelopeCallback(position));
                }
                if (response.getNote().getPitchbend().isPresent()) {
                    noteMap.putPitchbend(
                            position,
                            prevPitch,
                            response.getNote().getPitchbend().get(),
                            getPitchbendCallback(position),
                            vibratoEditor);
                }

                // Add measures if necessary.
                if (!response.getNext().isPresent()) {
                    setNumMeasures((position / Quantizer.COL_WIDTH / 4) + 4);
                }

                // Set the true lyric for this note.
                if (response.getNote().getTrueLyric().isPresent()) {
                    note.setTrueLyric(response.getNote().getTrueLyric().get());
                }
            }
        }

        @Override
        public void removeSongNote(int position) {
            noteMap.removeFullNote(position);
            RemoveResponse response = model.removeNote(position);
            if (response.getPrev().isPresent()) {
                NeighborData prev = response.getPrev().get();
                int prevDelta = prev.getDelta();
                Note prevTrackNote = noteMap.getNote(position - prevDelta);
                if (response.getNext().isPresent()) {
                    prevTrackNote.adjustForOverlap(prevDelta + response.getNext().get().getDelta());
                } else {
                    prevTrackNote.adjustForOverlap(Integer.MAX_VALUE);
                }
                noteMap.putEnvelope(
                        position - prevDelta,
                        prev.getEnvelope(),
                        getEnvelopeCallback(position - prevDelta));
            }
            if (response.getNext().isPresent()) {
                NeighborData next = response.getNext().get();
                int nextDelta = next.getDelta();
                noteMap.getNote(position + nextDelta).setTrueLyric(next.getTrueLyric());
                noteMap.putEnvelope(
                        position + nextDelta,
                        next.getEnvelope(),
                        getEnvelopeCallback(position + nextDelta));
                String prevPitch =
                        PitchUtils.rowNumToPitch(noteMap.getNote(position + nextDelta).getRow());
                if (response.getPrev().isPresent()) {
                    prevPitch = PitchUtils.rowNumToPitch(
                            noteMap.getNote(position - response.getPrev().get().getDelta())
                                    .getRow());
                }
                noteMap.putPitchbend(
                        position + nextDelta,
                        prevPitch,
                        next.getPitchbend(),
                        getPitchbendCallback(position + nextDelta),
                        vibratoEditor);
            }

            // Remove all measures if necessary.
            if (noteMap.isEmpty()) {
                setNumMeasures(4);
            }
        }

        @Override
        public void removeTrackNote(Note trackNote) {
            playbackManager.clearHighlights();
            noteMap.removeNoteElement(trackNote);
            // TODO: If last note, remove measures until you have 4 measures + previous note.
        }

        @Override
        public Mode getCurrentMode() {
            return model.getCurrentMode();
        }

        @Override
        public Optional<EnvelopeData> getEnvelope(int position) {
            if (noteMap.hasEnvelope(position)) {
                return Optional.of(noteMap.getEnvelope(position).getData());
            }
            return Optional.absent();
        }

        @Override
        public Optional<PitchbendData> getPitchbend(int position) {
            if (noteMap.hasPitchbend(position)) {
                return Optional.of(noteMap.getPitchbend(position).getData(position));
            }
            return Optional.absent();
        }

        @Override
        public boolean hasVibrato(int position) {
            if (noteMap.hasPitchbend(position)) {
                return noteMap.getPitchbend(position).hasVibrato();
            }
            return false;
        }

        @Override
        public void setHasVibrato(int position, boolean hasVibrato) {
            if (noteMap.hasPitchbend(position)) {
                noteMap.getPitchbend(position).setHasVibrato(hasVibrato);
            }
        }

        @Override
        public void openNoteProperties(Note note) {
            if (playbackManager.isAnythingHighlighted()) {
                model.openNoteProperties(playbackManager.getRegionBounds());
            } else {
                // Open on current note if nothing is highlighted.
                model.openNoteProperties(note.getValidBounds());
            }
        }
    };

    private EnvelopeCallback getEnvelopeCallback(final int position) {
        return new EnvelopeCallback() {
            @Override
            public void modifySongEnvelope() {
                Note toModify = noteMap.getNote(position);
                NoteData mutation = new NoteData(
                        toModify.getAbsPositionMs(),
                        toModify.getDurationMs(),
                        PitchUtils.rowNumToPitch(toModify.getRow()),
                        toModify.getLyric(),
                        noteMap.getEnvelope(position).getData());
                model.modifyNote(mutation);
            }
        };
    }

    private PitchbendCallback getPitchbendCallback(final int position) {
        return new PitchbendCallback() {
            @Override
            public void modifySongPitchbend() {
                Note toModify = noteMap.getNote(position);
                NoteData mutation = new NoteData(
                        position,
                        toModify.getDurationMs(),
                        PitchUtils.rowNumToPitch(toModify.getRow()),
                        toModify.getLyric(),
                        noteMap.getPitchbend(position).getData(position));
                model.modifyNote(mutation);
            }
        };
    }
}
