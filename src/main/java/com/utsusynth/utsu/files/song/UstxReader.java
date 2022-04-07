package com.utsusynth.utsu.files.song;

import java.util.*;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.utsusynth.utsu.common.utils.RoundUtils;
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
            if (getStringValue(yamlEntry.getKeyNode(), "").equals("voice_parts")) {
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
        Node value = section.getValueNode();
        switch (getStringValue(section.getKeyNode(), "")) {
            case "name":
                builder.setProjectName(getStringValue(value, ""));
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
                double tempo = getDoubleValue(value, -1);
                if (tempo > 0) {
                    builder.setTempo(tempo);
                }
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
                parseTrack(getListEntries(section.getValueNode()).get(trackNum - 1), builder);
                break;
            case "wave_parts":
                // Do nothing for now.
                break;
            default:
                printKeyErrorMessage(section);
        }
    }

    private void parseTrack(Node voiceTrack, Song.Builder builder) {
        for (NodeTuple section : getMapEntries(voiceTrack)) {
            Node value = section.getValueNode();
            switch (getStringValue(section.getKeyNode(), "")) {
                case "name":
                    builder.setProjectName(getStringValue(section.getValueNode(), ""));
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
                    TreeMap<Integer, Note> sortedNotes = new TreeMap<>();
                    for (Node noteNode : getListEntries(value)) {
                        parseNote(noteNode, sortedNotes);
                    }
                    addAllNotes(sortedNotes, builder);
                    break;
                default:
                    printKeyErrorMessage(section);
            }
        }
    }

    private void addAllNotes(TreeMap<Integer, Note> sortedNotes, Song.Builder builder) {
        int curPosition = 0;
        // TreeMap.entrySet() will return a set in sorted order.
        for (Map.Entry<Integer, Note> entry : sortedNotes.entrySet()) {
            int newPosition = entry.getKey();
            if (newPosition > curPosition) {
                // Fill any blank space with a rest note.
                Note restNote = new Note();
                restNote.setDuration(newPosition - curPosition);
                restNote.setLyric("R");
                restNote.setNoteNum(60);
                builder.addRestNote(restNote);
                curPosition = newPosition;
            }
            curPosition += entry.getValue().getDuration();
            builder.addNote(entry.getValue());
        }
    }

    private void parseNote(Node noteNode, TreeMap<Integer, Note> sortedNotes) {
        int position = -1;
        Note note = new Note();
        for (NodeTuple section : getMapEntries(noteNode)) {
            Node value = section.getValueNode();
            switch (getStringValue(section.getKeyNode(), "")) {
                case "position":
                    position = getIntValue(value, position);
                    break;
                case "duration":
                    note.setDuration(getIntValue(value, -1));
                    break;
                case "tone":
                    note.setNoteNum(getIntValue(value, -1));
                    break;
                case "lyric":
                    note.setLyric(getStringValue(value, ""));
                    break;
                case "pitch":
                    parsePitch(value, note);
                    break;
                case "vibrato":
                    parseVibrato(value, note);
                    break;
                case "note_expressions":
                    break;
                case "phoneme_expressions":
                    break;
                case "phoneme_overrides":
                    break;
                default:
                    printKeyErrorMessage(section);
            }
        }
        if (position > 0) {
            sortedNotes.put(position, note);
        }
    }

    private void parsePitch(Node pitchNode, Note note) {
        for (NodeTuple pitchSection : getMapEntries(pitchNode)) {
            Node value = pitchSection.getValueNode();
            switch (getStringValue(pitchSection.getKeyNode(), "")) {
                case "data":
                    List<Node> dataNodes = getListEntries(value);
                    double curX = 0;
                    double[] pbs = new double[2];
                    double[] pbw = new double[Math.max(dataNodes.size() - 1, 0)];
                    double[] pby = new double[Math.max(dataNodes.size() - 2, 0)];
                    String[] pbm = new String[Math.max(dataNodes.size() - 1, 0)];
                    Arrays.fill(pbm, "");
                    for (int i = 0; i < dataNodes.size(); i++) {
                        for (NodeTuple dataSection : getMapEntries(dataNodes.get(i))) {
                            Node dataValue = dataSection.getValueNode();
                            switch(getStringValue(dataSection.getKeyNode(), "")) {
                                case "x":
                                    double xValue = getDoubleValue(dataValue, 0);
                                    if (i == 0) {
                                        pbs[0] = xValue;
                                    } else {
                                        pbw[i - 1] = xValue - curX;
                                    }
                                    curX = xValue;
                                    break;
                                case "y":
                                    if (i > 0 && i < dataNodes.size() - 1) {
                                        pby[i - 1] = getDoubleValue(dataValue, 0);
                                    }
                                    break;
                                case "shape":
                                    if (i >= dataNodes.size() - 1) {
                                        break;
                                    }
                                    switch (getStringValue(dataValue, "io")) {
                                        case "l":
                                            pbm[i] = "s"; // Straight.
                                            break;
                                        case "i":
                                            pbm[i] = "j"; // Sine in.
                                            break;
                                        case "o":
                                            pbm[i] = "r"; // Sine out.
                                            break;
                                        case "io":
                                        default:
                                            pbm[i] = ""; // Sine in out.
                                    }
                                    break;
                                default:
                                    printKeyErrorMessage(dataSection);
                            }
                        }
                        note.setPBS(pbs);
                        note.setPBW(pbw);
                        note.setPBY(pby);
                        note.setPBM(pbm);
                    }
                    break;
                case "snap_first":
                    // Do nothing.
                    break;
                default:
                    printKeyErrorMessage(pitchSection);
            }
        }
    }

    private void parseVibrato(Node vibratoNode, Note note) {
        int[] vibrato = new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        for (NodeTuple vibratoSection : getMapEntries(vibratoNode)) {
            Node value = vibratoSection.getValueNode();
            switch (getStringValue(vibratoSection.getKeyNode(), "")) {
                case "length":
                    vibrato[0] = getIntValue(value, vibrato[0]);
                    break;
                case "period":
                    vibrato[1] = getIntValue(value, vibrato[1]);
                    break;
                case "depth":
                    vibrato[2] = getIntValue(value, vibrato[2]);
                    break;
                case "in":
                    vibrato[3] = getIntValue(value, vibrato[3]);
                    break;
                case "out":
                    vibrato[4] = getIntValue(value, vibrato[4]);
                    break;
                case "shift":
                    vibrato[5] = getIntValue(value, vibrato[5]);
                    break;
                case "drift":
                    vibrato[6] = getIntValue(value, vibrato[6]);
                    break;
                default:
                    printKeyErrorMessage(vibratoSection);
            }
        }
        note.setVibrato(vibrato);
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

    private static void printKeyErrorMessage(NodeTuple nodeTuple) {
        System.out.println(
                "Unkown USTX field: " + getStringValue(nodeTuple.getKeyNode(), "UNKNOWN"));
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

    private static String getStringValue(Node yamlNode, String fallback) {
        if (!(yamlNode instanceof ScalarNode)) {
            return fallback;
        }
        return ((ScalarNode) yamlNode).getValue();
    }

    private static int getIntValue(Node yamlNode, int fallback) {
        if (!(yamlNode instanceof ScalarNode)) {
            return fallback;
        }
        return RoundUtils.round(Double.parseDouble(((ScalarNode) yamlNode).getValue()));
    }

    private static double getDoubleValue(Node yamlNode, double fallback) {
        if (!(yamlNode instanceof ScalarNode)) {
            return fallback;
        }
        return Double.parseDouble(((ScalarNode) yamlNode).getValue());
    }
}
