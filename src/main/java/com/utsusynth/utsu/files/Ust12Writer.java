package com.utsusynth.utsu.files;

import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.Iterator;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.utsusynth.utsu.model.song.Note;
import com.utsusynth.utsu.model.song.Song;

/**
 * Writes a song to a Shift-JIS UST 1.2 file.
 */
public class Ust12Writer {

    public void writeSong(Song song, PrintStream ps) {
        ps.println("[#VERSION]");
        ps.println("UST Version1.2");
        ps.println("[#SETTING]");
        ps.println("Tempo=" + roundDecimal(song.getTempo(), "#.##"));
        ps.println("ProjectName=" + song.getProjectName());
        ps.println("OutFile=" + song.getOutputFile());
        ps.println("VoiceDir=" + song.getVoiceDir());
        ps.println("Flags=" + song.getFlags());
        ps.println("Mode2=" + (song.getMode2() ? "True" : "False"));

        Iterator<Note> iterator = song.getNoteIterator();
        int index = 0;
        Optional<Note> prevNote = Optional.absent();
        while (iterator.hasNext()) {
            int prevDuration = prevNote.isPresent() ? prevNote.get().getDuration() : 0;
            Note note = iterator.next();
            if (note.getDelta() > prevDuration) {
                int numRestNotes = (note.getDelta() - prevDuration) / 480;
                int leftoverMs = (note.getDelta() - prevDuration) % 480;
                // Insert rest notes.
                for (int i = 0; i < numRestNotes; i++) {
                    ps.println(getNoteLabel(index));
                    index++;
                    ps.println("Length=" + 480);
                    ps.println("Lyric=R");
                    ps.println("NoteNum=60");
                }
                if (leftoverMs > 0) {
                    ps.println(getNoteLabel(index));
                    index++;
                    ps.println("Length=" + leftoverMs);
                    ps.println("Lyric=R");
                    ps.println("NoteNum=60");
                }
            }
            ps.println(getNoteLabel(index));
            index++;
            ps.println("Length=" + note.getDuration());
            ps.println("Lyric=" + note.getLyric());
            ps.println("NoteNum=" + note.getNoteNum());
            ps.println("Velocity=" + roundDecimal(note.getVelocity(), "#.##"));
            ps.println("StartPoint=" + roundDecimal(note.getStartPoint(), "#.##"));
            ps.println("Intensity=" + note.getIntensity());
            ps.println("Modulation=" + note.getModulation());
            ps.println("Flags=" + note.getNoteFlags());

            // Pitch bends.
            ImmutableList<Double> pbs = note.getPBS();
            ps.print("PBS=");
            for (int i = 0; i < pbs.size() - 1; i++) {
                ps.print(Double.toString(pbs.get(i)) + ",");
            }
            ps.println(Double.toString(pbs.get(pbs.size() - 1)));
            ImmutableList<Double> pbw = note.getPBW();
            ps.print("PBW=");
            for (int i = 0; i < pbw.size() - 1; i++) {
                ps.print(Double.toString(pbw.get(i)) + ",");
            }
            ps.println(Double.toString(pbw.get(pbw.size() - 1)));
            ImmutableList<Double> pby = note.getPBY();
            if (!pby.isEmpty()) {
                ps.print("PBY=");
                for (int i = 0; i < pby.size() - 1; i++) {
                    ps.print(Double.toString(pby.get(i)) + ",");
                }
                ps.println(Double.toString(pbw.get(pbw.size() - 1)));
            }
            ImmutableList<String> pbm = note.getPBM();
            if (!pbm.isEmpty()) {
                ps.print("PBM=");
                for (int i = 0; i < pbm.size() - 1; i++) {
                    ps.print(pbm.get(i) + ",");
                }
                ps.println(pbm.get(pbm.size() - 1));
            }

            // Envelope.
            ps.print("Envelope=");
            for (String value : note.getFullEnvelope()) {
                ps.print(value + ",");
            }
            ps.println("0.0"); // Not sure what the meaning of this value is.

            // Vibrato.
            ps.print("VBR=");
            String[] vibrato = note.getVibrato();
            for (int i = 0; i < 9; i++) {
                ps.print(vibrato[i] + ",");
            }
            ps.println(vibrato[9]);

            // Save the current note for one more iteration.
            prevNote = Optional.of(note);
        }
        ps.println("[#TRACKEND]");
    }

    private String getNoteLabel(int index) {
        if (index > 9999) {
            // TODO: Throw error
            System.out.println("Too many notes!");
        } else if (index >= 1000) {
            return "[#" + index + "]";
        } else if (index >= 100) {
            return "[#0" + index + "]";
        } else if (index >= 10) {
            return "[#00" + index + "]";
        } else if (index >= 0) {
            return "[#000" + index + "]";
        } else {
            // TODO: Throw error
            System.out.println("Negative notes!");
        }
        return null;
    }

    private String roundDecimal(double number, String roundFormat) {
        int formatNumPlaces = roundFormat.length() - roundFormat.indexOf(".") - 1;
        String formatted = new DecimalFormat(roundFormat).format(number);
        if (formatted.contains(".")) {
            int numPlaces = formatted.length() - formatted.indexOf(".") - 1;
            for (int i = numPlaces; i < formatNumPlaces; i++) {
                formatted = formatted + "0";
            }
        } else {
            formatted = formatted + ".";
            for (int i = 0; i < formatNumPlaces; i++) {
                formatted = formatted + "0";
            }
        }
        return formatted;
    }
}
