package com.utsusynth.utsu.view.song;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.utsusynth.utsu.common.RegionBounds;
import com.utsusynth.utsu.common.data.*;
import com.utsusynth.utsu.common.exception.NoteAlreadyExistsException;
import com.utsusynth.utsu.common.i18n.Localizer;
import com.utsusynth.utsu.common.quantize.Quantizer;
import com.utsusynth.utsu.common.quantize.Scaler;
import com.utsusynth.utsu.common.utils.PitchUtils;
import com.utsusynth.utsu.common.utils.RoundUtils;
import com.utsusynth.utsu.controller.UtsuController.CheckboxType;
import com.utsusynth.utsu.view.song.note.AddNoteBox;
import com.utsusynth.utsu.view.song.note.Note;
import com.utsusynth.utsu.view.song.note.NoteCallback;
import com.utsusynth.utsu.view.song.note.NoteFactory;
import com.utsusynth.utsu.view.song.note.envelope.EnvelopeCallback;
import com.utsusynth.utsu.view.song.note.pitch.PitchbendCallback;
import com.utsusynth.utsu.view.song.playback.PlaybackCallback;
import com.utsusynth.utsu.view.song.playback.PlaybackManager;
import com.utsusynth.utsu.view.song.playback.SelectionBox;
import com.utsusynth.utsu.view.song.track.Track;
import com.utsusynth.utsu.view.song.track.TrackCallback;
import com.utsusynth.utsu.view.song.track.TrackItem;
import com.utsusynth.utsu.view.song.track.TrackItemSet;
import javafx.beans.property.*;
import javafx.scene.Group;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.*;
import java.util.stream.Collectors;

public class SongEditor {
    private final Track track;
    private final PlaybackManager playbackManager;
    private final SelectionBox selectionBox;
    private final AddNoteBox addNoteBox;
    private final ContextMenu editorContextMenu;
    private final SongClipboard clipboard;
    private final NoteFactory noteFactory;
    private final NoteMap noteMap;
    private final Quantizer quantizer;
    private final Scaler scaler;

    // Whether the vibrato editor is active for this song editor.
    private final BooleanProperty vibratoEditor;

    //private HBox measures;
    private Region canvas;
    //private Rectangle selection;
    //private int numMeasures;
    private SongCallback model;

    // Temporary cache values.
    private DragHandler dragHandler; // Expect this to sometimes be null.
    private double curX;
    private double curY;

    @Inject
    public SongEditor(
            Track track,
            PlaybackManager playbackManager,
            SelectionBox selectionBox,
            AddNoteBox addNoteBox,
            SongClipboard clipboard,
            NoteFactory trackNoteFactory,
            NoteMap noteMap,
            Localizer localizer,
            Quantizer quantizer,
            Scaler scaler) {
        this.track = track;
        this.playbackManager = playbackManager;
        this.selectionBox = selectionBox;
        this.addNoteBox = addNoteBox;
        this.clipboard = clipboard;
        this.noteFactory = trackNoteFactory;
        this.noteMap = noteMap;
        this.quantizer = quantizer;
        this.scaler = scaler;

        vibratoEditor = new SimpleBooleanProperty(false);

        // Initialize context menu here so we can reuse it everywhere.
        editorContextMenu = new ContextMenu();
        MenuItem pasteItem = new MenuItem("Paste");
        pasteItem.disableProperty().bind(clipboard.clipboardFilledProperty().not());
        pasteItem.setOnAction(event -> pasteSelected());
        SeparatorMenuItem separator = new SeparatorMenuItem();
        MenuItem selectAllItem = new MenuItem("Select All");
        selectAllItem.setOnAction(event -> selectAll());
        MenuItem deselectItem = new MenuItem("Clear Selection");
        deselectItem.setOnAction(event -> playbackManager.clearHighlights());
        editorContextMenu.getItems().addAll(pasteItem, separator, selectAllItem, deselectItem);
        editorContextMenu.setOnShowing(event -> {
            pasteItem.setText(localizer.getMessage("menu.edit.paste"));
            selectAllItem.setText(localizer.getMessage("menu.edit.selectAll"));
            deselectItem.setText(localizer.getMessage("menu.edit.clearSelection"));
        });
    }

    /**
     * Initialize track with data from the controller. Not song-specific.
     */
    public void initialize(SongCallback callback) {
        model = callback;
        track.initialize(new TrackCallback() {
            @Override
            public VBox createNoteColumn(int colNum) {
                return createNoteColumnInternal(colNum);
            }

            @Override
            public VBox createDynamicsColumn(int colNum) {
                return createDyanmicsColumnInternal(colNum);
            }
        }, new DragHandler() {
            @Override
            public void onDragged(double absoluteX, double absoluteY) {
                if (dragHandler != null) {
                    dragHandler.onDragged(absoluteX, absoluteY);
                }
            }

            @Override
            public void onDragReleased(double absoluteX, double absoluteY) {
                if (dragHandler != null) {
                    dragHandler.onDragReleased(absoluteX, absoluteY);
                    dragHandler = null;
                }
            }
        });
        playbackManager.initialize(new PlaybackCallback() {
            @Override
            public void setBar(TrackItem bar) {
                track.removeItem(track.getNoteTrack(), bar);
                track.insertItem(track.getNoteTrack(), bar);
            }

            @Override
            public void removeBar(TrackItem bar) {
                track.removeItem(track.getNoteTrack(), bar);
            }

            @Override
            public void readjust(TrackItem bar) {
                int numColumns = bar.getColumns().size();
                track.insertItem(track.getNoteTrack(), bar);
                if (bar.getColumns().size() > numColumns) {
                    setBar(bar);
                }
            }
        });
    }

    /**
     * Initialize track with data for a specific song.
     */
    public ListView<TrackItemSet> createNewTrack(List<NoteData> notes) {
        clearTrack();
        if (notes.isEmpty()) {
            return track.getNoteTrack();
        }

        // Add as many octaves as needed.
        NoteData lastNote = notes.get(notes.size() - 1);
        setNumMeasures((lastNote.getPosition() / Quantizer.COL_WIDTH / 4) + 4);

        // Add all notes.
        NoteData prevNote = notes.get(0);
        for (NoteData note : notes) {
            Note newNote = noteFactory.createNote(
                    note,
                    noteCallback,
                    vibratoEditor,
                    model.getCheckboxValue(CheckboxType.SHOW_LYRICS),
                    model.getCheckboxValue(CheckboxType.SHOW_ALIASES));
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
                            vibratoEditor,
                            model.getCheckboxValue(CheckboxType.SHOW_PITCHBENDS));
                }
            } catch (NoteAlreadyExistsException e) {
                // TODO: Throw an error here?
                System.out.println("UST read found two notes in the same place :(");
            }
            noteMap.addNoteElement(newNote);
            prevNote = note;
        }
        return track.getNoteTrack();
        //return measures;
    }

    public Group getNotesElement() {
        return noteMap.getNotesElement();
    }

    public ListView<TrackItemSet> getDynamicsElement() {
        return track.getDynamicsTrack();
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

    public Region getCanvasElement() {
        return canvas;
    }

    /**
     * Start the playback bar animation. It will end on its own.
     */
    public DoubleProperty startPlayback(RegionBounds rendered, Duration duration) {
        int firstPosition = noteMap.getFirstPosition(rendered);
        int lastPosition = noteMap.getLastPosition(rendered);
        if (noteMap.hasNote(firstPosition) && noteMap.hasNote(lastPosition)) {
            int firstNoteStart = noteMap.getEnvelope(firstPosition).getStartMs();
            int renderStart = Math.min(firstNoteStart, rendered.getMinMs());
            int renderEnd = lastPosition + noteMap.getNote(lastPosition).getDurationMs();
            return playbackManager.startPlayback(
                    duration, new RegionBounds(renderStart, renderEnd));
        }
        return null;
    }

    /**
     * Attempts to pause. Does nothing if there is no ongoing playback.
     */
    public void pausePlayback() {
        playbackManager.pausePlayback();
    }

    /**
     * Attempts to resume. Does nothing if there is no ongoing paused playback.
     */
    public void resumePlayback() {
        playbackManager.resumePlayback();
    }

    /**
     * Manually stop any ongoing playback bar animation. Idempotent.
     */
    public void stopPlayback() {
        playbackManager.stopPlayback();
    }

    public double getWidthX() {
        return track.getNoteTrack().getWidth(); // Include pre-roll.
        //double measureWidth = 4 * scaler.scaleX(Quantizer.COL_WIDTH).get();
        //return measureWidth * (numMeasures + 1); // Include pre-roll.
    }

    public BooleanProperty clibboardFilledProperty() {
        return clipboard.clipboardFilledProperty();
    }

    public BooleanProperty isAnythingSelectedProperty() {
        return playbackManager.isAnythingHighlightedProperty();
    }

    public RegionBounds getPlayableTrack() {
        return playbackManager.getPlayableRegion();
    }

    public RegionBounds getSelectedTrack() {
        return playbackManager.getSelectedRegion();
    }

    public void selectRegion(RegionBounds region) {
        playbackManager.highlightRegion(region, noteMap.getAllValidNotes());
    }

    public void selectAll() {
        playbackManager.highlightAll(noteMap.getAllValidNotes());
    }

    public void copySelected() {
        List<NoteData> notesToCopy = playbackManager.getHighlightedNotes().stream()
                .map(curNote -> curNote.getNoteData()).collect(Collectors.toList());
        clipboard.setNotes(notesToCopy);
    }

    public void pasteSelected() {
        List<NoteData> toPaste = clipboard.getNotes();
        if (toPaste.isEmpty()) {
            return;
        }
        int curPosition = playbackManager.getCursorPosition();
        int positionDelta = curPosition - toPaste.get(0).getPosition();
        LinkedList<NoteData> toAdd = new LinkedList<>();
        LinkedList<Note> newNotes = new LinkedList<>();
        for (NoteData noteData : toPaste) {
            Note newNote = noteFactory.createNote(
                    noteData,
                    noteCallback,
                    vibratoEditor,
                    model.getCheckboxValue(CheckboxType.SHOW_LYRICS),
                    model.getCheckboxValue(CheckboxType.SHOW_ALIASES));
            newNote.moveNoteElement(positionDelta, 0);
            newNotes.add(newNote);
            curPosition = newNote.getAbsPositionMs();
            noteMap.addNoteElement(newNote);
            try {
                noteMap.putNote(curPosition, newNote);
            } catch (NoteAlreadyExistsException e) {
                newNote.setValid(false);
                continue;
            }
            toAdd.add(newNote.getNoteData());
        }

        if (toAdd.isEmpty()) {
            return;
        }
        model.addNotes(toAdd);
        refreshNotes(toAdd.getFirst().getPosition(), toAdd.getLast().getPosition());
        model.recordAction(() -> {
            playbackManager.clearHighlights();
            undoDeleteNotes(newNotes);
        }, () -> {
            playbackManager.clearHighlights();
            deleteNotes(newNotes);
        });
    }

    public void deleteSelected() {
        List<Note> toDelete = playbackManager.getHighlightedNotes();
        deleteNotes(toDelete);
        model.recordAction(() -> {
            playbackManager.clearHighlights();
            deleteNotes(toDelete);
        }, () -> {
            playbackManager.clearHighlights();
            undoDeleteNotes(toDelete);
        });
        playbackManager.clearHighlights();
    }

    private void deleteNotes(List<Note> notes) {
        Set<Integer> positionsToRemove = notes.stream().filter(Note::isValid)
                .map(Note::getAbsPositionMs).collect(Collectors.toSet());
        RegionBounds toStandardize = removeNotes(positionsToRemove);
        if (!toStandardize.equals(RegionBounds.INVALID)) {
            refreshNotes(toStandardize.getMinMs(), toStandardize.getMaxMs());
        }
        for (Note note : notes) {
            noteMap.removeNoteElement(note);
        }
    }

    private void undoDeleteNotes(List<Note> notes) {
        LinkedList<NoteData> toAdd = new LinkedList<>();
        for (Note note : notes) {
            noteMap.addNoteElement(note);
            note.setValid(true);
            try {
                noteMap.putNote(note.getAbsPositionMs(), note);
            } catch (NoteAlreadyExistsException e) {
                note.setValid(false);
                continue;
            }
            toAdd.add(note.getNoteData());
        }
        if (toAdd.isEmpty()) {
            return;
        }
        model.addNotes(toAdd);
        refreshNotes(toAdd.getFirst().getPosition(), toAdd.getLast().getPosition());
    }

    /**
     * Removes notes from the backend song, returns RegionBounds of notes that need refreshing.
     */
    private RegionBounds removeNotes(Set<Integer> positionsToRemove) {
        if (positionsToRemove.isEmpty()) {
            return RegionBounds.INVALID; // If no valid song notes to remove, do nothing.
        }
        MutateResponse response = model.removeNotes(positionsToRemove);
        // Remove all deleted notes from note map.
        for (NoteUpdateData updateData : response.getNotes()) {
            // Should never happen but let's check just in case.
            if (noteMap.hasNote(updateData.getPosition())) {
                noteMap.removeFullNote(updateData.getPosition());
            } else {
                System.out.println("Error: Note present in backend but not in frontend!");
            }
        }

        if (response.getPrev().isPresent() && response.getNext().isPresent()) {
            return new RegionBounds(
                    response.getPrev().get().getPosition(),
                    response.getNext().get().getPosition());
        } else if (response.getPrev().isPresent()) {
            int prevPosition = response.getPrev().get().getPosition();
            return new RegionBounds(prevPosition, prevPosition);
        } else if (response.getNext().isPresent()) {
            int nextPosition = response.getNext().get().getPosition();
            return new RegionBounds(nextPosition, nextPosition);
        }
        return RegionBounds.INVALID;
    }

    public void refreshSelected() {
        List<Note> highlightedNotes = playbackManager.getHighlightedNotes();
        if (highlightedNotes.isEmpty()) {
            return;
        }
        refreshNotes(
                highlightedNotes.get(0).getAbsPositionMs(),
                highlightedNotes.get(highlightedNotes.size() - 1).getAbsPositionMs());
    }

    private void refreshNotes(int firstPosition, int lastPosition) {
        MutateResponse standardizeResponse = model.standardizeNotes(firstPosition, lastPosition);
        String prevPitch = "";
        Note prevNote = null;
        if (standardizeResponse.getPrev().isPresent()) {
            NoteUpdateData prevData = standardizeResponse.getPrev().get();
            prevNote = noteMap.getNote(prevData.getPosition());
            prevNote.setBackupData(prevData);
            prevPitch = PitchUtils.rowNumToPitch(prevNote.getRow());
            noteMap.putEnvelope(
                    prevData.getPosition(),
                    prevData.getEnvelope(),
                    getEnvelopeCallback(prevData.getPosition()));
        }
        Iterator<NoteUpdateData> dataIterator = standardizeResponse.getNotes().iterator();
        NoteUpdateData curData = null;
        Note curNote = null;
        while (dataIterator.hasNext()) {
            curData = dataIterator.next();
            curNote = noteMap.getNote(curData.getPosition());
            curNote.setBackupData(curData);
            curNote.setTrueLyric(curData.getTrueLyric());
            noteMap.putEnvelope(
                    curData.getPosition(),
                    curData.getEnvelope(),
                    getEnvelopeCallback(curData.getPosition()));
            noteMap.putPitchbend(
                    curData.getPosition(),
                    prevPitch.isEmpty() ? PitchUtils.rowNumToPitch(curNote.getRow()) : prevPitch,
                    curData.getPitchbend(),
                    getPitchbendCallback(curData.getPosition()),
                    vibratoEditor,
                    model.getCheckboxValue(CheckboxType.SHOW_PITCHBENDS));
            if (prevNote != null) {
                prevNote.adjustForOverlap(curData.getPosition() - prevNote.getAbsPositionMs());
            }
            prevNote = curNote;
            prevPitch = PitchUtils.rowNumToPitch(curNote.getRow());
        }
        if (standardizeResponse.getNext().isPresent()) {
            NoteUpdateData nextData = standardizeResponse.getNext().get();
            Note nextNote = noteMap.getNote(nextData.getPosition());
            nextNote.setBackupData(nextData);
            nextNote.setTrueLyric(nextData.getTrueLyric());
            noteMap.putEnvelope(
                    nextData.getPosition(),
                    nextData.getEnvelope(),
                    getEnvelopeCallback(nextData.getPosition()));
            noteMap.putPitchbend(
                    nextData.getPosition(),
                    prevPitch.isEmpty() ? PitchUtils.rowNumToPitch(nextNote.getRow()) : prevPitch,
                    nextData.getPitchbend(),
                    getPitchbendCallback(nextData.getPosition()),
                    vibratoEditor,
                    model.getCheckboxValue(CheckboxType.SHOW_PITCHBENDS));
            if (curNote != null) {
                curNote.adjustForOverlap(nextData.getPosition() - curData.getPosition());
            }
        } else if (curNote != null) {
            curNote.adjustForOverlap(Integer.MAX_VALUE);
            // If this is the last note, adjust number of measures.
            setNumMeasures((curNote.getBounds().getMaxMs() / Quantizer.COL_WIDTH / 4) + 4);
        }
    }

    private void moveNotes(List<Note> notes, int positionDelta, int rowDelta) {
        Set<Integer> positionsToRemove = notes.stream().filter(Note::isValid)
                .map(Note::getAbsPositionMs).collect(Collectors.toSet());
        RegionBounds toStandardize = removeNotes(positionsToRemove);

        LinkedList<NoteData> toAdd = new LinkedList<>();
        for (Note curNote : notes) {
            curNote.moveNoteElement(positionDelta, rowDelta);
            curNote.setValid(true);
            try {
                noteMap.putNote(curNote.getAbsPositionMs(), curNote);
            } catch (NoteAlreadyExistsException e) {
                curNote.setValid(false);
                continue;
            }
            toAdd.add(curNote.getNoteData());
        }
        // Standardize and return early if nothing needs to be added.
        if (toAdd.isEmpty()) {
            if (!toStandardize.equals(RegionBounds.INVALID)) {
                refreshNotes(toStandardize.getMinMs(), toStandardize.getMaxMs());
            }
            return;
        }
        model.addNotes(toAdd);
        RegionBounds addRegion =
                new RegionBounds(toAdd.getFirst().getPosition(), toAdd.getLast().getPosition());
        toStandardize = toStandardize.mergeWith(addRegion);
        refreshNotes(toStandardize.getMinMs(), toStandardize.getMaxMs());
    }

    public Optional<Integer> getFocusNote() {
        List<Note> highlightedNotes = playbackManager.getHighlightedNotes();
        if (!highlightedNotes.isEmpty()) {
            return Optional.of(highlightedNotes.get(0).getAbsPositionMs());
        }
        return Optional.empty();
    }

    public void focusOnNote(int position) {
        if (!noteMap.hasNote(position)) {
            return;
        }
        // If old focus has lyric open, new focus should have it open too.
        boolean shouldOpenLyricInput = false;
        List<Note> highlightedNotes = playbackManager.getHighlightedNotes();
        if (!highlightedNotes.isEmpty()) {
            shouldOpenLyricInput = highlightedNotes.get(0).isLyricInputOpen();
        }
        Note newFocus = noteMap.getNote(position);
        playbackManager.clearHighlights();
        playbackManager.highlightNote(newFocus);
        playbackManager.realign();
        if (shouldOpenLyricInput) {
            newFocus.openLyricInput();
        }
    }

    public void openLyricInput(int position) {
        if (noteMap.hasNote(position)) {
            noteMap.getNote(position).openLyricInput();
        }
    }

    public void selectivelyShowRegion(double centerPercent, double margin) {
        int numMeasures = track.getNumMeasures();
        int measureWidthMs = 4 * Quantizer.COL_WIDTH;
        int marginMeasures = ((int) (margin / Math.round(scaler.scaleX(measureWidthMs).get()))) + 3;
        int centerMeasure = RoundUtils.round((numMeasures) * centerPercent) - 1; // Pre-roll.
        int clampedStartMeasure =
                Math.min(Math.max(centerMeasure - marginMeasures, 0), numMeasures - 1);
        int clampedEndMeasure =
                Math.min(Math.max(centerMeasure + marginMeasures, 0), numMeasures - 1);
        // Use measures to we don't have to redraw the visible region too much.
        showMeasures(clampedStartMeasure, clampedEndMeasure + 1);
        noteMap.setVisibleRegion(
                new RegionBounds(
                        clampedStartMeasure * measureWidthMs,
                        (clampedEndMeasure + 1) * measureWidthMs));
    }

    private void clearTrack() {
        // Remove current track.
        playbackManager.clear();
        noteMap.clear();
        // measures = new HBox();
        // selection = new Rectangle();

        // numMeasures = 0;
        // addMeasure(false);
        setNumMeasures(4);
    }

    private void showMeasures(int startMeasure, int endMeasure) {
        /*int measureWidth = 4 * RoundUtils.round(scaler.scaleX(Quantizer.COL_WIDTH).get());
        int startX = measureWidth * startMeasure;
        int endX = measureWidth * endMeasure;
        measures.getChildren().forEach(child -> {
            int measureX = RoundUtils.round(child.getLayoutX());
            child.setVisible(measureX >= startX && measureX <= endX);
        });*/
    }

    private void setNumMeasures(int newNumMeasures) {
        track.setNumMeasures(newNumMeasures);

        /*if (newNumMeasures < 0) {
            return;
        } else if (newNumMeasures > numMeasures) {
            for (int i = numMeasures; i < newNumMeasures; i++) {
                addMeasure(true);
            }
        } else if (newNumMeasures == numMeasures) {
            // Nothing needs to be done.
            return;
        } else {
            int measureWidth = 4 * RoundUtils.round(scaler.scaleX(Quantizer.COL_WIDTH).get());
            int maxWidth = measureWidth * (newNumMeasures + 1); // Include pre-roll.
            // Remove measures.
            measures.getChildren().removeIf(
                    child -> RoundUtils.round(child.getLayoutX()) >= maxWidth);
            numMeasures = newNumMeasures;
        }*/
    }

    private void addMeasure(boolean enabled) {
        double colWidth = scaler.scaleX(Quantizer.COL_WIDTH).get();
        double rowHeight = scaler.scaleY(Quantizer.ROW_HEIGHT).get();

        Pane newMeasure = new Pane();
        newMeasure.setPrefSize(colWidth * 4, rowHeight * PitchUtils.TOTAL_NUM_PITCHES);
        int rowNum = 0;
        for (int octave = 7; octave > 0; octave--) {
            for (String pitch : PitchUtils.REVERSE_PITCHES) {
                // Add row to track.
                for (int colNum = 0; colNum < 4; colNum++) {
                    Pane newCell = new Pane();
                    newCell.setPrefSize(colWidth, rowHeight);
                    newCell.getStyleClass().add("track-cell");
                    if (enabled) {
                        newCell.getStyleClass()
                                .add(pitch.endsWith("#") ? "black-key" : "white-key");
                    } else {
                        newCell.getStyleClass().add("gray-key");
                    }
                    if (colNum == 0) {
                        newCell.getStyleClass().add("measure-start");
                    } else if (colNum == 3) {
                        newCell.getStyleClass().add("measure-end");
                    }
                    newCell.setTranslateX(colWidth * colNum);
                    newCell.setTranslateY(rowHeight * rowNum);
                    newMeasure.getChildren().add(newCell);
                }
                rowNum++;
            }
        }
        //measures.getChildren().add(newMeasure);

        if (enabled) {
            //activateMeasure(newMeasure);
            //numMeasures++;
        }
    }

    private VBox createNoteColumnInternal(int colNum) {
        double colWidth = scaler.scaleX(Quantizer.COL_WIDTH).get();
        double rowHeight = scaler.scaleY(Quantizer.ROW_HEIGHT).get();

        VBox column = new VBox();
        for (int octave = 7; octave > 0; octave--) {
            for (String pitch : PitchUtils.REVERSE_PITCHES) {
                // Add row to track.
                Pane newCell = new Pane();
                newCell.setPrefSize(colWidth, rowHeight);
                newCell.getStyleClass().add("track-cell");
                if (colNum >= 4) {
                    newCell.getStyleClass()
                            .add(pitch.endsWith("#") ? "black-key" : "white-key");
                } else {
                    newCell.getStyleClass().add("gray-key");
                }
                if (colNum % 4 == 0) {
                    newCell.getStyleClass().add("measure-start");
                } else if (colNum % 4 == 3) {
                    newCell.getStyleClass().add("measure-end");
                }
                column.getChildren().add(newCell);
            }
        }
        activateNoteColumn(column, colNum);
        return column;
    }

    private void activateNoteColumn(VBox column, int colNum) {
        column.setOnMousePressed(event -> {
            double offsetX = colNum * scaler.scaleX(Quantizer.COL_WIDTH).get();
            // End any leftover drag action.
            if (dragHandler != null) {
                dragHandler.onDragReleased(offsetX + event.getX(), event.getY());
                dragHandler = null;
            }
            editorContextMenu.hide();
            curX = offsetX + event.getX();
            curY = event.getY();
        });
        column.setOnMouseReleased(event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                editorContextMenu.show(
                        track.getNoteTrack(), event.getScreenX(), event.getScreenY());
            }
            if (event.getButton() == MouseButton.SECONDARY || event.isShiftDown()) {
                double offsetX = colNum * scaler.scaleX(Quantizer.COL_WIDTH).get();
                double endX = offsetX + event.getX();
                int quantSize = quantizer.getQuant();
                int endMs = RoundUtils.round(
                        scaler.unscalePos(endX) / quantSize) * quantSize;
                playbackManager.setCursor(endMs);
            }
        });
        column.setOnDragDetected(event -> {
            column.startFullDrag();
            if (event.isShiftDown() || !event.isPrimaryButtonDown()) {
                // Select mode.
                dragHandler = new DragHandler() {
                    @Override
                    public void onDragged(double absoluteX, double absoluteY) {
                        // Draw selection rectangle.
                        double measureWidth = 4 * scaler.scaleX(Quantizer.COL_WIDTH).get();
                        double endX = Math.min(getWidthX(), Math.max(measureWidth, absoluteX));
                        double endY = Math.min(column.getHeight(), Math.max(0, absoluteY));
                        selectionBox.setStartX(Math.min(curX, endX));
                        selectionBox.setStartY(Math.min(curY, endY));
                        selectionBox.setWidth(Math.abs(endX - curX));
                        selectionBox.setHeight(Math.abs(endY - curY));
                        track.insertItem(track.getNoteTrack(), selectionBox);
                        // Update highlighted notes.
                        int startRow = (int) scaler.unscaleY(curY) / Quantizer.ROW_HEIGHT;
                        int endRow = (int) scaler.unscaleY(endY) / Quantizer.ROW_HEIGHT;
                        int startMs = RoundUtils.round(scaler.unscalePos(curX));
                        int endMs = RoundUtils.round(scaler.unscalePos(endX));
                        RegionBounds horizontalBounds = endMs >= startMs
                                ? new RegionBounds(startMs, endMs)
                                : new RegionBounds(endMs, startMs);
                        playbackManager.clearHighlights();
                        for (Note note : noteMap.getAllValidNotes()) {
                            int noteRow = note.getRow();
                            if (note.getValidBounds().intersects(horizontalBounds)
                                    && Math.abs(endRow - noteRow) + Math.abs(noteRow - startRow)
                                    == Math.abs(endRow - startRow)) {
                                playbackManager.highlightNote(note);
                            }
                        }
                    }

                    @Override
                    public void onDragReleased(double absoluteX, double absoluteY) {
                        track.removeItem(track.getNoteTrack(), selectionBox);
                        if (!playbackManager.getHighlightedNotes().isEmpty()) {
                            playbackManager.realign();
                        } else {
                            // Set cursor.
                            int quantSize = quantizer.getQuant();
                            double measureWidth = 4 * scaler.scaleX(Quantizer.COL_WIDTH).get();
                            double endX = Math.min(getWidthX(), Math.max(measureWidth, absoluteX));
                            int endMs = RoundUtils.round(
                                    scaler.unscalePos(endX) / quantSize) * quantSize;
                            playbackManager.setCursor(endMs);
                        }
                    }
                };
            } else {
                // Create mode.
                dragHandler = new DragHandler() {
                    @Override
                    public void onDragged(double absoluteX, double absoluteY) {
                        double measureWidth = 4 * scaler.scaleX(Quantizer.COL_WIDTH).get();
                        double endX = Math.min(getWidthX(), Math.max(measureWidth, absoluteX));
                        int quantSize = quantizer.getQuant();
                        int startMs = RoundUtils.round(
                                scaler.unscalePos(curX) / quantSize) * quantSize;
                        int endMs = RoundUtils.round(
                                scaler.unscalePos(endX) / quantSize) * quantSize;
                        if (endMs > startMs) {
                            // Draw selection rectangle.
                            int startRow = (int) scaler.unscaleY(curY) / Quantizer.ROW_HEIGHT;
                            addNoteBox.setStartX(scaler.scalePos(startMs).get());
                            addNoteBox.setStartY(
                                    scaler.scaleY(startRow * Quantizer.ROW_HEIGHT).get());
                            addNoteBox.setWidth(scaler.scaleX(endMs - startMs).get());
                            addNoteBox.setHeight(scaler.scaleY(Quantizer.ROW_HEIGHT).get());
                            track.insertItem(track.getNoteTrack(), addNoteBox);
                        } else {
                            track.removeItem(track.getNoteTrack(), addNoteBox);
                        }
                    }

                    @Override
                    public void onDragReleased(double absoluteX, double absoluteY) {
                        track.removeItem(track.getNoteTrack(), addNoteBox);
                        double measureWidth = 4 * scaler.scaleX(Quantizer.COL_WIDTH).get();
                        double endX = Math.min(getWidthX(), Math.max(measureWidth, absoluteX));
                        int quantSize = quantizer.getQuant();
                        int startMs = RoundUtils.round(
                                scaler.unscalePos(curX) / quantSize) * quantSize;
                        int endMs = RoundUtils.round(
                                scaler.unscalePos(endX) / quantSize) * quantSize;
                        // Create new note if size would be nonzero.
                        if (endMs > startMs) {
                            int startRow = (int) scaler.unscaleY(curY) / Quantizer.ROW_HEIGHT;
                            Note newNote = noteFactory.createDefaultNote(
                                    startRow,
                                    startMs,
                                    endMs - startMs,
                                    noteCallback,
                                    vibratoEditor,
                                    model.getCheckboxValue(CheckboxType.SHOW_LYRICS),
                                    model.getCheckboxValue(CheckboxType.SHOW_ALIASES));
                            noteMap.addNoteElement(newNote);
                            List<Note> noteList = ImmutableList.of(newNote);
                            model.recordAction(() -> {
                                playbackManager.clearHighlights();
                                undoDeleteNotes(noteList);
                            }, () -> {
                                playbackManager.clearHighlights();
                                deleteNotes(noteList);
                            });
                        }
                    }
                };
            }
        });
    }

    private VBox createDyanmicsColumnInternal(int colNum) {
        double colWidth = scaler.scaleX(Quantizer.COL_WIDTH).get();
        double rowHeight = 50;

        VBox newDynamics = new VBox();
        AnchorPane topCell = new AnchorPane();
        topCell.setPrefSize(colWidth, rowHeight);
        topCell.getStyleClass().add("dynamics-top-cell");
        if (colNum % 4 == 0) {
            topCell.getStyleClass().add("measure-start");
        }

        AnchorPane bottomCell = new AnchorPane();
        bottomCell.setPrefSize(colWidth, rowHeight);
        bottomCell.getStyleClass().add("dynamics-bottom-cell");
        if (colNum % 4 == 0) {
            bottomCell.getStyleClass().add("measure-start");
        }
        newDynamics.getChildren().addAll(topCell, bottomCell);
        return newDynamics;
    }

    private final NoteCallback noteCallback = new NoteCallback() {
        @Override
        public void highlightExclusive(Note note) {
            playbackManager.clearHighlights();
            playbackManager.highlightNote(note);
            playbackManager.realign();
        }

        @Override
        public void highlightInclusive(Note note) {
            RegionBounds merged =
                    note.getValidBounds().mergeWith(playbackManager.getSelectedRegion());
            playbackManager.highlightRegion(merged, noteMap.getAllValidNotes());
        }

        @Override
        public void realignHighlights() {
            playbackManager.realign();
        }

        @Override
        public boolean isExclusivelyHighlighted(Note note) {
            return playbackManager.isExclusivelyHighlighted(note);
        }

        @Override
        public void updateNote(Note note) {
            int positionMs = note.getAbsPositionMs();
            if (note.isValid()) {
                // Removes note if necessary.
                removeNotes(ImmutableSet.of(positionMs));
            }
            try {
                // Replaces note if possible.
                noteMap.putNote(positionMs, note);
                note.setValid(true);
                model.addNotes(ImmutableList.of(note.getNoteData()));
            } catch (NoteAlreadyExistsException e) {
                note.setValid(false);
            }
            // Refreshes notes regardless of whether a new one was placed.
            refreshNotes(positionMs, positionMs);
        }

        @Override
        public void moveNote(Note note, int positionDelta, int rowDelta) {
            List<Note> toMove =
                    playbackManager.isHighlighted(note) ? playbackManager.getHighlightedNotes()
                            : ImmutableList.of(note);
            moveNotes(toMove, positionDelta, rowDelta);
        }

        @Override
        public void recordNoteMovement(Note note, int positionDelta, int rowDelta) {
            // Records one note movement and how to undo/redo it.
            List<Note> toMove =
                    playbackManager.isHighlighted(note) ? playbackManager.getHighlightedNotes()
                            : ImmutableList.of(note);
            model.recordAction(() -> {
                playbackManager.clearHighlights();
                moveNotes(toMove, positionDelta, rowDelta);
            }, () -> {
                playbackManager.clearHighlights();
                moveNotes(toMove, -positionDelta, -rowDelta);
            });
        }

        @Override
        public void copyNote(Note note) {
            List<Note> notesToCopy =
                    playbackManager.isHighlighted(note) ? playbackManager.getHighlightedNotes()
                            : ImmutableList.of(note);
            List<NoteData> dataToCopy = notesToCopy.stream().map(Note::getNoteData)
                    .collect(Collectors.toList());
            clipboard.setNotes(dataToCopy);
        }

        @Override
        public void deleteNote(Note note) {
            List<Note> toDelete =
                    playbackManager.isHighlighted(note) ? playbackManager.getHighlightedNotes()
                            : ImmutableList.of(note);
            deleteNotes(toDelete);
            model.recordAction(() -> {
                playbackManager.clearHighlights();
                deleteNotes(toDelete);
            }, () -> {
                playbackManager.clearHighlights();
                undoDeleteNotes(toDelete);
            });
            if (playbackManager.isHighlighted(note)) {
                playbackManager.clearHighlights();
            }
        }

        @Override
        public RegionBounds getBounds(Note note) {
            return playbackManager.isHighlighted(note) ? playbackManager.getSelectedRegion()
                    : note.getBounds();
        }

        @Override
        public int getLowestRow(Note note) {
            return playbackManager.isHighlighted(note) ? playbackManager.getLowestRow()
                    : note.getRow();
        }

        @Override
        public int getHighestRow(Note note) {
            return playbackManager.isHighlighted(note) ? playbackManager.getHighestRow()
                    : note.getRow();
        }

        @Override
        public void recordAction(Runnable redoAction, Runnable undoAction) {
            model.recordAction(redoAction, undoAction);
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
            if (playbackManager.isHighlighted(note)) {
                model.openNoteProperties(playbackManager.getSelectedRegion());
            } else {
                // Open on current note if current note is not highlighted.
                model.openNoteProperties(note.getValidBounds());
            }
        }

        @Override
        public void openLyricConfig(Note note) {
            model.openLyricConfig(note.getAbsPositionMs());
        }

        @Override
        public void startDrag(DragHandler newDragHandler) {
            dragHandler = newDragHandler;
        }

        @Override
        public void clearCache(Note note) {
            if (playbackManager.isHighlighted(note)) {
                List<Note> highlightedNotes = playbackManager.getHighlightedNotes();
                model.clearCache(
                        highlightedNotes.get(0).getAbsPositionMs(),
                        highlightedNotes.get(highlightedNotes.size() - 1).getAbsPositionMs());
            } else {
                // Clear current note's cache if current note is not highlighted.
                model.clearCache(note.getAbsPositionMs(), note.getAbsPositionMs());
            }
        }
    };

    private EnvelopeCallback getEnvelopeCallback(final int positionMs) {
        return new EnvelopeCallback() {
            @Override
            public void modifySongEnvelope(EnvelopeData oldData, EnvelopeData newData) {
                Runnable redoAction = () -> {
                    modifyBackend(newData);
                    refreshNotes(positionMs, positionMs); // Update frontend.
                };
                Runnable undoAction = () -> {
                    modifyBackend(oldData);
                    refreshNotes(positionMs, positionMs); // Update frontend.
                };
                model.recordAction(redoAction, undoAction);
                noteMap.getNote(positionMs).setBackupData(modifyBackend(newData));
            }

            private NoteUpdateData modifyBackend(EnvelopeData updateData) {
                Note toModify = noteMap.getNote(positionMs);
                NoteData mutation = new NoteData(
                        positionMs,
                        toModify.getDurationMs(),
                        PitchUtils.rowNumToPitch(toModify.getRow()),
                        toModify.getLyric(),
                        updateData);
                return model.modifyNote(mutation);
            }
        };
    }

    private PitchbendCallback getPitchbendCallback(final int positionMs) {
        return new PitchbendCallback() {
            @Override
            public void modifySongPitchbend(PitchbendData oldData, PitchbendData newData) {
                Optional<int[]> vibrato = noteMap.getPitchbend(positionMs).getVibrato();
                Runnable redoAction = () -> {
                    modifyBackend(newData.withVibrato(vibrato));
                    refreshNotes(positionMs, positionMs); // Update frontend.
                };
                Runnable undoAction = () -> {
                    modifyBackend(oldData.withVibrato(vibrato));
                    refreshNotes(positionMs, positionMs); // Update frontend.
                };
                model.recordAction(redoAction, undoAction);
                NoteUpdateData update = modifyBackend(newData.withVibrato(vibrato));
                noteMap.getNote(positionMs).setBackupData(update);
            }

            @Override
            public void modifySongVibrato(int[] oldVibrato, int[] newVibrato) {
                PitchbendData data = noteMap.getPitchbend(positionMs).getData();
                Runnable redoAction = () -> {
                    modifyBackend(data.withVibrato(Optional.of(newVibrato)));
                    refreshNotes(positionMs, positionMs); // Update frontend.
                };
                Runnable undoAction = () -> {
                    modifyBackend(data.withVibrato(Optional.of(oldVibrato)));
                    refreshNotes(positionMs, positionMs); // Update frontend.
                };
                model.recordAction(redoAction, undoAction);
                NoteUpdateData update = modifyBackend(data.withVibrato(Optional.of(newVibrato)));
                noteMap.getNote(positionMs).setBackupData(update);
            }

            @Override
            public void startDrag(DragHandler newDragHandler) {
                dragHandler = newDragHandler;
            }

            @Override
            public void readjust() {
                // Redraw if any changes to columns have been made.
                TrackItem pitchbend = noteMap.getPitchbend(positionMs);
                int numColumns = pitchbend.getColumns().size();
                track.insertItem(track.getNoteTrack(), pitchbend);
                if (pitchbend.getColumns().size() > numColumns) {
                    track.removeItem(track.getNoteTrack(), pitchbend);
                    track.insertItem(track.getNoteTrack(), pitchbend);
                }
            }

            private NoteUpdateData modifyBackend(PitchbendData updateData) {
                Note toModify = noteMap.getNote(positionMs);
                NoteData mutation = new NoteData(
                        positionMs,
                        toModify.getDurationMs(),
                        PitchUtils.rowNumToPitch(toModify.getRow()),
                        toModify.getLyric(),
                        updateData);
                return model.modifyNote(mutation);
            }
        };
    }
}
