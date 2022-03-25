package com.utsusynth.utsu.files.song;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.utsusynth.utsu.files.voicebank.VoicebankReader;
import com.utsusynth.utsu.model.song.Note;
import com.utsusynth.utsu.model.song.Song;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.*;

/**
 * Reads a song from a ustx file.
 */
public class UstxReader implements SongReader {
    private final Provider<Song> songProvider;
    private final VoicebankReader voicebankReader;

    @Inject
    public UstxReader(Provider<Song> songProvider, VoicebankReader voicebankReader) {
        this.songProvider = songProvider;
        this.voicebankReader = voicebankReader;
    }

    @Override
    public int getNumTracks(String fileContents) {
        Yaml yaml = new Yaml();
        Node yamlNode = yaml.represent(yaml.load(fileContents));
        for (NodeTuple yamlEntry : getMapEntries(yamlNode)) {
            if (getStringValue(yamlEntry.getKeyNode()).equals("voice_parts")) {
                return Math.max(1, getListEntries(yamlEntry.getValueNode()).size());
            }
        }
        return 1;
    }

    @Override
    public Song loadSong(String fileContents, int trackNum) {
        Yaml yaml = new Yaml();
        Node yamlNode = yaml.represent(yaml.load(fileContents));
        Song.Builder songBuilder = songProvider.get().toBuilder();
        for (NodeTuple yamlEntry : getMapEntries(yamlNode)) {
            parseSection(yamlEntry, songBuilder, trackNum);
        }
        return songBuilder.build();
    }

    private void parseSection(NodeTuple section, Song.Builder builder, int trackNum) {
        switch (getStringValue(section.getKeyNode())) {
            case "name":
                builder.setProjectName(getStringValue(section.getValueNode()));
                break;
            case "comment":
                // Do nothing.
                break;
            case "output_dir":
                // Do nothing.
                break;
            case "cache_dir":
                // Do nothing.
                break;
            case "ustx_version":
                // Do nothing.
                break;
            case "bpm":
                builder.setTempo(getDoubleValue(section.getValueNode()));
                break;
            case "beat_per_bar":
                break;
            case "beat_unit":
                break;
            case "resolution":
                break;
            case "expressions":
                break;
            case "tracks":
                break;
            case "voice_parts":
                Node voiceTrack = getListEntries(section.getValueNode()).get(trackNum - 1);
                for (NodeTuple trackSection : getMapEntries(voiceTrack)) {
                    parseTrackSection(trackSection, builder);
                }
                break;
            case "wave_parts":
                // Do nothing for now.
                break;
            default:
                System.out.println("Unknown USTX field: " + getStringValue(section.getKeyNode()));
        }
    }

    private void parseTrackSection(NodeTuple section, Song.Builder builder) {
        switch (getStringValue(section.getKeyNode())) {
            case "name":
                builder.setProjectName(getStringValue(section.getValueNode()));
                break;
            case "comment":
                // Do nothing.
                break;
            case "track_no":
                // Do nothing.
                break;
            case "position":
                // Do nothing.
                break;
            case "notes":
                for (Node noteNode : getListEntries(section.getValueNode())) {
                    parseNote(noteNode, builder);
                }
                break;
            default:
                System.out.println("Unkown USTX field: " + getStringValue(section.getKeyNode()));
        }
    }

    private void parseNote(Node noteNode, Song.Builder builder) {
        Note note = new Note();
        for (NodeTuple noteSection : getMapEntries(noteNode)) {
            switch (getStringValue(noteSection.getKeyNode())) {
                case "position":
                    break;
                case "duration":
                    break;
                case "tone":
                    break;
                case "lyric":
                    break;
                case "pitch":
                    break;
                case "vibrato":
                    break;
                case "note_expressions":
                    break;
                case "phoneme_expressions":
                    break;
                case "phoneme_overrides":
                    break;
                default:
                    System.out.println(
                            "Unkown USTX field: " + getStringValue(noteSection.getKeyNode()));
            }
        }
        builder.addNote(note);
    }

    private void parsePitchSection(NodeTuple section, Note note) {
        switch (getStringValue(section.getKeyNode())) {
            case "data":
                // x, y, shape
                break;
            case "snap_first":
                break;
            default:
                System.out.println("Unkown USTX field: " + getStringValue(section.getKeyNode()));
        }
    }

    private void parseVibratoSection(NodeTuple section, Note note) {
        switch (getStringValue(section.getKeyNode())) {
            case "data":
                // x, y, shape
                break;
            case "snap_first":
                break;
            default:
                System.out.println("Unkown USTX field: " + getStringValue(section.getKeyNode()));
        }
    }

    private int parseNote(String[] lines, int noteStart, Song.Builder builder) {
        Note note = new Note();
        for (int i = noteStart; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.startsWith("Length=") && !line.equals("Length=")) {
                note.setDuration(Integer.parseInt(line.substring("Length=".length())));
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
                note.setPBS(line.substring("PBS=".length()).split("[,;]"));
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
            } else {
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
            } else {
                return i;
            }
        }
        return -1;
    }

    private static List<NodeTuple> getMapEntries(Node yamlNode) {
        if (!(yamlNode instanceof MappingNode)) {
            return ImmutableList.of();
        }
        return ((MappingNode) yamlNode).getValue();
    }

    private static List<Node> getListEntries(Node yamlNode) {
        if (!(yamlNode instanceof SequenceNode)) {
            return ImmutableList.of();
        }
        return ((SequenceNode) yamlNode).getValue();
    }

    private static String getStringValue(Node yamlNode) {
        if (!(yamlNode instanceof ScalarNode)) {
            return "";
        }
        return ((ScalarNode) yamlNode).getValue();
    }

    private static double getDoubleValue(Node yamlNode) {
        if (!(yamlNode instanceof ScalarNode)) {
            return 0;
        }
        return Double.parseDouble(((ScalarNode) yamlNode).getValue());
    }
}
