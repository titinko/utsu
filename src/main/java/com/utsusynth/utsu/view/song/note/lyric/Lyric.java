package com.utsusynth.utsu.view.song.note.lyric;

import com.google.common.collect.ImmutableSet;
import com.utsusynth.utsu.common.quantize.Quantizer;
import com.utsusynth.utsu.common.quantize.Scaler;
import com.utsusynth.utsu.view.song.track.TrackItem;
import javafx.animation.PauseTransition;
import javafx.beans.property.*;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.HashSet;

public class Lyric implements TrackItem {
    private final HashMap<Double, HBox> drawnHBoxes;
    private final HashMap<Double, Label> drawnLyrics;
    private final HashMap<Double, Label> drawnAliases;
    private final HashSet<Integer> drawnColumns;
    private final Scaler scaler;

    // UI-independent state.
    private final DoubleProperty startX;
    private final DoubleProperty noteWidth;
    private final DoubleProperty currentY;
    private final BooleanProperty editMode;
    private final StringProperty lyric;
    private final StringProperty alias;

    private LyricCallback trackNote;
    private BooleanProperty showLyrics;
    private BooleanProperty showAliases;

    public Lyric(String defaultLyric, Scaler scaler) {
        this.scaler = scaler;
        startX = new SimpleDoubleProperty(0);
        noteWidth = new SimpleDoubleProperty(Quantizer.TEXT_FIELD_WIDTH);
        currentY = new SimpleDoubleProperty(0);
        editMode = new SimpleBooleanProperty(false);
        lyric = new SimpleStringProperty(defaultLyric);
        alias = new SimpleStringProperty("");

        drawnHBoxes = new HashMap<>();
        drawnLyrics = new HashMap<>();
        drawnAliases = new HashMap<>();
        drawnColumns = new HashSet<>();
    }

    /** Connect this lyric to a track note. */
    public void initialize(
            LyricCallback callback,
            DoubleProperty startX,
            DoubleProperty noteWidth,
            DoubleProperty currentY,
            BooleanProperty showLyrics,
            BooleanProperty showAliases) {
        trackNote = callback;
        this.startX.bind(startX);
        this.noteWidth.bind(noteWidth);
        this.currentY.bind(currentY);
        this.showLyrics = showLyrics;
        this.showAliases = showAliases;
    }

    public void setVisibleLyric(String newLyric) {
        if (!newLyric.equals(lyric.get())) {
            lyric.set(newLyric);
            adjustLyricAndAlias();
        }
    }

    @Override
    public TrackItemType getType() {
        return TrackItemType.LYRIC;
    }

    @Override
    public double getStartX() {
        return startX.get();
    }

    @Override
    public double getWidth() {
        return Math.max(noteWidth.get(), Quantizer.TEXT_FIELD_WIDTH * 2);
    }

    @Override
    public HBox redraw() {
        return redraw(0);
    }

    @Override
    public HBox redraw(double offsetX) {
        Label lyricText = new Label();
        lyricText.textProperty().bind(lyric);
        lyricText.getStyleClass().add("track-note-text");
        lyricText.setMaxWidth(Math.max(noteWidth.get() / 2, Quantizer.TEXT_FIELD_WIDTH));
        drawnLyrics.put(offsetX, lyricText);

        Label aliasText = new Label();
        aliasText.textProperty().bind(alias);
        aliasText.getStyleClass().add("track-note-text");
        aliasText.visibleProperty().bind(showAliases);
        aliasText.setMaxWidth(Math.max(noteWidth.get() / 2, Quantizer.TEXT_FIELD_WIDTH));
        drawnAliases.put(offsetX, aliasText);

        HBox lyricAndAlias = new HBox(lyricText, aliasText);
        lyricAndAlias.setMouseTransparent(true);
        lyricAndAlias.visibleProperty().bind(showLyrics);
        lyricAndAlias.translateXProperty().bind(startX.subtract(offsetX));
        lyricAndAlias.translateYProperty().bind(currentY);
        drawnHBoxes.put(offsetX, lyricAndAlias);

        Rectangle clip = new Rectangle(scaler.scaleX(Quantizer.TRACK_COL_WIDTH) + 1,
                scaler.scaleY(Quantizer.ROW_HEIGHT));
        clip.xProperty().bind(startX.subtract(offsetX).negate());
        lyricAndAlias.setClip(clip);
        return lyricAndAlias;
    }

    @Override
    public ImmutableSet<Integer> getColumns() {
        return ImmutableSet.copyOf(drawnColumns);
    }

    @Override
    public void addColumn(int colNum) {
        drawnColumns.add(colNum);
    }

    @Override
    public void removeColumn(int colNum) {
        drawnColumns.remove(colNum);
        double offsetX = colNum * scaler.scaleX(Quantizer.TRACK_COL_WIDTH);
        drawnHBoxes.remove(offsetX);
        drawnLyrics.remove(offsetX);
        drawnAliases.remove(offsetX);
    }

    @Override
    public void removeAllColumns() {
        drawnColumns.clear();
        drawnHBoxes.clear();
        drawnLyrics.clear();
        drawnAliases.clear();
    }

    public String getLyric() {
        return lyric.get();
    }

    public void setVisibleAlias(String newAlias) {
        if (!newAlias.equals(alias.get())) {
            if (newAlias.length() > 0) {
                alias.set(" (" + newAlias + ")");
            } else {
                alias.set(newAlias);
            }
            adjustLyricAndAlias();
        }
    }

    public void openTextField() {
        closeTextFieldIfNeeded();
        AnchorPane lyricPane = trackNote.getLyricPane();
        TextField textField = new TextField();
        textField.setMaxWidth(Quantizer.TEXT_FIELD_WIDTH);
        textField.setMaxHeight(Quantizer.TEXT_FIELD_HEIGHT);
        textField.setText(lyric.get());
        textField.selectAll();
        textField.setOnAction(action -> closeTextFieldIfNeeded());
        textField.focusedProperty().addListener(event -> {
            if (!textField.isFocused()) {
                closeTextFieldIfNeeded();
            }
        });
        for (double offsetX : drawnHBoxes.keySet()) {
            HBox activeNode = drawnHBoxes.get(offsetX);
            Bounds lyricScreenPosition = activeNode.localToScreen(activeNode.getBoundsInLocal());
            // Find bounds
            Bounds paneScreenPosition = lyricPane.localToScreen(lyricPane.getBoundsInLocal());
            textField.setTranslateX(Math.max(
                    0,
                    Math.min(
                        lyricPane.getWidth() - Quantizer.TEXT_FIELD_WIDTH,
                        lyricScreenPosition.getMinX() - paneScreenPosition.getMinX())));
            textField.setTranslateY(Math.max(
                    0,
                    Math.min(
                            lyricPane.getHeight() - Quantizer.TEXT_FIELD_HEIGHT,
                            lyricScreenPosition.getMinY() - paneScreenPosition.getMinY())));
            lyricPane.getChildren().add(textField);
            editMode.set(true);
            PauseTransition briefPause = new PauseTransition(Duration.millis(50));
            briefPause.setOnFinished(event -> textField.requestFocus());
            briefPause.play();
            // textField.requestFocus();
            return;
        }
    }

    public boolean isTextFieldOpen() {
        return editMode.get();
    }

    public void closeTextFieldIfNeeded() {
        if (!isTextFieldOpen()) {
            return;
        }
        editMode.set(false);
        String oldLyric = lyric.get();
        String newLyric = oldLyric;
        AnchorPane lyricPane = trackNote.getLyricPane();
        for (Node child : lyricPane.getChildren()) {
            if (child instanceof TextField) {
                newLyric = ((TextField) child).getText();
            }
        }
        lyricPane.getChildren().removeIf(child -> child instanceof TextField);
        if (!oldLyric.equals(newLyric)) {
            setVisibleLyric(newLyric);
            trackNote.replaceSongLyric(oldLyric, newLyric);
        }
    }

    public void registerLyric() {
        trackNote.setSongLyric(lyric.get());
    }

    private void adjustLyricAndAlias() {
        if (editMode.get()) {
            return;
        }
        for (double offsetX : drawnHBoxes.keySet()) {
            HBox hBox = drawnHBoxes.get(offsetX);
            hBox.getChildren().clear();
            if (!lyric.get().isEmpty()) {
                hBox.getChildren().add(drawnLyrics.get(offsetX));
            }
            if (!alias.get().isEmpty()) {
                hBox.getChildren().add(drawnAliases.get(offsetX));
            }
        }
    }
}
