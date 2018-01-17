package com.utsusynth.utsu.view.note;

import com.google.common.base.Optional;
import com.utsusynth.utsu.UtsuController.Mode;
import com.utsusynth.utsu.common.PitchUtils;
import com.utsusynth.utsu.common.data.EnvelopeData;
import com.utsusynth.utsu.common.data.NoteData;
import com.utsusynth.utsu.common.data.PitchbendData;
import com.utsusynth.utsu.common.exception.NoteAlreadyExistsException;
import com.utsusynth.utsu.common.quantize.Quantizer;
import com.utsusynth.utsu.common.quantize.Scaler;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

public class TrackNote {
    private final StackPane layout;
    private final Rectangle note;
    private final Rectangle dragEdge;
    private final Rectangle overlap;
    private final ContextMenu contextMenu;
    private final TrackNoteCallback track;
    private final TrackLyric lyric;
    private final TrackVibrato vibrato;
    private final Quantizer quantizer;
    private final Scaler scaler;

    // Temporary cache values.
    private enum SubMode {
        CLICKING, DRAGGING, RESIZING,
    }

    private SubMode subMode;
    private int positionInNote;

    TrackNote(
            Rectangle note,
            Rectangle dragEdge,
            Rectangle overlap,
            TrackLyric lyric,
            TrackVibrato vibrato,
            StackPane layout,
            TrackNoteCallback callback,
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
        this.vibrato = vibrato;
        this.layout = layout;
        this.layout.getChildren()
                .addAll(this.note, this.overlap, this.lyric.getElement(), this.dragEdge);

        TrackNote thisNote = this;
        lyric.initialize(new TrackLyricCallback() {
            @Override
            public void setHighlighted(boolean highlighted) {
                callback.setHighlighted(thisNote, false);
            }

            @Override
            public void setSongLyric(String newLyric) {
                thisNote.updateNote(
                        thisNote.getAbsPosition(),
                        thisNote.getAbsPosition(),
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

        // Create context menu.
        this.contextMenu = new ContextMenu();
        MenuItem deleteMenuItem = new MenuItem("Delete");
        deleteMenuItem.setOnAction(action -> deleteNote());
        CheckMenuItem vibratoMenuItem = new CheckMenuItem("Vibrato");
        vibratoMenuItem.setSelected(vibrato.getVibrato().isPresent());
        vibratoMenuItem.setOnAction(action -> {
            if (vibratoMenuItem.isSelected()) {
                vibrato.addDefaultVibrato();
            } else {
                vibrato.clearVibrato();
            }
            track.modifySongVibrato(getAbsPosition()); // Update backend.
        });
        contextMenu.getItems().addAll(deleteMenuItem, vibratoMenuItem);
        layout.setOnContextMenuRequested(event -> {
            contextMenu.hide();
            contextMenu.show(layout, event.getScreenX(), event.getScreenY());
        });

        layout.setOnMouseClicked(event -> {
            if (event.getButton() != MouseButton.PRIMARY) {
                return;
            }
            if (track.getCurrentMode() == Mode.DELETE) {
                deleteNote();
            } else if (subMode == SubMode.CLICKING) {
                contextMenu.hide();
                if (this.track.isHighlighted(this)) {
                    this.lyric.openTextField();
                } else {
                    this.track.setHighlighted(this, true);
                }
            }
            subMode = SubMode.CLICKING;
        });
        layout.setOnMouseDragged(event -> {
            if (subMode == SubMode.RESIZING) {
                // Find quantized mouse position.
                int quantSize = Quantizer.COL_WIDTH / quantizer.getQuant();
                int newQuant = (int) Math.floor(
                        (scaler.unscaleX(event.getX()) * 1.0 + getAbsPosition()) / quantSize);

                // Find what to compare quantized mouse position to.
                int oldEndPos = getAbsPosition() + getDuration();
                int increasingQuantEnd = (int) Math.floor(oldEndPos * 1.0 / quantSize);
                int decreasingQuantEnd = (int) (Math.ceil(oldEndPos * 1.0 / quantSize)) - 1;

                // Calculate actual change in duration.
                int oldPosition = getAbsPosition();
                int newPosition = newQuant * (Quantizer.COL_WIDTH / quantizer.getQuant());
                int positionChange = newPosition - oldPosition;

                // Increase or decrease duration.
                if (newQuant > increasingQuantEnd) {
                    resizeNote(positionChange);
                } else if (newQuant >= getQuantizedStart() && newQuant < decreasingQuantEnd) {
                    resizeNote(positionChange + quantSize);
                }
            } else {
                // Handle vertical movement and check against row bounds.
                int oldRow = getRow();
                int newRow = ((int) Math
                        .floor(scaler.unscaleY(event.getY()) * 1.0 / Quantizer.ROW_HEIGHT))
                        + oldRow;
                if (!track.isInBounds(newRow)) {
                    newRow = oldRow;
                }

                // Handle horizontal movement.
                int curQuant = quantizer.getQuant(); // Ensure constant quantization.
                int curQuantSize = Quantizer.COL_WIDTH / curQuant;
                // Determine whether a note is aligned with the current quantization.
                boolean aligned = getAbsPosition() % curQuantSize == 0;
                int oldQuantInNote = positionInNote / (Quantizer.COL_WIDTH / curQuant);
                int newQuantInNote =
                        (int) Math.floor(scaler.unscaleX(event.getX()) * 1.0 / curQuantSize);
                int quantChange = newQuantInNote - oldQuantInNote;
                if (!aligned) {
                    // Possibly increase quantChange by 1.
                    int minBound = getDuration();
                    int ceilQuantDur = (int) Math.ceil(getDuration() * 1.0 / curQuantSize);
                    if (scaler.unscaleX(event.getX()) > minBound && newQuantInNote < ceilQuantDur) {
                        quantChange++;
                    }
                    // Convert to smallest quantization.
                    quantChange *= (Quantizer.COL_WIDTH / curQuant);
                    // Both values are in the smallest quantization.
                    int truncatedStart =
                            getAbsPosition() / curQuantSize * (Quantizer.COL_WIDTH / curQuant);
                    int actualStart = getAbsPosition();
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
                    moveNote(oldQuant, newQuant, curQuant, newRow);
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
                positionInNote = (int) Math.round(scaler.unscaleX(event.getX()));
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

    public Optional<int[]> getVibrato() {
        return vibrato.getVibrato();
    }

    public int getAbsPosition() {
        return (int) scaler.unscaleX(layout.getTranslateX());
    }

    public int getDuration() {
        return (int) scaler.unscaleX(note.getWidth() + 1);
    }

    public String getLyric() {
        return lyric.getLyric();
    }

    /**
     * Sets a note's highlighted state. Should only be called from track.
     * 
     * @param highlighted Whether the note should be highlighted.
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
            lyric.setVisibleAlias("");
            adjustForOverlap(Integer.MAX_VALUE);
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
            resizeNote(getDuration());
        }
        adjustDragEdge(getDuration());
    }

    /** Sets the lyric that will be used to render the note. */
    public void setTrueLyric(String trueLyric) {
        this.lyric.setVisibleAlias(trueLyric);
    }

    private void deleteNote() {
        contextMenu.hide();
        lyric.closeTextFieldIfNeeded();
        if (note.getStyleClass().contains("valid-note")) {
            track.removeSongNote(getAbsPosition());
        }
        track.removeTrackNote(this);
    }

    private void resizeNote(int newDuration) {
        note.setWidth(scaler.scaleX(newDuration) - 1);
        adjustDragEdge(newDuration);
        updateNote(
                getAbsPosition(),
                getAbsPosition(),
                getDuration(),
                getRow(),
                newDuration,
                lyric.getLyric());
    }

    private void moveNote(int oldQuant, int newQuant, int quantization, int newRow) {
        int oldPosition = oldQuant * (Quantizer.COL_WIDTH / quantization);
        int newPosition = newQuant * (Quantizer.COL_WIDTH / quantization);
        layout.setTranslateX(scaler.scaleX(newPosition));
        layout.setTranslateY(scaler.scaleY(newRow * Quantizer.ROW_HEIGHT));
        int curDuration = getDuration();
        adjustDragEdge(curDuration);
        updateNote(oldPosition, newPosition, curDuration, newRow, curDuration, lyric.getLyric());
    }

    private void adjustDragEdge(double newDuration) {
        double scaledDuration = scaler.scaleX(newDuration);
        StackPane
                .setMargin(dragEdge, new Insets(0, 0, 0, scaledDuration - dragEdge.getWidth() - 1));
        StackPane.setMargin(overlap, new Insets(0, 0, 0, scaledDuration - overlap.getWidth() - 1));
    }

    private void updateNote(
            int oldPosition,
            int newPosition,
            int oldDuration,
            int newRow,
            int newDuration,
            String newLyric) {
        // System.out.println("***");
        Optional<EnvelopeData> envelope = Optional.absent();
        Optional<PitchbendData> portamento = Optional.absent();
        if (note.getStyleClass().contains("valid-note")) {
            // System.out.println(String.format(
            // "Moving from valid %d, %s", oldQuant, lyric.getLyric()));
            envelope = track.getEnvelope(oldPosition);
            portamento = track.getPortamento(oldPosition);
            track.removeSongNote(oldPosition);
        } else {
            // System.out.println(String.format(
            // "Moving from invalid %d, %s", oldQuant, lyric.getLyric()));
        }
        // System.out.println(String.format("Moving to %d, %d, %s", newRow, newQuant, newLyric));
        try {
            setValid(true);
            Optional<PitchbendData> pitchbend = Optional.absent();
            if (portamento.isPresent()) {
                pitchbend = Optional.of(portamento.get().withVibrato(vibrato.getVibrato()));
            }
            String newPitch = PitchUtils.rowNumToPitch(newRow);
            NoteData toAdd = new NoteData(
                    newPosition,
                    newDuration,
                    newPitch,
                    newLyric,
                    envelope,
                    pitchbend,
                    Optional.absent());
            track.addSongNote(this, toAdd);
        } catch (NoteAlreadyExistsException e) {
            setValid(false);
            // System.out.println("WARNING: New note is invalid!");
        }
    }

    private int getQuantizedStart() {
        return getQuantizedStart(quantizer.getQuant());
    }

    private int getQuantizedStart(int quantization) {
        return getAbsPosition() / (Quantizer.COL_WIDTH / quantization);
    }
}
