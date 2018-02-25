package com.utsusynth.utsu.files;

import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.Iterator;
import com.google.common.collect.ImmutableList;
import com.utsusynth.utsu.model.song.Note;
import com.utsusynth.utsu.model.song.Song;

/**
 * Writes a song to a Unicode UST 2.0 file.
 */
public class Ust20Writer {
    public void writeSong(Song song, PrintStream ps, String charset) {
        ps.println("[#VERSION]");
        ps.println("UST Version2.0");
        ps.println("Charset=" + charset);
        ps.println("[#SETTING]");
        ps.println("TimeSignatures=(4/4/0),");
        ps.println("Tempo=" + roundDecimal(song.getTempo(), "#.##"));
        ps.println("ProjectName=" + song.getProjectName());
        ps.println("OutFile=" + song.getOutputFile());
        ps.println("VoiceDir=" + song.getVoiceDir());
        ps.println("Flags=" + song.getFlags());
        ps.println("Mode2=" + (song.getMode2() ? "True" : "False"));

        Iterator<Note> iterator = song.getNoteIterator();
        int index = 0;
        while (iterator.hasNext()) {
            Note note = iterator.next();
            ps.println(getNoteLabel(index));
            ps.println("Delta=" + note.getDelta());
            ps.println("Duration=" + note.getDuration());
            ps.println("Length=" + note.getLength());
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
                ps.print(roundDecimal(pbs.get(i), "#.#") + ",");
            }
            ps.println(roundDecimal(pbs.get(pbs.size() - 1), "#.#"));
            ImmutableList<Double> pbw = note.getPBW();
            ps.print("PBW=");
            for (int i = 0; i < pbw.size() - 1; i++) {
                ps.print(roundDecimal(pbw.get(i), "#.#") + ",");
            }
            ps.println(roundDecimal(pbw.get(pbw.size() - 1), "#.#"));
            ImmutableList<Double> pby = note.getPBY();
            if (!pby.isEmpty()) {
                ps.print("PBY=");
                for (int i = 0; i < pby.size() - 1; i++) {
                    ps.print(roundDecimal(pby.get(i), "#.#") + ",");
                }
                ps.println(roundDecimal(pby.get(pby.size() - 1), "#.#"));
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
            for (double value : note.getRawFullEnvelope()) {
                ps.print(roundDecimal(value, "#.#") + ",");
            }
            ps.println("0.0,1.0,100.0,1.0,100.0"); // Not sure what the meaning of these values is.

            // Vibrato.
            ps.print("VBR=");
            String[] vibrato = note.getVibrato();
            for (int i = 0; i < 9; i++) {
                ps.print(vibrato[i] + ",");
            }
            ps.println(vibrato[9]);
            index++;
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
