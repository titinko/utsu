package com.utsusynth.utsu.view.song.note;

import com.google.common.base.Optional;
import com.utsusynth.utsu.common.RegionBounds;
import com.utsusynth.utsu.common.data.EnvelopeData;
import com.utsusynth.utsu.common.data.NoteConfigData;
import com.utsusynth.utsu.common.data.NoteData;
import com.utsusynth.utsu.common.data.NoteUpdateData;
import com.utsusynth.utsu.common.data.PitchbendData;
import com.utsusynth.utsu.common.quantize.Quantizer;
import com.utsusynth.utsu.common.quantize.Scaler;
import com.utsusynth.utsu.common.utils.PitchUtils;
import com.utsusynth.utsu.common.utils.RoundUtils;
import javafx.beans.property.BooleanProperty;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

public class Note implements Comparable<Note> {
    private final StackPane layout;
    private final Rectangle note;
    private final Rectangle dragEdge;
    private final Rectangle overlap;
    private final ContextMenu contextMenu;
    private final NoteCallback track;
    private final Lyric lyric;
    private final Quantizer quantizer;
    private final Scaler scaler;

    // Temporary cache values.
    private enum SubMode {
        CLICKING, DRAGGING, RESIZING,
    }

    private SubMode subMode;
    private int positionInNote;
    private NoteUpdateData backupData; // Cache of backend song data for re-adding backend note.

    Note(
            Rectangle note,
            Rectangle dragEdge,
            Rectangle overlap,
            Lyric lyric,
            StackPane layout,
            NoteCallback callback,
            BooleanProperty vibratoEditor,
            Quantizer quantizer,
            Scaler scaler) {
        this.note = note;
        this.dragEdge = dragEdge;
        this.overlap = overlap;
        this.track = callback;
        this.subMode = SubMode.CLICKING;
        this.positionInNote = 0;
        this.quantizer = quantizer;
        this.scaler = scaler;
        this.lyric = lyric;
        this.layout = layout;
        this.layout.getChildren()
                .addAll(this.note, this.overlap, this.lyric.getElement(), this.dragEdge);

        Note thisNote = this;
        lyric.initialize(new LyricCallback() {
            @Override
            public void setSongLyric(String newLyric) {
                thisNote.track.updateNote(thisNote);
            }

            @Override
            public void adjustColumnSpan() {
                // TODO: Factor lyric width into this.
                thisNote.adjustDragEdge(thisNote.getDurationMs());
            }
        });

        // Create context menu.
        this.contextMenu = new ContextMenu();
        MenuItem deleteMenuItem = new MenuItem("Delete");
        deleteMenuItem.setOnAction(action -> deleteNote());
        CheckMenuItem vibratoMenuItem = new CheckMenuItem("Vibrato");
        vibratoMenuItem.setSelected(track.hasVibrato(getAbsPositionMs()));
        vibratoMenuItem.setOnAction(action -> {
            track.setHasVibrato(getAbsPositionMs(), vibratoMenuItem.isSelected());
        });
        CheckMenuItem vibratoEditorMenuItem = new CheckMenuItem("Vibrato Editor");
        vibratoEditorMenuItem.selectedProperty().bindBidirectional(vibratoEditor);
        MenuItem notePropertiesItem = new MenuItem("Note Properties");
        notePropertiesItem.setOnAction(action -> track.openNoteProperties(this));
        contextMenu.getItems().addAll(
                deleteMenuItem,
                vibratoMenuItem,
                new SeparatorMenuItem(),
                vibratoEditorMenuItem,
                new SeparatorMenuItem(),
                notePropertiesItem);
        layout.setOnContextMenuRequested(event -> {
            contextMenu.hide();
            contextMenu.show(layout, event.getScreenX(), event.getScreenY());
        });

        layout.setOnMouseReleased(event -> {
            if (event.getButton() != MouseButton.PRIMARY) {
                return;
            }
            if (subMode == SubMode.CLICKING) {
                contextMenu.hide();
                if (event.isShiftDown()) {
                    this.track.highlightInclusive(this);
                } else if (this.track.isExclusivelyHighlighted(this)) {
                    this.lyric.openTextField();
                } else {
                    this.track.highlightExclusive(this);
                }
            } else if (subMode == SubMode.DRAGGING || subMode == SubMode.RESIZING) {
                if (note.getStyleClass().contains("highlighted")) {
                    this.track.realignHighlights();
                }
            }
            subMode = SubMode.CLICKING;
        });
        layout.setOnMouseDragged(event -> {
            if (subMode == SubMode.RESIZING) {
                // Find quantized mouse position.
                int quantSize = Quantizer.COL_WIDTH / quantizer.getQuant();
                int newQuant = (int) Math.floor(
                        (scaler.unscaleX(event.getX()) * 1.0 + getAbsPositionMs()) / quantSize);

                // Find what to compare quantized mouse position to.
                int oldEndPos = getAbsPositionMs() + getDurationMs();
                int increasingQuantEnd = (int) Math.floor(oldEndPos * 1.0 / quantSize);
                int decreasingQuantEnd = (int) (Math.ceil(oldEndPos * 1.0 / quantSize)) - 1;

                // Calculate actual change in duration.
                int oldPosition = getAbsPositionMs();
                int newPosition = newQuant * (Quantizer.COL_WIDTH / quantizer.getQuant());
                int positionChange = newPosition - oldPosition;

                // Increase or decrease duration.
                if (newQuant > increasingQuantEnd) {
                    resizeNote(positionChange);
                } else if (newQuant >= getQuantizedStart() && newQuant < decreasingQuantEnd) {
                    resizeNote(positionChange + quantSize);
                }
            } else {
                // Handle vertical movement.
                int oldRow = getRow();
                int newRow = ((int) Math
                        .floor(scaler.unscaleY(event.getY()) * 1.0 / Quantizer.ROW_HEIGHT))
                        + oldRow;
                // Check whether new row is in bounds.
                if (!(newRow >= 0 && newRow < 7 * PitchUtils.PITCHES.size())) {
                    newRow = oldRow;
                }

                // Handle horizontal movement.
                int curQuant = quantizer.getQuant(); // Ensure constant quantization.
                int curQuantSize = Quantizer.COL_WIDTH / curQuant;
                // Determine whether a note is aligned with the current quantization.
                boolean aligned = getAbsPositionMs() % curQuantSize == 0;
                int oldQuantInNote = positionInNote / (Quantizer.COL_WIDTH / curQuant);
                int newQuantInNote =
                        (int) Math.floor(scaler.unscaleX(event.getX()) * 1.0 / curQuantSize);
                int quantChange = newQuantInNote - oldQuantInNote;
                if (!aligned) {
                    // Possibly increase quantChange by 1.
                    int minBound = getDurationMs();
                    int ceilQuantDur = (int) Math.ceil(getDurationMs() * 1.0 / curQuantSize);
                    if (scaler.unscaleX(event.getX()) > minBound && newQuantInNote < ceilQuantDur) {
                        quantChange++;
                    }
                    // Convert to smallest quantization.
                    quantChange *= (Quantizer.COL_WIDTH / curQuant);
                    // Both values are in the smallest quantization.
                    int truncatedStart =
                            getAbsPositionMs() / curQuantSize * (Quantizer.COL_WIDTH / curQuant);
                    int actualStart = getAbsPositionMs();
                    // Align start quant with true quantization.
                    if (quantChange > 0) {
                        // Subtract from quantChange.
                        quantChange -= (actualStart - truncatedStart);
                    } else if (quantChange < 0) {
                        // Add to quantChange.
                        quantChange += (truncatedStart + Quantizer.COL_WIDTH - actualStart);
                    }
                    // Adjust curQuant now that quantChange has been corrected.
                    curQuant = Quantizer.COL_WIDTH;
                    curQuantSize = 1;
                }
                int oldQuant = getQuantizedStart(curQuant);
                int newQuant = oldQuant + quantChange;

                // Check column bounds.
                if (newQuant < 0) {
                    newQuant = oldQuant;
                }

                // Actual movement.
                if (oldRow != newRow || oldQuant != newQuant) {
                    int oldPosition = oldQuant * (Quantizer.COL_WIDTH / curQuant);
                    int newPosition = newQuant * (Quantizer.COL_WIDTH / curQuant);
                    this.track.moveNote(this, newPosition - oldPosition, newRow - oldRow);
                    // moveNote(oldQuant, newQuant, curQuant, newRow);
                }
                subMode = SubMode.DRAGGING;
            }
        });
        dragEdge.setOnMouseEntered(event -> {
            dragEdge.getScene().setCursor(Cursor.W_RESIZE);
        });
        dragEdge.setOnMouseExited(event -> {
            dragEdge.getScene().setCursor(Cursor.DEFAULT);
        });
        layout.setOnMousePressed(event -> {
            if (layout.getScene().getCursor() == Cursor.W_RESIZE) {
                subMode = SubMode.RESIZING;
            } else {
                positionInNote = RoundUtils.round(scaler.unscaleX(event.getX()));
                subMode = SubMode.CLICKING; // Note that this may become dragging in the future.
            }
        });
    }

    public StackPane getElement() {
        return layout;
    }

    public int getRow() {
        return (int) scaler.unscaleY(layout.getTranslateY()) / Quantizer.ROW_HEIGHT;
    }

    public int getAbsPositionMs() {
        return RoundUtils.round(scaler.unscalePos(layout.getTranslateX()));
    }

    public int getDurationMs() {
        return RoundUtils.round(scaler.unscaleX(note.getWidth() + 1));
    }

    public String getLyric() {
        return lyric.getLyric();
    }

    public RegionBounds getBounds() {
        int absPosition = getAbsPositionMs();
        return new RegionBounds(absPosition, absPosition + getDurationMs());
    }

    public RegionBounds getValidBounds() {
        if (note.getStyleClass().contains("invalid")) {
            return RegionBounds.INVALID;
        }
        int absPosition = getAbsPositionMs();
        int validDur = (int) Math.round(scaler.unscaleX(note.getWidth() - overlap.getWidth()));
        return new RegionBounds(absPosition, absPosition + validDur);
    }

    /**
     * Sets a note's highlighted state. Idempotent. Should only be called from track.
     * 
     * @param highlighted Whether the note should be highlighted.
     */
    public void setHighlighted(boolean highlighted) {
        note.getStyleClass().set(2, highlighted ? "highlighted" : "not-highlighted");

        if (!highlighted) {
            lyric.closeTextFieldIfNeeded();
        }
    }

    public boolean isValid() {
        return note.getStyleClass().contains("valid");
    }

    public void setValid(boolean isValid) {
        note.getStyleClass().set(1, isValid ? "valid" : "invalid");
        if (!isValid) {
            lyric.setVisibleAlias("");
            adjustForOverlap(Integer.MAX_VALUE);
        }
    }

    public boolean isLyricInputOpen() {
        return lyric.isTextFieldOpen();
    }

    public void openLyricInput() {
        if (!lyric.isTextFieldOpen()) {
            lyric.openTextField(); // Don't open it text field already open.
        }
    }

    public void adjustForOverlap(int distanceToNextNote) {
        double oldOverlap = overlap.getWidth();
        double noteWidth = scaler.unscaleX(this.note.getWidth());
        if (noteWidth > distanceToNextNote) {
            overlap.setWidth(scaler.scaleX(noteWidth - distanceToNextNote));
        } else {
            overlap.setWidth(0);
        }
        // Resize note if necessary.
        if (overlap.getWidth() < oldOverlap) {
            resizeNote(getDurationMs());
        }
        adjustDragEdge(getDurationMs());
    }

    /** Sets the lyric that will be used to render the note. */
    public void setTrueLyric(String trueLyric) {
        this.lyric.setVisibleAlias(trueLyric);
    }

    /** Only moves the visual note. */
    public void moveNoteElement(int positionMsDelta, int rowDelta) {
        int newPositionMs = getAbsPositionMs() + positionMsDelta;
        layout.setTranslateX(scaler.scalePos(newPositionMs));
        int newRow = getRow() + rowDelta;
        layout.setTranslateY(scaler.scaleY(newRow * Quantizer.ROW_HEIGHT));
    }

    public void setBackupData(NoteUpdateData backupData) {
        this.backupData = backupData;
    }

    public NoteData getNoteData() {
        Optional<EnvelopeData> envelopeData = Optional.absent();
        Optional<PitchbendData> pitchbendData = Optional.absent();
        Optional<NoteConfigData> configData = Optional.absent();
        if (backupData != null) {
            envelopeData = Optional.of(backupData.getEnvelope());
            pitchbendData = Optional.of(backupData.getPitchbend());
            configData = Optional.of(backupData.getConfigData());
        }
        return new NoteData(
                getAbsPositionMs(),
                getDurationMs(),
                PitchUtils.rowNumToPitch(getRow()),
                lyric.getLyric(),
                Optional.absent(),
                envelopeData,
                pitchbendData,
                configData);
    }

    private void deleteNote() {
        contextMenu.hide();
        lyric.closeTextFieldIfNeeded();
        track.deleteNote(this);
    }

    private void resizeNote(int newDuration) {
        note.setWidth(scaler.scaleX(newDuration) - 1);
        adjustDragEdge(newDuration);
        track.updateNote(this);
    }

    private void adjustDragEdge(double newDuration) {
        double scaledDuration = scaler.scaleX(newDuration);
        StackPane
                .setMargin(dragEdge, new Insets(0, 0, 0, scaledDuration - dragEdge.getWidth() - 1));
        StackPane.setMargin(overlap, new Insets(0, 0, 0, scaledDuration - overlap.getWidth() - 1));
    }

    private int getQuantizedStart() {
        return getQuantizedStart(quantizer.getQuant());
    }

    private int getQuantizedStart(int quantization) {
        return getAbsPositionMs() / (Quantizer.COL_WIDTH / quantization);
    }

    @Override
    public int compareTo(Note other) {
        int result = Integer.compare(getAbsPositionMs(), other.getAbsPositionMs());
        if (result == 0 && this != other) {
            return -1; // Only return 0 if object references are actually the same.
        }
        return result;
    }
}
