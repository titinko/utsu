package com.utsusynth.utsu.files;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.google.inject.Provider;
import com.utsusynth.utsu.common.RegionBounds;
import com.utsusynth.utsu.common.data.LyricConfigData;
import com.utsusynth.utsu.common.data.LyricConfigData.FrqStatus;
import com.utsusynth.utsu.common.data.NoteData;
import com.utsusynth.utsu.common.data.PitchMapData;
import com.utsusynth.utsu.engine.EngineHelper;
import com.utsusynth.utsu.engine.ExternalProcessRunner;
import com.utsusynth.utsu.files.VoicebankReader;
import com.utsusynth.utsu.model.song.NoteList;
import com.utsusynth.utsu.model.song.NoteStandardizer;
import com.utsusynth.utsu.model.song.Song;
import com.utsusynth.utsu.model.song.pitch.PitchCurve;
import com.utsusynth.utsu.model.song.pitch.portamento.PortamentoFactory;
import com.utsusynth.utsu.model.voicebank.DisjointLyricSet;
import com.utsusynth.utsu.model.voicebank.LyricConfigMap;
import com.utsusynth.utsu.model.voicebank.PitchMap;
import com.utsusynth.utsu.model.voicebank.Voicebank;
import com.utsusynth.utsu.model.voicebank.VoicebankContainer;
import com.utsusynth.utsu.model.voicebank.VoicebankManager;

import org.junit.Test;

public class VoicebankReaderTest {

    @Test
    public void TestVoiceBank() {

        ExternalProcessRunner runner = new ExternalProcessRunner();

        // Create a reader
        VoicebankReader reader = EngineHelper.createVoicebankReader(runner, EngineHelper.DEFAULT_VOICE_PATH);
        assertTrue("VoicebankReader is null", reader != null);

        // Load a voice bank
        Voicebank bank = reader.loadVoicebankFromDirectory(new File(EngineHelper.DEFAULT_VOICE_PATH));
        assertTrue("Voicebank is null", bank != null);

        // Check pitch maps
        Iterator<PitchMapData> pitchDataIterator = bank.getPitchData();
        int pitchCount = 0;

        while (pitchDataIterator.hasNext()) {
            PitchMapData pitchData = pitchDataIterator.next();
            assertNotNull("Null pitch defined", pitchData.getPitch());
            assertTrue("Invalid pitch: " + pitchData.getPitch(), pitchData.getPitch().length() > 1);
            pitchCount++;
        }

        // Must have a sensible number of pitches
        assertTrue("Too few pitches defined in Voicebank", pitchCount > 10);

        // Check config data state
        Set<String> categories = bank.getCategories();
        int lyricDataCount = 0;
        String validFrqStatus = FrqStatus.VALID.toString();

        for (String category : categories) {

            Iterator<LyricConfigData> lyricDataIterator = bank.getLyricData(category);

            // Beware - this is destructive!
            bank.generateFrqs(lyricDataIterator);

            lyricDataIterator = bank.getLyricData(category);

            while (lyricDataIterator.hasNext()) {
                lyricDataCount++;
                LyricConfigData lyricData = lyricDataIterator.next();
                String dataStatus = lyricData.frqStatusProperty().get();
                assertTrue("LyricConfigData status is " + dataStatus, validFrqStatus.equals(dataStatus));
            }
        }

        assertTrue("Too few LyricConfigData values", lyricDataCount > 10);
    }
}