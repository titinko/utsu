package com.utsusynth.utsu.engine;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.inject.Provider;
import com.utsusynth.utsu.common.RegionBounds;
import com.utsusynth.utsu.common.data.NoteData;
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

/**
 * Ensures the Engine works for a trivial song. The rendered output file must be
 * a sensible size. A subsequent render with an edited song should be of
 * comparable size
 */
public class EngineTest {

    @Test
    public void testEngineCreation() {

        ExternalProcessRunner runner = new ExternalProcessRunner();
        Song song = createSong(runner);

        // Trivial song data
        List<NoteData> notes = new ArrayList<>();
        notes.add(new NoteData(0, 600, "A4", "わ"));
        notes.add(new NoteData(600, 1200, "G4", "た"));
        notes.add(new NoteData(1200, 1800, "F#4", "し"));

        song.addNotes(notes);
        
        // Initial render
        Engine engine = createEngine(runner);
        File output = createOutputFile(1);

        engine.renderWav(song, output);

        long originalLength = output.length();
        assertTrue(originalLength > 500 * 1024);

        // Does it get confused if we edit the song and render again?
        ArrayList<NoteData> moreNotes = new ArrayList<>();
        moreNotes.add(new NoteData(1800, 2400, "E4", "も"));
        song.addNotes(moreNotes);

        // Must do this, otherwise it won't render!
        song.setRendered(RegionBounds.INVALID);
        
        File output2 = createOutputFile(2); // Won't allow us to overwrite the original file
        engine.renderWav(song, output2);
        long newLength = output2.length();

        // Has the engine produced sensible output?
        assertTrue(newLength > originalLength);
        assertTrue(newLength < 1.5 * originalLength);
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
                wavtoolFile);
    }

    private FrqGenerator createFrqGenerator(ExternalProcessRunner runner) {

        String os = System.getProperty("os.name").toLowerCase();
        String frqGeneratorPath;

        if (os.contains("win")) {
            frqGeneratorPath = "assets/win64/frq0003gen.exe";
        } else if (os.contains("mac")) {
            frqGeneratorPath = "assets/Mac/frq0003gen";
        } else {
            frqGeneratorPath = "assets/linux64/frq0003gen";
        }

        return new FrqGenerator(runner, new File(frqGeneratorPath), 256);
    }

    private VoicebankContainer createVoicebankContainer(ExternalProcessRunner runner) {

        LyricConfigMap lyricConfigs = new LyricConfigMap();
        PitchMap pitchMap = new PitchMap();
        DisjointLyricSet conversionSet = new DisjointLyricSet();
        Set<File> soundFiles = new HashSet<>();
        FrqGenerator frqGenerator = createFrqGenerator(runner);

        Provider<Voicebank> voicebankProvider = () -> new Voicebank(lyricConfigs, pitchMap, conversionSet, soundFiles, frqGenerator);

        File defaultVoicePath = new File("assets/voice/Iona_Beta");
        File lyricConversionPath = new File("assets/config/lyric_conversions.txt");

        VoicebankReader voicebankReader = new VoicebankReader(defaultVoicePath, lyricConversionPath, voicebankProvider);
        VoicebankManager voicebankManager = new VoicebankManager();

        return new VoicebankContainer(voicebankManager, voicebankReader);
    }

    private Song createSong(ExternalProcessRunner runner) {
        return new Song(createVoicebankContainer(runner),
                new NoteStandardizer(),
                new NoteList(),
                new PitchCurve(new PortamentoFactory()));
    }

    private File createOutputFile(int index) {

        File output = new File("src/test/output/engine-test-" + index + ".wav");
        File dir = output.getParentFile();
        output.deleteOnExit();
        
        if (!dir.exists()) {
            // Make sure this folder exists first!
            dir.mkdirs();
        }

        return output;
    }
}
