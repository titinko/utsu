package com.utsusynth.utsu.engine;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentHashMap;

import com.google.inject.Inject;
import com.utsusynth.utsu.common.utils.PitchUtils;
import com.utsusynth.utsu.files.FileNameMapper;
import com.utsusynth.utsu.model.song.Note;
import com.utsusynth.utsu.model.song.Song;
import com.utsusynth.utsu.model.voicebank.LyricConfig;

public class Resampler {
    private static final File SILENCE_PATH = new File("assets/silence.wav");

    private ConcurrentHashMap<String, String> cacheMap = new ConcurrentHashMap<>();
    private final ExternalProcessRunner runner;
    private String cacheDir;

    @Inject
    Resampler(ExternalProcessRunner runner) {
        this.runner = runner;
        this.cacheDir = "cache";
        new File(this.cacheDir).mkdirs();
    }

    File resample(
            File resamplerPath,
            Note note,
            double noteLength,
            LyricConfig config,
            File outputFile,
            String pitchString,
            Song song) {

        FileNameMapper fileUtils = FileNameMapper.getInstance();
        String inputFilePath = fileUtils.getOSName(config.getPathToFile().getAbsolutePath());
        String pitch = PitchUtils.noteNumToPitch(note.getNoteNum());
        String consonantVelocity = Double.toString(note.getVelocity() * (song.getTempo() / 125));
        String flags = note.getNoteFlags().isEmpty() ? song.getFlags() : note.getNoteFlags();
        String offset = Double.toString(config.getOffset());
        double startPoint = note.getStartPoint() + note.getAutoStartPoint();
        double scaledLength = noteLength * (125 / song.getTempo()) + startPoint + 1;
        double consonantLength = config.getConsonant(); // TODO: Cutoff?
        String cutoff = Double.toString(config.getCutoff());
        String intensity = Integer.toString(note.getIntensity());
        String modulation = Integer.toString(note.getModulation()); // TODO: Set this song-wide?
        String tempo = "T" + Double.toString(song.getTempo()); // TODO: Override with note tempo.

        String[] args = {
            resamplerPath.getAbsolutePath(),
            inputFilePath,
            "",
            pitch,
            consonantVelocity,
            flags.isEmpty() ? "?" : flags, // Uses placeholder value if there are no flags.
            offset,
            Double.toString(scaledLength),
            Double.toString(consonantLength),
            cutoff,
            intensity,
            modulation,
            tempo,
            pitchString
        };

        return resampleWithCache(args, outputFile);
    }

    File resampleSilence(File resamplerPath, File outputFile, double duration) {
        String desiredLength = Double.toString(duration + 1);

        String[] args = {
            resamplerPath.getAbsolutePath(),
            SILENCE_PATH.getAbsolutePath(),
            "",
            "C4",
            "100",
            "?",
            "0",
            desiredLength,
            "0",
            "0",
            "100",
            "0"
        };

        return resampleWithCache(args, outputFile);
    }


    private File resampleWithCache(String[] args, File outputFile) {

        File cacheFile;

        try {
            String cacheFileName = getCacheFileName(args);
            cacheFile = new File(cacheFileName);
            
            // Output the render to this file name
            args[2] = cacheFileName;

            if (cacheFile.exists()) {
                // This has already been cached
                return cacheFile;
            }

        } catch (Exception e) {
            cacheFile = outputFile;
        }

        runner.runProcess(args);

        return cacheFile;
    }

    private String getCacheFileName(String[] args) throws UnsupportedEncodingException, NoSuchAlgorithmException {

        String cacheString = String.join("::", args);
        String cacheFileName = null;

        if (cacheMap.contains(cacheString)) {
            // Avoid creating a new MD5, if possible
            cacheFileName = cacheMap.get(cacheString);
        }

        if (cacheFileName == null || cacheFileName.length() == 0) {

            // Create a hash of the values for good file names
            byte[] bytesOfMessage = cacheString.getBytes("UTF-8");
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] thedigest = md.digest(bytesOfMessage);

            StringBuilder sb = new StringBuilder();
            for (byte b : thedigest) {
                sb.append(String.format("%02x", b));
            }

            cacheFileName = cacheDir + "/note-" + sb.toString() + ".wav";

            if (!cacheMap.contains(cacheString)) {
                cacheMap.put(cacheString, cacheFileName);
            }
        }

        return cacheFileName;
    }
}