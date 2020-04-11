package com.utsusynth.utsu.engine;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.*;

import com.utsusynth.utsu.common.RegionBounds;
import com.utsusynth.utsu.common.data.NoteData;
import com.utsusynth.utsu.files.VoicebankFileManager;
import com.utsusynth.utsu.files.VoicebankReader;
import com.utsusynth.utsu.model.song.NoteList;
import com.utsusynth.utsu.model.song.NoteStandardizer;
import com.utsusynth.utsu.model.song.Song;
import com.utsusynth.utsu.model.song.pitch.PitchCurve;
import com.utsusynth.utsu.model.song.pitch.portamento.PortamentoFactory;
import com.utsusynth.utsu.model.voicebank.LyricConfig;
import com.utsusynth.utsu.model.voicebank.Voicebank;

import org.junit.Test;

/**
 * Ensures the Engine works for a trivial song. The rendered output file must be
 * a sensible size. A subsequent render with an edited song should be of
 * comparable size
 */
public class EngineTest {

    @Test
    public void testEngineCreation() {
        testEngineCreation(new File(EngineHelper.DEFAULT_VOICE_PATH));
    }

    @Test
    public void testExtraEngineCreation() {
        File voiceDir = new File("src/test/resources/voice");
        if (!voiceDir.exists()) return;

        ArrayList<File> voicebankDirs = new VoicebankFileManager().getVoiceBankDirs(voiceDir);
        voicebankDirs.forEach(d -> testEngineCreation(d));
    }

    private void testEngineCreation(File voicePath) {

        ExternalProcessRunner runner = new ExternalProcessRunner();
        Song song = createSong(runner, voicePath);

        VoicebankReader reader = EngineHelper.createVoicebankReader(runner, voicePath);
        Voicebank bank = reader.loadVoicebankFromDirectory(voicePath);

        Iterator<LyricConfig> lyricIterator = bank.getLyricConfigs("Main");
        ArrayList<LyricConfig> lyricConfig = new ArrayList<>();

        while (lyricIterator.hasNext()) lyricConfig.add(lyricIterator.next());

        // Trivial song data
        List<NoteData> notes = new ArrayList<>();
        notes.add(new NoteData(0, 600, "A4", lyricConfig.get(0).getTrueLyric()));
        notes.add(new NoteData(600, 1200, "G4", lyricConfig.get(1).getTrueLyric()));
        notes.add(new NoteData(1200, 1800, "F#4", lyricConfig.get(2).getTrueLyric()));

        song.addNotes(notes);
        
        // Initial render
        Engine engine = createEngine(runner);
        File output = createOutputFile(1);

        engine.renderWav(song, output);

        long originalLength = output.length();
        assertTrue("Rendered wav file is too short", originalLength > 500 * 1024);

        // Does it get confused if we edit the song and render again?
        ArrayList<NoteData> moreNotes = new ArrayList<>();
        moreNotes.add(new NoteData(1800, 2400, "E4", lyricConfig.get(3).getTrueLyric()));
        song.addNotes(moreNotes);

        // Must do this, otherwise it won't render!
        song.setRendered(RegionBounds.INVALID);
        
        File output2 = createOutputFile(2); // Won't allow us to overwrite the original file
        engine.renderWav(song, output2);
        long newLength = output2.length();

        // Has the engine produced sensible output?
        assertTrue("Edited song has not changed the wav file", newLength > originalLength);
        assertTrue("Edited song wav file is too long", newLength < 1.5 * originalLength);
    }

    private Engine createEngine(ExternalProcessRunner runner) {

        String os = System.getProperty("os.name").toLowerCase();
        String resamplerPath;
        String wavtoolPath;

        if (os.contains("win")) {
            resamplerPath = "assets/win64/macres.exe";
            wavtoolPath = "assets/win64/wavtool-yawu.exe";
        } else if (os.contains("mac")) {
            resamplerPath = "assets/Mac/macres";
            wavtoolPath = "assets/Mac/wavtool-yawu";
        } else {
            resamplerPath = "assets/linux64/macres";
            wavtoolPath = "assets/linux64/wavtool-yawu";
        }

        Resampler resampler = new Resampler(runner);
        Wavtool wavtool = new Wavtool(runner);
        File resamplerFile = new File(resamplerPath);
        File wavtoolFile = new File(wavtoolPath);

        return new Engine(
                resampler,
                wavtool,
                /* statusBar= */ null,
                /* threadPoolSize= */ 10,
                resamplerFile,
                wavtoolFile,
                runner);
    }

    private Song createSong(ExternalProcessRunner runner, File voicePath) {
        return new Song(EngineHelper.createVoicebankContainer(runner, voicePath),
                new NoteStandardizer(),
                new NoteList(),
                new PitchCurve(new PortamentoFactory()));
    }

    private File createOutputFile(int index) {

        File output = new File("src/test/resources/engine-test-" + index + ".wav");
        File dir = output.getParentFile();
        output.deleteOnExit();
        
        if (!dir.exists()) {
            // Make sure this folder exists first!
            dir.mkdirs();
        }

        return output;
    }
}
