package com.utsusynth.utsu.view.song.note;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

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
import com.utsusynth.utsu.view.song.TrackItem;
import javafx.beans.property.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
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
public class Note implements TrackItem, Comparable<Note> {
    private final NoteCallback track;
    private final BooleanProperty vibratoEditor;
    private final Lyric lyric;
    private final Localizer localizer;
    private final Quantizer quantizer;
    private final Scaler scaler;
    private final Set<Integer> drawnColumns;

    // UI-independent state.
    private final IntegerProperty currentRow;
    private final DoubleProperty startX;
    private final DoubleProperty widthX;
    private final DoubleProperty overlapWidthX;
    private boolean isValid = true;
    private boolean isHighlighted = false;
    private boolean isDisplayOnly = false;

    // UI-dependent state.
    private StackPane layout;
    private Rectangle note;
    private Rectangle dragEdge;
    private Rectangle overlap;

    private enum SubMode {
        CLICKING, DRAGGING, RESIZING,
    }

    // Temporary cache values.
    private ContextMenu contextMenu;
    private SubMode subMode = SubMode.CLICKING;
    private int positionInNote = 0;
    private int startPos = 0;
    private int startRow = 0;
    private int startDuration = 0;
    private boolean hasMoved = false;
    private NoteUpdateData backupData; // Cache of backend song data for re-adding backend note.

    Note(
            int currentRow,
            double startX,
            double widthX,
            Lyric lyric,
            NoteCallback callback,
            BooleanProperty vibratoEditor,
            BooleanProperty showLyrics,
            BooleanProperty showAliases,
            Localizer localizer,
            Quantizer quantizer,
            Scaler scaler) {
        this.track = callback;
        this.vibratoEditor = vibratoEditor;
        this.localizer = localizer;
        this.quantizer = quantizer;
        this.scaler = scaler;
        this.lyric = lyric;
        this.currentRow = new SimpleIntegerProperty(currentRow);
        this.startX = new SimpleDoubleProperty(startX);
        this.widthX = new SimpleDoubleProperty(widthX);
        overlapWidthX = new SimpleDoubleProperty(0);
        drawnColumns = new HashSet<>();

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
        }, showLyrics, showAliases);
    }

    @Override
    public double getStartX() {
        return startX.get();
    }

    @Override
    public double getWidth() {
        return widthX.get();
    }

    @Override
    public StackPane getElement() {
        return redraw(-1, 0);
    }

    @Override
    public StackPane redraw(int colNum, double offsetX) {
        drawnColumns.add(colNum);
        if (layout != null) {
            layout.translateXProperty().unbind();
        }
        if (note != null) {
            note.widthProperty().unbindBidirectional(widthX);
        }
        if (overlap != null) {
            overlap.widthProperty().unbindBidirectional(overlapWidthX);
        }
        note = new Rectangle();
        note.widthProperty().bindBidirectional(widthX);
        note.setHeight(scaler.scaleY(Quantizer.ROW_HEIGHT).get() - 1);
        note.getStyleClass().addAll(
                "track-note",
                isValid ? "valid" : "invalid",
                isHighlighted ? "highlighted" : "not-highlighted");

        dragEdge = new Rectangle();
        dragEdge.setWidth(isDisplayOnly ? 0 : 3);
        dragEdge.setHeight(note.getHeight());
        dragEdge.setOpacity(0.0);
        dragEdge.setOnMouseEntered(event -> {
            dragEdge.getScene().setCursor(Cursor.W_RESIZE);
        });
        dragEdge.setOnMouseExited(event -> {
            dragEdge.getScene().setCursor(Cursor.DEFAULT);
        });

        overlap = new Rectangle();
        overlap.widthProperty().bindBidirectional(overlapWidthX);
        overlap.setHeight(note.getHeight());
        overlap.getStyleClass().add("note-overlap");

        layout = new StackPane();
        layout.setPickOnBounds(false);
        layout.setAlignment(Pos.CENTER_LEFT);
        layout.setTranslateY(scaler.scaleY(currentRow.get() * Quantizer.ROW_HEIGHT).get());
        layout.translateXProperty().bind(startX.subtract(offsetX));
        if (isDisplayOnly) {
            layout.getChildren().add(note);
            layout.setMouseTransparent(true);
        } else {
            layout.getChildren().addAll(note, overlap, lyric.getElement(), dragEdge);
        }
        StackPane.setAlignment(note, Pos.TOP_LEFT);
        initializeLayout(layout);
        return layout;
    }

    private void initializeLayout(StackPane newLayout) {
        Note thisNote = this;
        newLayout.setOnContextMenuRequested(event -> {
            if (contextMenu != null) {
                contextMenu.hide(); // Hide any existing context menu.
            }
            createContextMenu().show(newLayout, event.getScreenX(), event.getScreenY());
        });

        newLayout.setOnMouseReleased(event -> {
            if (event.getButton() != MouseButton.PRIMARY) {
                return;
            }
            if (subMode == SubMode.DRAGGING && hasMoved) {
                int newPos = getAbsPositionMs();
                int newRow = getRow();
                if (newPos != startPos || newRow != startRow) {
                    track.recordNoteMovement(this, newPos - startPos, newRow - startRow);
                }
                if (isHighlighted) {
                    track.realignHighlights();
                }
            } else if (subMode == SubMode.RESIZING) {
                final int oldDuration = startDuration;
                final int newDuration = getDurationMs();
                if (newDuration != oldDuration) {
                    track.recordAction(
                            () -> resizeNote(newDuration),
                            () -> resizeNote(oldDuration));
                }
                if (isHighlighted) {
                    track.realignHighlights();
                }
            } else {
                if (contextMenu != null) {
                    contextMenu.hide();
                }
                if (event.isShiftDown()) {
                    track.highlightInclusive(this);
                } else if (track.isExclusivelyHighlighted(this)) {
                    lyric.openTextField();
                } else {
                    track.highlightExclusive(this);
                }
            }
            subMode = SubMode.CLICKING;
        });
        newLayout.setOnMouseDragged(event -> {
            if (subMode == SubMode.RESIZING) {
                // Find quantized mouse position.
                int quantSize = quantizer.getQuant();
                int newQuant = (int) Math.floor(
                        (scaler.unscaleX(event.getX()) + getAbsPositionMs()) / quantSize);

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
                        .floor(scaler.unscaleY(event.getY()) / Quantizer.ROW_HEIGHT))
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
                        (int) Math.floor(scaler.unscaleX(event.getX()) / curQuantSize);
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
                if (track.getBounds(thisNote).getMinMs() + positionChange < 0) {
                    newQuant = oldQuant;
                }

                // Actual movement.
                if (oldRow != newRow || oldQuant != newQuant) {
                    int oldPosition = oldQuant * curQuantSize;
                    int newPosition = newQuant * curQuantSize;
                    track.moveNote(thisNote, newPosition - oldPosition, newRow - oldRow);
                    hasMoved = true;
                }
                subMode = SubMode.DRAGGING;
            }
        });
        newLayout.setOnMousePressed(event -> {
            if (newLayout.getScene().getCursor() == Cursor.W_RESIZE) {
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

    private ContextMenu createContextMenu() {
        Note thisNote = this;
        contextMenu = new ContextMenu();
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
        vibratoMenuItem.setSelected(track.hasVibrato(getAbsPositionMs()));
        vibratoMenuItem.setOnAction(action -> {
            track.setHasVibrato(getAbsPositionMs(), vibratoMenuItem.isSelected());
        });
        CheckMenuItem vibratoEditorMenuItem = new CheckMenuItem("Vibrato Editor");
        vibratoEditorMenuItem.setSelected(vibratoEditor.get());
        vibratoEditorMenuItem.setOnAction(event -> {
            vibratoEditor.set(vibratoEditorMenuItem.isSelected());
        });
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
        return contextMenu;
    }

    @Override
    public Set<Integer> getColumns() {
        return drawnColumns;
    }

    @Override
    public void clearColumns() {
        drawnColumns.clear();
    }

    public int getRow() {
        return currentRow.get();
    }

    public int getAbsPositionMs() {
        return RoundUtils.round(scaler.unscalePos(getStartX()));
    }

    public int getDurationMs() {
        return RoundUtils.round(scaler.unscaleX(getWidth() + 1));
    }

    public String getLyric() {
        return lyric.getLyric();
    }

    public RegionBounds getBounds() {
        int absPosition = getAbsPositionMs();
        return new RegionBounds(absPosition, absPosition + getDurationMs());
    }

    public RegionBounds getValidBounds() {
        if (!isValid()) {
            return RegionBounds.INVALID;
        }
        int absPosition = getAbsPositionMs();
        int validDur = (int) Math.round(scaler.unscaleX(getWidth() - overlapWidthX.get()));
        return new RegionBounds(absPosition, absPosition + validDur);
    }

    /**
     * Sets a note's highlighted state. Idempotent. Should only be called from track.
     * 
     * @param highlighted Whether the note should be highlighted.
     */
    public void setHighlighted(boolean highlighted) {
        isHighlighted = highlighted;
        if (note != null) {
            note.getStyleClass().set(2, highlighted ? "highlighted" : "not-highlighted");
        }

        if (!isHighlighted) {
            lyric.closeTextFieldIfNeeded();
        }
    }

    public boolean isValid() {
        return isValid;
    }

    public void setValid(boolean isValid) {
        this.isValid = isValid;
        if (note != null) {
            note.getStyleClass().set(1, isValid ? "valid" : "invalid");
        }
        if (!isValid) {
            lyric.setVisibleAlias("");
            adjustForOverlap(Integer.MAX_VALUE);
        }
    }

    /** Make note non-interactable and only a background object. Irreversible. */
    public void setToDisplayOnly() {
        isDisplayOnly = true;
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
        double oldOverlap = overlapWidthX.get();
        double noteWidthMs = scaler.unscaleX(getWidth());
        if (noteWidthMs > distanceToNextNote) {
            overlapWidthX.set(scaler.scaleX(noteWidthMs - distanceToNextNote).get());
        } else {
            overlapWidthX.set(0);
        }
        // Resize note if necessary.
        if (overlapWidthX.get() < oldOverlap) {
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
        startX.set(scaler.scalePos(newPositionMs).get());
        int newRow = getRow() + rowDelta;
        currentRow.set(newRow);
        if (layout != null) {
            layout.setTranslateY(scaler.scaleY(newRow * Quantizer.ROW_HEIGHT).get());
        }
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
        widthX.set(scaler.scaleX(newDuration).get() - 1);
        adjustDragEdge(newDuration);
        track.updateNote(this);
    }

    private void adjustDragEdge(double newDuration) {
        if (dragEdge == null || overlap == null) {
            return;
        }
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
