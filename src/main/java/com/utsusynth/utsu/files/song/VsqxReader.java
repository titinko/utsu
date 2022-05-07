package com.utsusynth.utsu.files.song;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.utsusynth.utsu.common.utils.RoundUtils;
import com.utsusynth.utsu.model.song.Note;
import com.utsusynth.utsu.model.song.Song;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Reads a song from a VSQx file.
 */
public class VsqxReader implements SongReader {
    private final Provider<Song> songProvider;
    private final DocumentBuilderFactory documentBuilderFactory;

    @Inject
    public VsqxReader(
            Provider<Song> songProvider, DocumentBuilderFactory documentBuilderFactory) {
        this.songProvider = songProvider;
        this.documentBuilderFactory = documentBuilderFactory;
    }

    @Override
    public String getSaveFormat(File file) {
        return "";
    }

    @Override
    public int getNumTracks(File file) {
        try {
            documentBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Node root = documentBuilder.parse(file).getDocumentElement();
            if (!ImmutableList.of("vsq3", "vsq4").contains(root.getNodeName())) {
                System.out.println("Unable to detect number of VSQx tracks, assuming 1.");
                return 1;
            }
            NodeList elements = root.getChildNodes();
            int numTracks = 0;
            for (int i = 0; i < elements.getLength(); i++) {
                Node element = elements.item(i);
                if (element.getNodeName().equals("vsTrack")) {
                    numTracks++;
                }
            }
            return numTracks;
        } catch (Exception e) {
            System.out.println("Unable to detect number of VSQx tracks, assuming 1.");
        }
        return 1;
    }

    @Override
    public Song loadSong(File file, int trackNum) {
        Song.Builder songBuilder = songProvider.get().toBuilder();
        try {
            documentBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Node root = documentBuilder.parse(file).getDocumentElement();
            if (!ImmutableList.of("vsq3", "vsq4").contains(root.getNodeName())) {
                System.out.println("Unable to parse VSQx file: not vsq3 or vsq4.");
                return songBuilder.build();
            }
            NodeList elements = root.getChildNodes();
            double resolution = 480;
            for (int i = 0; i < elements.getLength(); i++) {
                Node element = elements.item(i);
                if (element.getNodeName().equals("masterTrack")) {
                    resolution = parseSettings(element, songBuilder);
                } else if (element.getNodeName().equals("vsTrack")) {
                    parseTrack(element, songBuilder, trackNum, resolution);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Unable to parse VSQx file, returning empty song.");
        }
        return songBuilder.build();
    }

    private double parseSettings(Node settings, Song.Builder songBuilder) {
        NodeList elements = settings.getChildNodes();
        double resolution = 480;
        for (int i = 0; i < elements.getLength(); i++) {
            Node element = elements.item(i);
            switch (element.getNodeName()) {
                case "resolution":
                    resolution = safeParseDouble(element.getTextContent(), resolution);
                    break;
                case "tempo":
                    NodeList tempoElements = element.getChildNodes();
                    for (int j = 0; j < tempoElements.getLength(); j++) {
                        Node tempoElement = tempoElements.item(j);
                        switch(tempoElement.getNodeName()) {
                            case "bpm":
                            case "v":
                                double tempo = safeParseDouble(tempoElement.getTextContent(), 125);
                                songBuilder.setTempo(tempo / 100);
                                break;
                            case "posTick":
                            case "t":
                            default:
                                // Do nothing.
                        }
                    }
                    break;
                case "preMeasure":
                case "timeSig":
                default:
                    // Do nothing.
            }
        }
        return resolution;
    }

    private void parseTrack(Node track, Song.Builder songBuilder, int trackNum, double resolution) {
        int trackPosition = 0;
        TreeMap<Integer, Note> sortedNotes = new TreeMap<>();
        NodeList elements = track.getChildNodes();
        for (int i = 0; i < elements.getLength(); i++) {
            Node element = elements.item(i);
            switch (element.getNodeName()) {
                case "vsTrackNo":
                case "tNo":
                    if (trackNum != safeParseInt(element.getTextContent(), trackNum) + 1) {
                        // Skip if the current track is not the requested one.
                        return;
                    }
                    break;
                case "musicalPart":
                case "vsPart":
                    NodeList partElements = element.getChildNodes();
                    for (int j = 0; j < partElements.getLength(); j++) {
                        Node partElement = partElements.item(j);
                        switch(partElement.getNodeName()) {
                            case "posTick":
                            case "t":
                                trackPosition = scaleTick(safeParseInt(
                                        partElement.getTextContent(), trackPosition), resolution);
                                break;
                            case "note":
                                parseNote(partElement, sortedNotes, trackPosition, resolution);
                                break;
                            default:
                                // Do nothing.
                        }
                    }
                    break;
                default:
                    // Do nothing.
            }
        }
        addAllNotes(sortedNotes, songBuilder);
    }

    private void parseNote(
            Node noteNode, TreeMap<Integer, Note> sortedNotes, int trackPosition, double resolution) {
        int notePosition = -1;
        Note note = new Note();
        NodeList elements = noteNode.getChildNodes();
        for (int i = 0; i < elements.getLength(); i++) {
            Node element = elements.item(i);
            switch(element.getNodeName()) {
                case "posTick":
                case "t":
                    int positionTicks = safeParseInt(element.getTextContent(), notePosition);
                    notePosition = scaleTick(positionTicks, resolution);
                    break;
                case "durTick":
                case "dur":
                    int durationTicks = safeParseInt(element.getTextContent(), 20);
                    note.setDuration(scaleTick(durationTicks, resolution));
                    break;
                case "noteNum":
                case "n":
                    note.setNoteNum(safeParseInt(element.getTextContent(), 60));
                    break;
                case "lyric":
                case "y":
                    // Expecting format "<![CDATA[lyric]]>".
                    String lyric = element.getTextContent();
                    if (lyric.startsWith("<![CDATA[") && lyric.endsWith("]]>")) {
                        lyric = lyric.substring
                                ("<![CDATA[".length(), lyric.length() - "]]>".length());
                    }
                    note.setLyric(lyric);
            }
        }
        if (notePosition > 0) {
            sortedNotes.put(trackPosition + notePosition, note);
        }
    }

    private void addAllNotes(TreeMap<Integer, Note> sortedNotes, Song.Builder songBuilder) {
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
                songBuilder.addRestNote(restNote);
                curPosition = newPosition;
            }
            curPosition += entry.getValue().getDuration();
            songBuilder.addNote(entry.getValue());
        }
    }

    private static int safeParseInt(String number, int backup) {
        try {
            return Integer.parseInt(number);
        } catch (NumberFormatException e) {
            System.out.println("Warning: Unable to parse integer value in VSQx.");
            return backup;
        }
    }

    private static double safeParseDouble(String number, double backup) {
        try {
            return Double.parseDouble(number);
        } catch (NumberFormatException e) {
            System.out.println("Warning: Unable to parse double value in VSQx.");
            return backup;
        }
    }

    private static int scaleTick(int tick, double resolution) {
        // Convert to UTSU's resolution of 480 ticks per quarter note.
        double scale = 480.0 / resolution;
        return RoundUtils.round(scale * tick);
    }
}
