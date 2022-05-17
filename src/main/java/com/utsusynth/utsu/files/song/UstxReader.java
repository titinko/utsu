package com.utsusynth.utsu.files.song;

import java.io.File;
import java.io.IOException;
import java.util.*;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.utsusynth.utsu.common.exception.ErrorLogger;
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
    private static final ErrorLogger errorLogger = ErrorLogger.getLogger();

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
            for (NodeTuple yamlEntry : UtsuFileUtils.getYamlMapEntries(yamlNode)) {
                if (UtsuFileUtils.getYamlStringValue(
                        yamlEntry.getKeyNode(), "").equals("voice_parts")) {
                    return Math.max(
                            1, UtsuFileUtils.getYamlListEntries(yamlEntry.getValueNode()).size());
                }
            }
        } catch (IOException e) {
            // TODO: Handle this.
            errorLogger.logError(e);
        }
        return 1;
    }

    @Override
    public Song loadSong(File file, int trackNum) {
        Yaml yaml = new Yaml();
        Song.Builder songBuilder = songProvider.get().toBuilder();
        try {
            Node yamlNode = yaml.represent(yaml.load(FileUtils.openInputStream(file)));
            for (NodeTuple yamlEntry : UtsuFileUtils.getYamlMapEntries(yamlNode)) {
                parseSection(yamlEntry, songBuilder, trackNum);
            }
        } catch (IOException e) {
            // TODO: Handle this.
            errorLogger.logError(e);
        }
        return songBuilder.build();
    }

    private void parseSection(NodeTuple section, Song.Builder builder, int trackNum) {
        Node value = section.getValueNode();
        switch (UtsuFileUtils.getYamlStringValue(section.getKeyNode(), "")) {
            case "name":
                builder.setProjectName(UtsuFileUtils.getYamlStringValue(value, ""));
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
                double tempo = UtsuFileUtils.getYamlDoubleValue(value, -1);
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
                parseTrack(
                        UtsuFileUtils.getYamlListEntries(section.getValueNode()).get(trackNum - 1),
                        builder);
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
        for (NodeTuple section : UtsuFileUtils.getYamlMapEntries(voiceTrack)) {
            Node value = section.getValueNode();
            switch (UtsuFileUtils.getYamlStringValue(section.getKeyNode(), "")) {
                case "name":
                    builder.setProjectName(
                            UtsuFileUtils.getYamlStringValue(section.getValueNode(), ""));
                    break;
                case "comment":
                    // Do nothing.
                    break;
                case "track_no":
                    // Do nothing.
                    break;
                case "position":
                    trackPosition = UtsuFileUtils.getYamlIntValue(value, trackPosition);
                    break;
                case "notes":
                    TreeMap<Integer, Note> sortedNotes = new TreeMap<>();
                    for (Node noteNode : UtsuFileUtils.getYamlListEntries(value)) {
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
        for (NodeTuple section : UtsuFileUtils.getYamlMapEntries(noteNode)) {
            Node value = section.getValueNode();
            switch (UtsuFileUtils.getYamlStringValue(section.getKeyNode(), "")) {
                case "position":
                    notePosition = UtsuFileUtils.getYamlIntValue(value, notePosition);
                    break;
                case "duration":
                    note.setDuration(UtsuFileUtils.getYamlIntValue(value, -1));
                    break;
                case "tone":
                    note.setNoteNum(UtsuFileUtils.getYamlIntValue(value, -1));
                    break;
                case "lyric":
                    note.setLyric(UtsuFileUtils.getYamlStringValue(value, ""));
                    break;
                case "pitch":
                    parsePitch(value, note);
                    break;
                case "vibrato":
                    parseVibrato(value, note);
                    break;
                case "note_expressions":
                case "phoneme_expressions":
                    for (Node expressionNode : UtsuFileUtils.getYamlListEntries(value)) {
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
        for (NodeTuple pitchSection : UtsuFileUtils.getYamlMapEntries(pitchNode)) {
            Node value = pitchSection.getValueNode();
            switch (UtsuFileUtils.getYamlStringValue(pitchSection.getKeyNode(), "")) {
                case "data":
                    List<Node> dataNodes = UtsuFileUtils.getYamlListEntries(value);
                    double curX = 0;
                    double[] pbs = new double[2];
                    double[] pbw = new double[Math.max(dataNodes.size() - 1, 0)];
                    double[] pby = new double[Math.max(dataNodes.size() - 2, 0)];
                    String[] pbm = new String[Math.max(dataNodes.size() - 1, 0)];
                    Arrays.fill(pbm, "");
                    for (int i = 0; i < dataNodes.size(); i++) {
                        for (NodeTuple dataSection
                                : UtsuFileUtils.getYamlMapEntries(dataNodes.get(i))) {
                            Node dataKey = dataSection.getKeyNode();
                            Node dataValue = dataSection.getValueNode();
                            switch(UtsuFileUtils.getYamlStringValue(dataKey, "")) {
                                case "x":
                                    double xVal = UtsuFileUtils.getYamlDoubleValue(dataValue, 0);
                                    if (i == 0) {
                                        pbs[0] = xVal;
                                    } else {
                                        pbw[i - 1] = xVal - curX;
                                    }
                                    curX = xVal;
                                    break;
                                case "y":
                                    if (i > 0 && i < dataNodes.size() - 1) {
                                        pby[i - 1] =
                                                UtsuFileUtils.getYamlDoubleValue(dataValue, 0);
                                    }
                                    break;
                                case "shape":
                                    if (i >= dataNodes.size() - 1) {
                                        break;
                                    }
                                    switch (UtsuFileUtils.getYamlStringValue(dataValue, "io")) {
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
        for (NodeTuple vibratoSection : UtsuFileUtils.getYamlMapEntries(vibratoNode)) {
            Node value = vibratoSection.getValueNode();
            switch (UtsuFileUtils.getYamlStringValue(vibratoSection.getKeyNode(), "")) {
                case "length":
                    vibrato[0] = UtsuFileUtils.getYamlIntValue(value, vibrato[0]);
                    break;
                case "period":
                    vibrato[1] = UtsuFileUtils.getYamlIntValue(value, vibrato[1]);
                    break;
                case "depth":
                    vibrato[2] = UtsuFileUtils.getYamlIntValue(value, vibrato[2]);
                    break;
                case "in":
                    vibrato[3] = UtsuFileUtils.getYamlIntValue(value, vibrato[3]);
                    break;
                case "out":
                    vibrato[4] = UtsuFileUtils.getYamlIntValue(value, vibrato[4]);
                    break;
                case "shift":
                    vibrato[5] = UtsuFileUtils.getYamlIntValue(value, vibrato[5]);
                    break;
                case "drift":
                    vibrato[6] = UtsuFileUtils.getYamlIntValue(value, vibrato[6]);
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
        for (NodeTuple expressionSection : UtsuFileUtils.getYamlMapEntries(expressionNode)) {
            Node value = expressionSection.getValueNode();
            switch (UtsuFileUtils.getYamlStringValue(expressionSection.getKeyNode(), "")) {
                case "index":
                    // Not sure what this does.
                    break;
                case "abbr":
                    expressionName = UtsuFileUtils.getYamlStringValue(value, expressionName);
                    break;
                case "value":
                    expressionValue = UtsuFileUtils.getYamlIntValue(value, expressionValue);
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
        System.out.println("Unkown USTX field: "
                + UtsuFileUtils.getYamlStringValue(nodeTuple.getKeyNode(), "UNKNOWN"));
    }
}
