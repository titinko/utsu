package com.utsusynth.utsu.view.voicebank;

import java.util.Iterator;
import com.google.inject.Inject;
import com.utsusynth.utsu.common.data.LyricConfigData;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;

public class VoicebankEditor {
    private final LyricConfigFactory lyricFactory;

    private GridPane lyrics;
    private VoicebankCallback model;

    @Inject
    public VoicebankEditor(LyricConfigFactory lyricFactory) {
        this.lyricFactory = lyricFactory;
    }

    /** Initialize editor with data from the controller. */
    public void initialize(VoicebankCallback callback) {
        this.model = callback;
    }

    public GridPane createNew(Iterator<LyricConfigData> configs) {
        clear();
        if (!configs.hasNext()) {
            return lyrics;
        }
        // Add headers.
        int rowNum = 0;
        lyrics.add(lyricFactory.createCell("Lyric"), 0, rowNum);
        lyrics.add(lyricFactory.createCell("File"), 1, rowNum);
        lyrics.add(lyricFactory.createCell("Offset"), 2, rowNum);
        lyrics.add(lyricFactory.createCell("Consonant"), 3, rowNum);
        lyrics.add(lyricFactory.createCell("Cutoff"), 4, rowNum);
        lyrics.add(lyricFactory.createCell("Preutter"), 5, rowNum);
        lyrics.add(lyricFactory.createCell("Overlap"), 6, rowNum);

        // Populate lyrics
        rowNum++;
        while (configs.hasNext()) {
            LyricConfigData config = configs.next();
            StackPane[] configCells = lyricFactory.createLyricConfig(config).getCells();
            for (int colNum = 0; colNum < configCells.length; colNum++) {
                lyrics.add(configCells[colNum], colNum, rowNum);
            }
            rowNum++;
        }
        return lyrics;
    }

    private void clear() {
        // Remove current lyric configs.
        lyrics = new GridPane();
    }
}
