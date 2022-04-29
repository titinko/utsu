package com.utsusynth.utsu.files.song;

import com.google.inject.Inject;
import com.utsusynth.utsu.common.utils.UtsuFileUtils;

import java.io.File;

/** Singleton class, finds the appropriate SongReader for a file. */
public class SongReaderManager {
    private final Ust12Reader ust12Reader;
    private final Ust20Reader ust20Reader;
    private final UstxReader ustxReader;
    private final MidiReader midiReader;

    @Inject
    public SongReaderManager(
            Ust12Reader ust12Reader,
            Ust20Reader ust20Reader,
            UstxReader ustxReader,
            MidiReader midiReader) {
        this.ust12Reader = ust12Reader;
        this.ust20Reader = ust20Reader;
        this.ustxReader = ustxReader;
        this.midiReader = midiReader;
    }

    public SongReader getSongReader(File file) {
        if (file.getName().endsWith(".ustx")) {
            return ustxReader;
        } else if (file.getName().endsWith(".mid")) {
            return midiReader;
        } else {
            String content = UtsuFileUtils.readConfigFile(file);
            if (content.contains("UST Version1.2")) {
                return ust12Reader;
            } else if (content.contains("UST Version2.0")) {
                return ust20Reader;
            }
        }
        // If no version found, assume UST 1.2 for now.
        return ust12Reader;
    }
}
