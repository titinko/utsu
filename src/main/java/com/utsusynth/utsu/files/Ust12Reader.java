package com.utsusynth.utsu.files;

import java.util.regex.Pattern;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.utsusynth.utsu.model.song.Song;
import com.utsusynth.utsu.model.song.Note;
import com.utsusynth.utsu.model.voicebank.VoicebankReader;

/**
 * Reads a song from a Unicode UST 1.2 file.
 */
public class Ust12Reader {
    private static final Pattern HEADER_PATTERN = Pattern.compile("\\[#[A-Z0-9]+\\]");
    private static final Pattern NOTE_PATTERN = Pattern.compile("\\[#[0-9]{4,}\\]");
    private final Provider<Song> songProvider;
    private final VoicebankReader voicebankReader;

    @Inject
    public Ust12Reader(Provider<Song> songProvider, VoicebankReader voicebankReader) {
        this.songProvider = songProvider;
        this.voicebankReader = voicebankReader;
    }

    public Song loadSong(String fileContents) {
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
        for (int i = noteStart; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.startsWith("Length=")) {
                note.setDuration(Integer.parseInt(line.substring("Length=".length())));
            } else if (line.startsWith("Lyric=")) {
                note.setLyric(line.substring("Lyric=".length()));
            } else if (line.startsWith("NoteNum=")) {
                note.setNoteNum(Integer.parseInt(line.substring("NoteNum=".length())));
            } else if (line.startsWith("Velocity=")) {
                note.setVelocity(Double.parseDouble(line.substring("Velocity=".length())));
            } else if (line.startsWith("StartPoint=")) {
                note.setStartPoint(Double.parseDouble(line.substring("StartPoint=".length())));
            } else if (line.startsWith("Intensity=")) {
                note.setIntensity(Integer.parseInt(line.substring("Intensity=".length())));
            } else if (line.startsWith("Modulation=")) {
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
            } else if (HEADER_PATTERN.matcher(line).matches()) {
                if (note.getLyric().equals("R")) {
                    builder.addRestNote(note);
                } else {
                    builder.addNote(note);
                }
                return i;
            }
        }
        return -1;
    }

    private int parseVersion(String[] lines, int versionStart) {
        for (int i = versionStart; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.startsWith("UST Version")) {
                String version = line.substring("UST Version".length());
                if (!version.equals("2.0")) {
                    // throw error
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
                builder.setTempo(Double.parseDouble(line.substring(6)));
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
