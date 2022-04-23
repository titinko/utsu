package com.utsusynth.utsu.files.song;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.utsusynth.utsu.common.utils.UtsuFileUtils;
import com.utsusynth.utsu.files.voicebank.VoicebankReader;
import com.utsusynth.utsu.model.song.Note;
import com.utsusynth.utsu.model.song.Song;
import org.apache.commons.io.FileUtils;

/**
 * Reads a song from a Unicode UST 2.0 file.
 */
public class Ust20Reader implements SongReader {
    private static final Pattern HEADER_PATTERN = Pattern.compile("\\[#[A-Z0-9]+\\]");
    private static final Pattern NOTE_PATTERN = Pattern.compile("\\[#[0-9]{1,4}\\]");
    private final Provider<Song> songProvider;
    private final VoicebankReader voicebankReader;

    @Inject
    public Ust20Reader(Provider<Song> songProvider, VoicebankReader voicebankReader) {
        this.songProvider = songProvider;
        this.voicebankReader = voicebankReader;
    }

    @Override
    public String getSaveFormat(File file) {
        try {
            return UtsuFileUtils.guessCharset(FileUtils.readFileToByteArray(file)).equals("UTF-8")
                    ? "UST 2.0 (UTF-8)" : "UST 2.0 (Shift JIS)";
        } catch (IOException e) {
            return "UST 2.0 (UTF-8)";
        }
    }

    @Override
    public int getNumTracks(File file) {
        return 1;
    }

    @Override
    public Song loadSong(File file, int trackNum) {
        String fileContents = UtsuFileUtils.readConfigFile(file);
        String[] lines = fileContents.split("\n");
        Song.Builder songBuilder = songProvider.get().toBuilder();
        int curLine = 0;
        while (curLine >= 0 && curLine < lines.length) {
            curLine = parseSection(lines, curLine, songBuilder);
        }
        return songBuilder.build();
    }

    private int parseSection(String[] lines, int sectionStart, Song.Builder builder) {
        String header = lines[sectionStart].trim();
        if (!HEADER_PATTERN.matcher(header).matches()) {
            // Report parse section not called on section header warning.
            System.out.println("Parse header not called on section header.");
            return -1;
        }
        // Case for notes.
        if (NOTE_PATTERN.matcher(header).matches()) {
            return parseNote(lines, sectionStart + 1, builder);
        }
        switch (header) {
            case "[#VERSION]":
                return parseVersion(lines, sectionStart + 1);
            case "[#SETTING]":
                return parseSetting(lines, sectionStart + 1, builder);
            case "[#TRACKEND]":
                System.out.println("Finished parsing the track!");
                return -1;
            default:
                System.out.println("Unexpected header discovered.");
                // Report unexpected header discovered warning.
                return -1;
        }
    }

    private int parseNote(String[] lines, int noteStart, Song.Builder builder) {
        Note note = new Note();
        boolean outsideMainTrack = false;
        for (int i = noteStart; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.startsWith("Delta=") && !line.equals("Delta=")) {
                note.setDelta(Integer.parseInt(line.substring("Delta=".length())));
            } else if (line.startsWith("Duration=") && !line.equals("Duration=")) {
                note.setDuration(Integer.parseInt(line.substring("Duration=".length())));
            } else if (line.startsWith("Length=") && !line.equals("Length=")) {
                note.setLength(Integer.parseInt(line.substring("Length=".length())));
            } else if (line.startsWith("Lyric=")) {
                note.setLyric(line.substring("Lyric=".length()));
            } else if (line.startsWith("NoteNum=") && !line.equals("NoteNum=")) {
                note.setNoteNum(Integer.parseInt(line.substring("NoteNum=".length())));
            } else if (line.startsWith("PreUtterance=") && !line.equals("PreUtterance=")) {
                note.setPreutter(Double.parseDouble(line.substring("PreUtterance=".length())));
            } else if (line.startsWith("VoiceOverlap=") && !line.equals("VoiceOverlap=")) {
                note.setOverlap(Double.parseDouble(line.substring("VoiceOverlap=".length())));
            } else if (line.startsWith("Velocity=") && !line.equals("Velocity=")) {
                note.setVelocity(Double.parseDouble(line.substring("Velocity=".length())));
            } else if (line.startsWith("StartPoint=") && !line.equals("StartPoint=")) {
                note.setStartPoint(Double.parseDouble(line.substring("StartPoint=".length())));
            } else if (line.startsWith("Intensity=") && !line.equals("Intensity=")) {
                note.setIntensity(Integer.parseInt(line.substring("Intensity=".length())));
            } else if (line.startsWith("Modulation=") && !line.equals("Modulation=")) {
                note.setModulation(Integer.parseInt(line.substring("Modulation=".length())));
            } else if (line.startsWith("Flags=")) {
                note.setNoteFlags(line.substring("Flags=".length()));
            } else if (line.startsWith("PBS=")) {
                note.setPBS(line.substring("PBS=".length()).split(","));
            } else if (line.startsWith("PBW=")) {
                note.setPBW(line.substring("PBW=".length()).split(","));
            } else if (line.startsWith("PBY=")) {
                note.setPBY(line.substring("PBY=".length()).split(","));
            } else if (line.startsWith("PBM=")) {
                note.setPBM(line.substring("PBM=".length()).split(","));
            } else if (line.startsWith("Envelope=")) {
                note.setEnvelope(line.substring("Envelope=".length()).split(","));
            } else if (line.startsWith("VBR=")) {
                note.setVibrato(line.substring("VBR=".length()).split(","));
            } else if (line.startsWith("layer=")) {
                outsideMainTrack = true;
            } else if (HEADER_PATTERN.matcher(line).matches()) {
                if (outsideMainTrack) {
                    // TODO: Make these notes valid once multi-track is supported.
                    builder.addInvalidNote(note);
                } else {
                    builder.addNote(note);
                }
                return i;
            }
        }
        return -1;
    }

    private int parseVersion(String[] lines, int versionStart) {
        // Ignore charset, we don't really care about it.
        for (int i = versionStart; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.startsWith("UST Version")) {
                String version = line.substring("UST Version".length());
                if (!version.equals("2.0")) {
                    // throw error?
                }
            } else if (HEADER_PATTERN.matcher(line).matches()) {
                return i;
            }
        }
        return -1;
    }

    private int parseSetting(String[] lines, int settingStart, Song.Builder builder) {
        for (int i = settingStart; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.startsWith("Tempo=")) {
                builder.setTempo(Double.parseDouble(line.substring("Tempo=".length())));
            } else if (line.startsWith("ProjectName=")) {
                builder.setProjectName(line.substring("ProjectName=".length()));
            } else if (line.startsWith("OutFile=")) {
                builder.setOutputFile(voicebankReader.parseFilePath(line, "OutFile="));
            } else if (line.startsWith("VoiceDir=")) {
                builder.setVoiceDirectory(voicebankReader.parseFilePath(line, "VoiceDir="));
            } else if (line.startsWith("Flags=")) {
                builder.setFlags(line.substring("Flags=".length()));
            } else if (line.startsWith("Mode2=")) {
                builder.setMode2(Boolean.parseBoolean(line.substring("Mode2=".length())));
            } else if (HEADER_PATTERN.matcher(line).matches()) {
                return i;
            }
        }
        return -1;
    }
}
