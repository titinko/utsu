package com.utsusynth.utsu.files;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Iterator;
import java.util.Set;

import com.utsusynth.utsu.common.data.LyricConfigData;
import com.utsusynth.utsu.common.data.LyricConfigData.FrqStatus;
import com.utsusynth.utsu.common.data.PitchMapData;
import com.utsusynth.utsu.engine.EngineHelper;
import com.utsusynth.utsu.engine.ExternalProcessRunner;
import com.utsusynth.utsu.model.voicebank.Voicebank;

import org.junit.Test;

public class VoicebankReaderTest {

    @Test
    public void testVoiceBank() {
        testVoiceBank(EngineHelper.DEFAULT_VOICE_PATH);
    }

    @Test
    public void testExtraVoiceBanks() {
        File voiceDir = new File("src/test/resources/voice");
        if (!voiceDir.exists()) return;

        testVoiceBankDir(voiceDir);
    }

    private void testVoiceBankDir(File path) {

        // Recurse through child directories
        String[] directories = path.list(new FilenameFilter() {
            @Override
            public boolean accept(File current, String name) {
                return new File(current, name).isDirectory();
            }
        });

        if (directories != null & directories.length > 0) {
            // Run this for each child directory
            for (String dir : directories) {
                testVoiceBankDir(new File(path.getPath() + "/" + dir));
            }
        }

        // Finally, see if this is a voice bank
        File otoFile = new File(path + "/oto.ini");
        if (otoFile.exists()) {
            testVoiceBank(path.toString());
        }
    }

    private void testVoiceBank(String voicePath) {

        ExternalProcessRunner runner = new ExternalProcessRunner();

        // Create a reader
        VoicebankReader reader = EngineHelper.createVoicebankReader(runner, voicePath);
        assertTrue("VoicebankReader is null", reader != null);

        // Load a voice bank
        Voicebank bank = reader.loadVoicebankFromDirectory(new File(voicePath));
        assertTrue("Voicebank is null", bank != null);
        System.out.println("Testing voice bank: " + bank.getDescription());

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
        assertTrue("Too few categories defined", categories.size() > 0);

        int lyricDataCount = 0;
        String validFrqStatus = FrqStatus.VALID.toString();
        boolean shouldTestFrq = !EngineHelper.DEFAULT_VOICE_PATH.equalsIgnoreCase(voicePath);

        for (String category : categories) {

            Iterator<LyricConfigData> lyricDataIterator = bank.getLyricData(category);

            if (shouldTestFrq) {
                // Beware - this is destructive!
                bank.generateFrqs(lyricDataIterator);
                lyricDataIterator = bank.getLyricData(category);
            }

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