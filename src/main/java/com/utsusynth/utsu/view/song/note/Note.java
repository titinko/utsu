package com.utsusynth.utsu.view.song.note;

import java.util.Optional;
import com.utsusynth.utsu.common.RegionBounds;
import com.utsusynth.utsu.common.data.EnvelopeData;
import com.utsusynth.utsu.common.data.NoteConfigData;
import com.utsusynth.utsu.common.data.NoteData;
import com.utsusynth.utsu.common.data.NoteUpdateData;
import com.utsusynth.utsu.common.data.PitchbendData;
import com.utsusynth.utsu.common.i18n.Localizer;
import com.utsusynth.utsu.common.quantize.Quantizer;
import com.utsusynth.utsu.common.quantize.Scaler;
import com.utsusynth.utsu.common.utils.PitchUtils;
import com.utsusynth.utsu.common.utils.RoundUtils;
import javafx.beans.property.BooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

/**
 * Frontend representation of a note. The backend representation is found in the model's Note class.
 */
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

    private enum SubMode {
        CLICKING, DRAGGING, RESIZING,
    }

    // Temporary cache values.
    private SubMode subMode = SubMode.CLICKING;
    private int positionInNote = 0;
    private int startPos = 0;
    private int startRow = 0;
    private int startDuration = 0;
    private boolean hasMoved = false;
    private NoteUpdateData backupData; // Cache of backend song data for re-adding backend note.

    Note(
            Rectangle note,
            Rectangle dragEdge,
            Rectangle overlap,
            Lyric lyric,
            StackPane layout,
            NoteCallback callback,
            BooleanProperty vibratoEditor,
            BooleanProperty showLyrics,
            BooleanProperty showAliases,
            Localizer localizer,
            Quantizer quantizer,
            Scaler scaler) {
        this.note = note;
        this.dragEdge = dragEdge;
        this.overlap = overlap;
        this.track = callback;
        this.quantizer = quantizer;
        this.scaler = scaler;
        this.lyric = lyric;
        this.layout = layout;
        layout.getChildren()
                .addAll(this.note, this.overlap, this.lyric.getElement(), this.dragEdge);
        StackPane.setAlignment(this.note, Pos.TOP_LEFT);

        Note thisNote = this;
        lyric.initialize(new LyricCallback() {
            @Override
            public void setSongLyric(String newLyric) {
                thisNote.track.updateNote(thisNote);
            }

            @Override
            public void replaceSongLyric(String oldLyric, String newLyric) {
                thisNote.track.updateNote(thisNote);
                thisNote.track.recordAction(() -> {
                    lyric.setVisibleLyric(newLyric);
                    thisNote.track.updateNote(thisNote);
                }, () -> {
                    lyric.setVisibleLyric(oldLyric);
                    thisNote.track.updateNote(thisNote);
                });
            }

            @Override
            public void adjustColumnSpan() {
                // TODO: Factor lyric width into this.
                thisNote.adjustDragEdge(thisNote.getDurationMs());
            }

            @Override
            public void bringToFront() {
                thisNote.getElement().toFront();
            }
        }, showLyrics, showAliases);

        // Create context menu.
        this.contextMenu = new ContextMenu();
        MenuItem cutMenuItem = new MenuItem("Cut");
        cutMenuItem.setOnAction(action -> {
            track.copyNote(thisNote);
            deleteNote();
        });
        MenuItem copyMenuItem = new MenuItem("Copy");
        copyMenuItem.setOnAction(action -> track.copyNote(thisNote));
        MenuItem deleteMenuItem = new MenuItem("Delete");
        deleteMenuItem.setOnAction(action -> deleteNote());
        CheckMenuItem vibratoMenuItem = new CheckMenuItem("Vibrato");
        if (track != null) {
            vibratoMenuItem.setSelected(track.hasVibrato(getAbsPositionMs()));
        }
        vibratoMenuItem.setOnAction(action -> {
            track.setHasVibrato(getAbsPositionMs(), vibratoMenuItem.isSelected());
        });
        CheckMenuItem vibratoEditorMenuItem = new CheckMenuItem("Vibrato Editor");
        vibratoEditorMenuItem.selectedProperty().bindBidirectional(vibratoEditor);
        MenuItem lyricConfigItem = new MenuItem("Open Lyric Config");
        lyricConfigItem.setOnAction(action -> track.openLyricConfig(this));
        MenuItem notePropertiesItem = new MenuItem("Note Properties");
        notePropertiesItem.setOnAction(action -> track.openNoteProperties(this));
        MenuItem clearCacheItem = new MenuItem("Clear Cache");
        clearCacheItem.setOnAction(action -> track.clearCache(this));
        contextMenu.getItems().addAll(
                cutMenuItem,
                copyMenuItem,
                deleteMenuItem,
                new SeparatorMenuItem(),
                vibratoMenuItem,
                vibratoEditorMenuItem,
                new SeparatorMenuItem(),
                lyricConfigItem,
                new SeparatorMenuItem(),
                notePropertiesItem,
                new SeparatorMenuItem(),
                clearCacheItem);
        contextMenu.setOnShowing(event -> {
            cutMenuItem.setText(localizer.getMessage("menu.edit.cut"));
            copyMenuItem.setText(localizer.getMessage("menu.edit.copy"));
            deleteMenuItem.setText(localizer.getMessage("menu.edit.delete"));
            vibratoMenuItem.setText(localizer.getMessage("song.note.vibrato"));
            vibratoEditorMenuItem.setText(localizer.getMessage("song.note.vibratoEditor"));
            lyricConfigItem.setText(localizer.getMessage("song.note.openLyricConfig"));
            notePropertiesItem.setText(localizer.getMessage("menu.edit.noteProperties"));
            clearCacheItem.setText(localizer.getMessage("song.note.clearCache"));
        });
        layout.setOnContextMenuRequested(event -> {
            contextMenu.hide();
            contextMenu.show(layout, event.getScreenX(), event.getScreenY());
        });

        layout.setOnMouseReleased(event -> {
            if (event.getButton() != MouseButton.PRIMARY) {
                return;
            }
            if (subMode == SubMode.DRAGGING && hasMoved) {
                int newPos = getAbsPositionMs();
                int newRow = getRow();
                if (newPos != startPos || newRow != startRow) {
                    this.track.recordNoteMovement(this, newPos - startPos, newRow - startRow);
                }
                if (note.getStyleClass().contains("highlighted")) {
                    this.track.realignHighlights();
                }
            } else if (subMode == SubMode.RESIZING) {
                final int oldDuration = startDuration;
                final int newDuration = getDurationMs();
                if (newDuration != oldDuration) {
                    this.track.recordAction(
                            () -> resizeNote(newDuration),
                            () -> resizeNote(oldDuration));
                }
                if (note.getStyleClass().contains("highlighted")) {
                    this.track.realignHighlights();
                }
            } else {
                contextMenu.hide();
                if (event.isShiftDown()) {
                    this.track.highlightInclusive(this);
                } else if (this.track.isExclusivelyHighlighted(this)) {
                    this.lyric.openTextField();
                } else {
                    this.track.highlightExclusive(this);
                }
            }
            subMode = SubMode.CLICKING;
        });
        layout.setOnMouseDragged(event -> {
            if (subMode == SubMode.RESIZING) {
                // Find quantized mouse position.
                int quantSize = quantizer.getQuant();
                int newQuant = (int) Math.floor(
                        (scaler.unscaleX(event.getX()) * 1.0 + getAbsPositionMs()) / quantSize);

                // Find what to compare quantized mouse position to.
                int oldEndPos = getAbsPositionMs() + getDurationMs();
                int increasingQuantEnd = (int) Math.floor(oldEndPos * 1.0 / quantSize);
                int decreasingQuantEnd = (int) (Math.ceil(oldEndPos * 1.0 / quantSize)) - 1;

                // Calculate actual change in duration.
                int oldPosition = getAbsPositionMs();
                int newPosition = newQuant * quantSize;
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
                // Check whether new row is in bounds for every highlighted note.
                int lowestNewRow = track.getLowestRow(thisNote) + (newRow - oldRow);
                int highestNewRow = track.getHighestRow(thisNote) + (newRow - oldRow);
                if (!(lowestNewRow >= 0 && highestNewRow < 7 * PitchUtils.PITCHES.size())) {
                    newRow = oldRow;
                }

                // Handle horizontal movement.
                int curQuantSize = quantizer.getQuant(); // Ensure constant quantization.
                // Determine whether a note is aligned with the current quantization.
                boolean aligned = getAbsPositionMs() % curQuantSize == 0;
                int oldQuantInNote = positionInNote / curQuantSize;
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
                    quantChange *= curQuantSize;
                    // Both values are in the smallest quantization.
                    int truncatedStart = (getAbsPositionMs() / curQuantSize) * curQuantSize;
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
                    curQuantSize = 1;
                }
                int oldQuant = getQuantizedStart(curQuantSize);
                int newQuant = oldQuant + quantChange;

                // Check column bounds of leftmost note.
                int positionChange = (newQuant - oldQuant) * curQuantSize;
                if (this.track.getBounds(thisNote).getMinMs() + positionChange < 0) {
                    newQuant = oldQuant;
                }

                // Actual movement.
                if (oldRow != newRow || oldQuant != newQuant) {
                    int oldPosition = oldQuant * curQuantSize;
                    int newPosition = newQuant * curQuantSize;
                    this.track.moveNote(thisNote, newPosition - oldPosition, newRow - oldRow);
                    hasMoved = true;
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
                startDuration = getDurationMs();
            } else {
                subMode = SubMode.CLICKING; // Note that this may become dragging in the future.
                positionInNote = RoundUtils.round(scaler.unscaleX(event.getX()));
                startPos = getAbsPositionMs();
                startRow = getRow();
                hasMoved = false;
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
            overlap.setWidth(scaler.scaleX(noteWidth - distanceToNextNote).get());
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
        layout.setTranslateX(scaler.scalePos(newPositionMs).get());
        int newRow = getRow() + rowDelta;
        layout.setTranslateY(scaler.scaleY(newRow * Quantizer.ROW_HEIGHT).get());
    }

    public void setBackupData(NoteUpdateData backupData) {
        this.backupData = backupData;
    }

    public NoteData getNoteData() {
        Optional<EnvelopeData> envelopeData = Optional.empty();
        Optional<PitchbendData> pitchbendData = Optional.empty();
        Optional<NoteConfigData> configData = Optional.empty();
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
                Optional.empty(),
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
        note.setWidth(scaler.scaleX(newDuration).get() - 1);
        adjustDragEdge(newDuration);
        track.updateNote(this);
    }

    private void adjustDragEdge(double newDuration) {
        double scaledDuration = scaler.scaleX(newDuration).get();
        StackPane
                .setMargin(dragEdge, new Insets(0, 0, 0, scaledDuration - dragEdge.getWidth() - 1));
        StackPane.setMargin(overlap, new Insets(0, 0, 0, scaledDuration - overlap.getWidth() - 1));
    }

    private int getQuantizedStart() {
        return getQuantizedStart(quantizer.getQuant());
    }

    private int getQuantizedStart(int quantization) {
        return getAbsPositionMs() / quantization;
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
