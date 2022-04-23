package com.utsusynth.utsu.files.song;

import java.io.File;
import java.io.IOException;
import java.util.*;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.utsusynth.utsu.common.utils.RoundUtils;
import com.utsusynth.utsu.common.utils.UtsuFileUtils;
import com.utsusynth.utsu.files.voicebank.VoicebankReader;
import com.utsusynth.utsu.model.song.Note;
import com.utsusynth.utsu.model.song.Song;
import org.apache.commons.io.FileUtils;
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
    public String getSaveFormat(File file) {
        return "";
    }

    @Override
    public int getNumTracks(File file) {
        Yaml yaml = new Yaml();
        try {
            Node yamlNode = yaml.represent(yaml.load(FileUtils.openInputStream(file)));
            for (NodeTuple yamlEntry : getMapEntries(yamlNode)) {
                if (getStringValue(yamlEntry.getKeyNode(), "").equals("voice_parts")) {
                    return Math.max(1, getListEntries(yamlEntry.getValueNode()).size());
                }
            }
        } catch (IOException e) {
            // TODO
        }
        return 1;
    }

    @Override
    public Song loadSong(File file, int trackNum) {
        Yaml yaml = new Yaml();
        Song.Builder songBuilder = songProvider.get().toBuilder();
        try {
            Node yamlNode = yaml.represent(yaml.load(FileUtils.openInputStream(file)));
            for (NodeTuple yamlEntry : getMapEntries(yamlNode)) {
                parseSection(yamlEntry, songBuilder, trackNum);
            }
        } catch (IOException e) {
            // TODO
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
        int trackPosition = 0;
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
                    trackPosition = getIntValue(value, trackPosition);
                    break;
                case "notes":
                    TreeMap<Integer, Note> sortedNotes = new TreeMap<>();
                    for (Node noteNode : getListEntries(value)) {
                        parseNote(noteNode, sortedNotes, trackPosition);
                    }
                    addAllNotes(sortedNotes, builder);
                    break;
                case "tags":
                    // Do nothing.
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

    private void parseNote(Node noteNode, TreeMap<Integer, Note> sortedNotes, int trackPosition) {
        int notePosition = -1;
        Note note = new Note();
        for (NodeTuple section : getMapEntries(noteNode)) {
            Node value = section.getValueNode();
            switch (getStringValue(section.getKeyNode(), "")) {
                case "position":
                    notePosition = getIntValue(value, notePosition);
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
                case "phoneme_expressions":
                    for (Node expressionNode : getListEntries(value)) {
                        parseExpression(expressionNode, note);
                    }
                    break;
                case "phoneme_overrides":
                    // Do nothing.
                    break;
                default:
                    printKeyErrorMessage(section);
            }
        }
        if (notePosition > 0) {
            sortedNotes.put(trackPosition + notePosition, note);
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

    private void parseExpression(Node expressionNode, Note note) {
        String expressionName = "";
        Integer expressionValue = null;
        for (NodeTuple expressionSection : getMapEntries(expressionNode)) {
            Node value = expressionSection.getValueNode();
            switch (getStringValue(expressionSection.getKeyNode(), "")) {
                case "index":
                    // Not sure what this does.
                    break;
                case "abbr":
                    expressionName = getStringValue(value, expressionName);
                    break;
                case "value":
                    expressionValue = getIntValue(value, expressionValue);
                    break;
                default:
                    printKeyErrorMessage(expressionSection);
            }
        }
        if (expressionValue == null) {
            return;
        }
        switch (expressionName.toLowerCase()) {
            case "vel":
                note.setVelocity(Math.max(0, Math.min(200, expressionValue)));
                break;
            case "vol":
                note.setIntensity(Math.max(0, Math.min(200, expressionValue)));
                break;
            case "gen":
                int genderValue = Math.max(-100, Math.min(100, expressionValue));
                note.setNoteFlags(note.getNoteFlags() + "g" + genderValue);
                break;
            case "bre":
                int breathValue = Math.max(0, Math.min(100, expressionValue));
                note.setNoteFlags(note.getNoteFlags() + "B" + breathValue);
                break;
            case "lpf":
                int lowPassValue = Math.max(0, Math.min(100, expressionValue));
                note.setNoteFlags(note.getNoteFlags() + "H" + lowPassValue);
                break;
            case "mod":
                note.setModulation(Math.max(0, Math.min(100, expressionValue)));
                break;
            default:
                // Ignore other expressions.
        }
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

    private static Integer getIntValue(Node yamlNode, Integer fallback) {
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
