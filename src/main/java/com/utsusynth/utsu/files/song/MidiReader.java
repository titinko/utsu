package com.utsusynth.utsu.files.song;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.utsusynth.utsu.UtsuModule.DefaultLyric;
import com.utsusynth.utsu.common.utils.RoundUtils;
import com.utsusynth.utsu.files.voicebank.VoicebankReader;
import com.utsusynth.utsu.model.song.Note;
import com.utsusynth.utsu.model.song.Song;

import javax.sound.midi.*;

/**
 * Reads a song from a MIDI file.
 */
public class MidiReader implements SongReader {
    public static final int NOTE_ON = 0x90;
    public static final int NOTE_OFF = 0x80;
    public static final int TEMPO = 0x51;

    private final Provider<Song> songProvider;
    private final String defaultLyric;

    @Inject
    public MidiReader(
            Provider<Song> songProvider,
            @DefaultLyric String defaultLyric) {
        this.songProvider = songProvider;
        this.defaultLyric = defaultLyric;
    }

    @Override
    public String getSaveFormat(File file) {
        return "";
    }

    @Override
    public int getNumTracks(File file) {
        try {
            Sequence sequence = MidiSystem.getSequence(file);
            return sequence.getTracks().length;
        } catch (InvalidMidiDataException | IOException e) {
            System.out.println("Unable to detect number of MIDI tracks, assuming 1.");
        }
        return 1;
    }

    @Override
    public Song loadSong(File file, int trackNum) {
        Song.Builder songBuilder = songProvider.get().toBuilder();
        TreeMap<Integer, Note> sortedNotes = new TreeMap<>();
        try {
            Sequence sequence = MidiSystem.getSequence(file);
            int resolution = sequence.getResolution();
            for (int curTrack = 0; curTrack < sequence.getTracks().length; curTrack++) {
                Track track = sequence.getTracks()[curTrack];
                for (int eventNum = 0; eventNum < track.size(); eventNum++) {
                    MidiEvent event = track.get(eventNum);
                    MidiMessage message = event.getMessage();
                    parseTempo(message, songBuilder); // Any track can contain the tempo.
                    if (curTrack + 1 != trackNum) {
                        continue; // Notes must come from the right track.
                    }
                    if (!(message instanceof ShortMessage)) {
                        continue; // Notes are of this message type.
                    }
                    ShortMessage shortMessage = (ShortMessage) message;
                    if (shortMessage.getCommand() != NOTE_ON) {
                        continue; // Indicates the start of a new note.
                    }
                    Note note = new Note();
                    note.setNoteNum(shortMessage.getData1());
                    note.setLyric(defaultLyric);
                    note.setDuration(20); // Default duration to be overwritten.
                    int noteStart = scaleTick(event.getTick(), resolution);
                    for (int nextNum = eventNum + 1; nextNum < track.size(); nextNum++) {
                        MidiEvent nextEvent = track.get(nextNum);
                        MidiMessage nextMessage = nextEvent.getMessage();
                        if (!(nextMessage instanceof ShortMessage)) {
                            continue;
                        }
                        ShortMessage nextShortMessage = (ShortMessage) nextMessage;
                        if (nextShortMessage.getCommand() != NOTE_OFF) {
                            continue; // For every NOTE_ON, we look for a later NOTE_OFF event.
                        }
                        int noteEnd = scaleTick(nextEvent.getTick(), resolution);
                        note.setDuration(noteEnd - noteStart);
                        break;
                    }
                    sortedNotes.put(noteStart, note);
                }
            }
        } catch (InvalidMidiDataException | IOException e) {
            System.out.println("Unable to parse MIDI file, returning empty song.");
        }
        addAllNotes(sortedNotes, songBuilder);
        return songBuilder.build();
    }

    /** If a midi message contains a tempo, parse and give to song builder. */
    private void parseTempo(MidiMessage message, Song.Builder songBuilder) {
        if (!(message instanceof MetaMessage)) {
            return;
        }
        MetaMessage metaMessage = (MetaMessage) message;
        if (metaMessage.getType() != TEMPO) {
            return;
        }
        byte[] data = metaMessage.getData();
        if (data.length < 3) {
            return;
        }
        int microsPerQuarterNote = (data[2] & 0xFF)
                | ((data[1] & 0xFF) << 8)
                | ((data[0] & 0xFF) << 16);
        if (microsPerQuarterNote <= 0) {
            microsPerQuarterNote = 1;
        }
        double tempo = ((double) 60_000_000L) / microsPerQuarterNote;
        if (tempo > 0) {
            songBuilder.setTempo(tempo);
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

    private static int scaleTick(long tick, int resolution) {
        // Convert to UTSU's resolution of 480 ticks per quarter note.
        double scale = 480.0 / resolution;
        return RoundUtils.round(scale * tick);
    }
}
