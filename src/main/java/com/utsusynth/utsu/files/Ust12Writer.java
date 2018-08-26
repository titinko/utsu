package com.utsusynth.utsu.files;

import java.io.PrintStream;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.utsusynth.utsu.common.RegionBounds;
import com.utsusynth.utsu.common.RoundUtils;
import com.utsusynth.utsu.model.song.Note;
import com.utsusynth.utsu.model.song.NoteIterator;
import com.utsusynth.utsu.model.song.Song;
import com.utsusynth.utsu.model.voicebank.LyricConfig;

/**
 * Writes a song to a Shift-JIS UST 1.2 file.
 */
public class Ust12Writer {
    /**
     * Writes a special format of UST 1.2 used as an input to legacy UTAU plugins.
     * 
     * @return An array containing the header of the plugin PREV note and the header of the first
     *         note after the plugin NEXT note.
     */
    public String[] writeToPlugin(Song song, RegionBounds bounds, PrintStream ps) {
        String[] headers = new String[] {getNoteLabel(0), getNoteLabel(9999)};

        ps.println("[#VERSION]");
        ps.println("UST Version 1.20"); // Version looks different for plugin input.
        writeSettings(song, ps);

        NoteIterator notes = song.getNoteIterator();
        boolean notesWritten = false;
        int totalDelta = 0;
        for (int index = 0; notes.hasNext(); index++) {
            Note note = notes.next();
            totalDelta += note.getDelta();
            int prevDuration =
                    notes.peekPrev().isPresent() ? notes.peekPrev().get().getDuration() : 0;

            String noteHeader;
            if (bounds.intersects(totalDelta, totalDelta + note.getDuration())) {
                // Case where note is in exported region.
                noteHeader = "";
            } else {
                if (notes.peekNext().isPresent() && bounds.intersects(
                        totalDelta + note.getLength(),
                        totalDelta + note.getLength() + notes.peekNext().get().getDuration())) {
                    // Case where note is just before exported region.
                    noteHeader = "[#PREV]";
                    // Rest notes before PREV aren't written.
                    index += getRestNotes(prevDuration, note).size();
                    headers[0] = getNoteLabel(index);
                } else if (notes.peekPrev().isPresent() && bounds.intersects(
                        totalDelta - note.getDelta(),
                        totalDelta - note.getDelta() + notes.peekPrev().get().getDuration())) {
                    // Case where note is just after exported region.
                    noteHeader = "[#NEXT]";
                    headers[1] = getNoteLabel(index + 1);
                } else {
                    // Keep track of indices but ignore note.
                    index += getRestNotes(prevDuration, note).size();
                    continue;
                }
            }

            // Write preceding rest notes if necessary.
            if (!noteHeader.equals("[#PREV]")) {
                for (int restLength : getRestNotes(prevDuration, note)) {
                    ps.println(getNoteLabel(index));
                    index++;
                    ps.println("Length=" + restLength);
                    ps.println("Lyric=R");
                    ps.println("NoteNum=60");
                }
            }
            // Write current note.
            if (noteHeader.isEmpty()) {
                noteHeader = getNoteLabel(index);
            }
            writeNote(noteHeader, note, ps);
            notesWritten = true;

            // Write extra data in plugin format.
            ps.println("@preuttr=" + note.getRealPreutter());
            ps.println("@overlap=" + note.getFadeIn());
            ps.println("@stpoint=" + note.getAutoStartPoint());

            // Write lyric data if readily available.
            if (!note.getTrueLyric().isEmpty()) {
                Optional<LyricConfig> config =
                        song.getVoicebank().getLyricConfig(note.getTrueLyric());
                if (config.isPresent()) {
                    ps.println("@filename=" + config.get().getFilename());
                    ps.println("@alias=" + note.getTrueLyric());
                }
            }
        }
        ps.println("[#TRACKEND]");

        // Special case where no notes are written to plugin.
        if (!notesWritten) {
            headers[0] = getNoteLabel(9999);
        }
        return headers;
    }

    public void writeSong(Song song, PrintStream ps) {
        ps.println("[#VERSION]");
        ps.println("UST Version1.2");
        writeSettings(song, ps);

        NoteIterator notes = song.getNoteIterator();
        for (int index = 0; notes.hasNext(); index++) {
            Note note = notes.next();
            int prevDuration =
                    notes.peekPrev().isPresent() ? notes.peekPrev().get().getDuration() : 0;
            // Write rest notes.
            for (int restLength : getRestNotes(prevDuration, note)) {
                ps.println(getNoteLabel(index));
                index++;
                ps.println("Length=" + restLength);
                ps.println("Lyric=R");
                ps.println("NoteNum=60");
            }
            writeNote(getNoteLabel(index), note, ps);
        }
        ps.println("[#TRACKEND]");
    }

    private void writeSettings(Song song, PrintStream ps) {
        ps.println("[#SETTING]");
        ps.println("Tempo=" + RoundUtils.roundDecimal(song.getTempo(), "#.##"));
        ps.println("ProjectName=" + song.getProjectName());
        ps.println("OutFile=" + song.getOutputFile());
        ps.println("VoiceDir=" + song.getVoiceDir());
        ps.println("Flags=" + song.getFlags());
        ps.println("Mode2=" + (song.getMode2() ? "True" : "False"));
    }

    private ImmutableList<Integer> getRestNotes(int prevDuration, Note note) {
        ImmutableList.Builder<Integer> builder = ImmutableList.builder();
        if (note.getDelta() > prevDuration) {
            int numFullRestNotes = (note.getDelta() - prevDuration) / 480;
            for (int i = 0; i < numFullRestNotes; i++) {
                builder.add(480);
            }
            int leftoverMs = (note.getDelta() - prevDuration) % 480;
            if (leftoverMs > 0) {
                builder.add(leftoverMs);
            }
        }
        return builder.build();
    }

    private void writeNote(String noteLabel, Note note, PrintStream ps) {
        ps.println(noteLabel);
        ps.println("Length=" + note.getDuration());
        ps.println("Lyric=" + note.getLyric());
        ps.println("NoteNum=" + note.getNoteNum());
        if (note.getPreutter().isPresent()) {
            ps.println("PreUtterance=" + RoundUtils.roundDecimal(note.getPreutter().get(), "#.##"));
        }
        if (note.getOverlap().isPresent()) {
            ps.println("VoiceOverlap=" + RoundUtils.roundDecimal(note.getOverlap().get(), "#.##"));
        }
        ps.println("Velocity=" + RoundUtils.roundDecimal(note.getVelocity(), "#.##"));
        ps.println("StartPoint=" + RoundUtils.roundDecimal(note.getStartPoint(), "#.##"));
        ps.println("Intensity=" + note.getIntensity());
        ps.println("Modulation=" + note.getModulation());
        ps.println("Flags=" + note.getNoteFlags());

        // Pitch bends.
        ImmutableList<Double> pbs = note.getPBS();
        ps.print("PBS=");
        for (int i = 0; i < pbs.size() - 1; i++) {
            ps.print(RoundUtils.roundDecimal(pbs.get(i), "#.#") + ",");
        }
        ps.println(RoundUtils.roundDecimal(pbs.get(pbs.size() - 1), "#.#"));
        ImmutableList<Double> pbw = note.getPBW();
        ps.print("PBW=");
        for (int i = 0; i < pbw.size() - 1; i++) {
            ps.print(RoundUtils.roundDecimal(pbw.get(i), "#.#") + ",");
        }
        ps.println(RoundUtils.roundDecimal(pbw.get(pbw.size() - 1), "#.#"));
        ImmutableList<Double> pby = note.getPBY();
        if (!pby.isEmpty()) {
            ps.print("PBY=");
            for (int i = 0; i < pby.size() - 1; i++) {
                ps.print(RoundUtils.roundDecimal(pby.get(i), "#.#") + ",");
            }
            ps.println(RoundUtils.roundDecimal(pby.get(pby.size() - 1), "#.#"));
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
            ps.print(RoundUtils.roundDecimal(value, "#.#") + ",");
        }
        ps.println("0.0"); // Not sure what the meaning of this value is.

        // Vibrato.
        ps.print("VBR=");
        String[] vibrato = note.getVibrato();
        for (int i = 0; i < 9; i++) {
            ps.print(vibrato[i] + ",");
        }
        ps.println(vibrato[9]);
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
}
