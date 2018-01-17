package com.utsusynth.utsu.model;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import com.google.common.base.Optional;
import com.utsusynth.utsu.common.PitchUtils;
import com.utsusynth.utsu.common.data.AddResponse;
import com.utsusynth.utsu.common.data.NeighborData;
import com.utsusynth.utsu.common.data.NoteData;
import com.utsusynth.utsu.common.data.PitchbendData;
import com.utsusynth.utsu.common.data.RemoveResponse;
import com.utsusynth.utsu.common.exception.NoteAlreadyExistsException;
import com.utsusynth.utsu.model.pitch.PitchCurve;
import com.utsusynth.utsu.model.voicebank.Voicebank;
import com.utsusynth.utsu.model.voicebank.VoicebankReader;

/**
 * In-code representation of a song. Compatible with UST versions 1.2 and 2.0.
 */
public class Song {
    private final VoicebankReader voicebankReader;
    private final SongNoteStandardizer standardizer;

    // Cached save format and location.
    private Optional<File> saveLocation;
    private String saveFormat;

    // Settings. (Anything marked with [#SETTING])
    // TODO:Insert Time signatures here
    private double tempo;
    private String projectName;
    private File outputFile;
    private File voiceDirectory; // Pulled directly from the oto file.
    private Voicebank voicebank;
    private String flags;
    private boolean mode2 = true;

    // Notes. (Anything marked with [#0000]-[#9999], [#TRACKEND] marks the end of these)
    private SongNoteList noteList;

    // Pitchbends, kept in a format suitable for rendering.
    private PitchCurve pitchbends;

    public class Builder {
        private final Song newSong;
        private final SongNoteList.Builder noteListBuilder;

        private Builder(Song newSong) {
            this.newSong = newSong;
            this.noteListBuilder = newSong.noteList.toBuilder();
        }

        public Builder setTempo(double tempo) {
            newSong.tempo = tempo;
            return this;
        }

        public Builder setProjectName(String projectName) {
            newSong.projectName = projectName;
            return this;
        }

        public Builder setOutputFile(File outputFile) {
            newSong.outputFile = outputFile;
            return this;
        }

        public Builder setVoiceDirectory(File voiceDirectory) {
            newSong.voiceDirectory = voiceDirectory;
            return this;
        }

        public Builder setFlags(String flags) {
            newSong.flags = flags;
            return this;
        }

        public Builder setMode2(boolean mode2) {
            newSong.mode2 = mode2;
            return this;
        }

        public Builder addNote(SongNote note) {
            Optional<SongNote> prevNote = noteListBuilder.getLatestNote();

            // Add this note to the list of notes.
            noteListBuilder.appendNote(note);

            // Add pitchbends for this note.
            newSong.pitchbends.addPitchbends(
                    noteListBuilder.getLatestDelta(),
                    note.getLength(),
                    note.getPitchbends(),
                    prevNote.isPresent() ? prevNote.get().getNoteNum() : note.getNoteNum(),
                    note.getNoteNum());
            return this;
        }

        public Builder addRestNote(SongNote note) {
            noteListBuilder.appendRestNote(note);
            return this;
        }

        public Builder addInvalidNote(SongNote note) {
            noteListBuilder.appendInvalidNote(note.getDelta(), note.getLength());
            return this;
        }

        public Song build() {
            newSong.voicebank =
                    newSong.voicebankReader.loadVoicebankFromDirectory(newSong.voiceDirectory);
            noteListBuilder.standardize(newSong.standardizer, newSong.voicebank);
            newSong.noteList = noteListBuilder.build();
            return newSong;
        }
    }

    public Song(
            VoicebankReader voicebankReader,
            SongNoteStandardizer standardizer,
            SongNoteList songNoteList,
            PitchCurve pitchbends) {
        this.voicebankReader = voicebankReader;
        this.standardizer = standardizer;
        this.saveLocation = Optional.absent();
        this.saveFormat = "UST 2.0 (UTF-8)";
        this.noteList = songNoteList;
        this.pitchbends = pitchbends;
        this.voiceDirectory = voicebankReader.getDefaultPath();
        this.outputFile = new File("outputFile");
        this.voicebank = voicebankReader.loadVoicebankFromDirectory(this.voiceDirectory);
        this.tempo = 125.0;
        this.projectName = "(no title)";
        this.flags = "";
    }

    public Builder toBuilder() {
        // Returns the builder of a new Song with this one's attributes.
        // The old Song's noteList and pitchbends objects are used in the new Song.
        return new Builder(
                new Song(this.voicebankReader, this.standardizer, this.noteList, this.pitchbends))
                        .setTempo(this.tempo).setProjectName(this.projectName)
                        .setOutputFile(this.outputFile).setVoiceDirectory(this.voiceDirectory)
                        .setFlags(this.flags).setMode2(this.mode2);
    }

    /**
     * Adds a note to the song object.
     * 
     * @throws NoteAlreadyExistsException
     */
    public AddResponse addNote(NoteData toAdd) throws NoteAlreadyExistsException {
        SongNote note = new SongNote();
        // New note's delta/length may be overridden while inserting into note list.
        note.setDelta(toAdd.getPosition());
        note.safeSetDuration(toAdd.getDuration());
        note.safeSetLength(toAdd.getDuration());
        note.setLyric(toAdd.getLyric());
        note.setNoteNum(PitchUtils.pitchToNoteNum(toAdd.getPitch()));
        note.setNoteFlags("B0");
        if (toAdd.getEnvelope().isPresent()) {
            note.setEnvelope(toAdd.getEnvelope().get());
        }
        if (toAdd.getPitchbend().isPresent()) {
            note.setPitchbends(toAdd.getPitchbend().get());
        }

        int positionMs = toAdd.getPosition();
        SongNode insertedNode = this.noteList.insertNote(note, positionMs);

        // Standardize note lengths and envelopes, in last -> first order.
        if (insertedNode.getNext().isPresent()) {
            insertedNode.getNext().get().standardize(standardizer, voicebank);
        }
        insertedNode.standardize(standardizer, voicebank);
        if (insertedNode.getPrev().isPresent()) {
            insertedNode.getPrev().get().standardize(standardizer, voicebank);
        }

        // Find neighbors to newly added note.
        Optional<NeighborData> prevNote = Optional.absent();
        if (insertedNode.getPrev().isPresent()) {
            SongNote prevSongNote = insertedNode.getPrev().get().getNote();
            prevNote = Optional.of(
                    new NeighborData(
                            note.getDelta(),
                            prevSongNote.getEnvelope(),
                            prevSongNote.getPitchbends(),
                            prevSongNote.getConfigData()));
        }
        Optional<NeighborData> nextNote = Optional.absent();
        if (insertedNode.getNext().isPresent()) {
            // Modify the next note's portamento.
            SongNote nextSongNote = insertedNode.getNext().get().getNote();
            int nextStart = positionMs + note.getLength();
            pitchbends.removePitchbends(
                    nextStart,
                    nextSongNote.getLength(),
                    nextSongNote.getPitchbends());
            pitchbends.addPitchbends(
                    nextStart,
                    nextSongNote.getLength(),
                    nextSongNote.getPitchbends(),
                    note.getNoteNum(),
                    nextSongNote.getNoteNum());

            nextNote = Optional.of(
                    new NeighborData(
                            note.getLength(),
                            nextSongNote.getEnvelope(),
                            nextSongNote.getPitchbends(),
                            nextSongNote.getConfigData()));
        }

        // Add this note's pitchbends.
        int prevNoteNum = insertedNode.getPrev().isPresent()
                ? insertedNode.getPrev().get().getNote().getNoteNum()
                : note.getNoteNum();
        this.pitchbends.addPitchbends(
                positionMs,
                note.getLength(),
                note.getPitchbends(),
                prevNoteNum,
                note.getNoteNum());

        return new AddResponse(
                new NoteData(
                        positionMs,
                        note.getDuration(),
                        toAdd.getPitch(),
                        note.getLyric(),
                        Optional.of(note.getEnvelope()),
                        Optional.of(note.getPitchbends()),
                        Optional.of(note.getConfigData())),
                prevNote,
                nextNote);
    }

    /** Removes the note at the specified position from the song object. */
    public RemoveResponse removeNote(int positionMs) {
        SongNode removedNode = this.noteList.removeNote(positionMs);

        // Do standardization separately as it must happen in back -> front order.
        if (removedNode.getNext().isPresent()) {
            // Adjust envelope, preutterance, and length of next note.
            removedNode.getNext().get().standardize(standardizer, voicebank);
        }
        if (removedNode.getPrev().isPresent()) {
            // Adjust envelope, preutterance, and length of previous note.
            removedNode.getPrev().get().standardize(standardizer, voicebank);
        }

        Optional<NeighborData> prevNote = Optional.absent();
        if (removedNode.getPrev().isPresent()) {
            SongNote prevSongNote = removedNode.getPrev().get().getNote();
            prevNote = Optional.of(
                    new NeighborData(
                            removedNode.getNote().getDelta(),
                            prevSongNote.getEnvelope(),
                            prevSongNote.getPitchbends(),
                            prevSongNote.getConfigData()));
        }

        Optional<NeighborData> nextNote = Optional.absent();
        if (removedNode.getNext().isPresent()) {
            // Modify the next note's portamento.
            SongNote nextSongNote = removedNode.getNext().get().getNote();
            int nextStart = positionMs + removedNode.getNote().getLength();
            int prevNoteNum = removedNode.getPrev().isPresent()
                    ? removedNode.getPrev().get().getNote().getNoteNum()
                    : nextSongNote.getNoteNum();
            pitchbends.removePitchbends(
                    nextStart,
                    nextSongNote.getLength(),
                    nextSongNote.getPitchbends());
            pitchbends.addPitchbends(
                    nextStart,
                    nextSongNote.getLength(),
                    nextSongNote.getPitchbends(),
                    prevNoteNum,
                    nextSongNote.getNoteNum());

            nextNote = Optional.of(
                    new NeighborData(
                            removedNode.getNote().getLength(),
                            nextSongNote.getEnvelope(),
                            nextSongNote.getPitchbends(),
                            nextSongNote.getConfigData()));
        }

        // Remove this note's pitchbends.
        this.pitchbends.removePitchbends(
                positionMs,
                removedNode.getNote().getLength(),
                removedNode.getNote().getPitchbends());

        return new RemoveResponse(prevNote, nextNote);
    }

    /** Modifies a note in-place without changing its lyric, position, or duration. */
    public void modifyNote(NoteData toModify) {
        int positionMs = toModify.getPosition();
        SongNode node = this.noteList.getNote(positionMs);
        SongNote note = node.getNote();
        if (toModify.getEnvelope().isPresent()) {
            note.setEnvelope(toModify.getEnvelope().get());
        }
        if (toModify.getPitchbend().isPresent()) {
            this.pitchbends.removePitchbends(positionMs, note.getLength(), note.getPitchbends());
            PitchbendData newPitchbend = toModify.getPitchbend().get();
            note.setPitchbends(newPitchbend);

            int prevNoteNum =
                    node.getPrev().isPresent() ? node.getPrev().get().getNote().getNoteNum()
                            : note.getNoteNum();
            this.pitchbends.addPitchbends(
                    positionMs,
                    note.getLength(),
                    newPitchbend,
                    prevNoteNum,
                    note.getNoteNum());
        }
    }

    public LinkedList<NoteData> getNotes() {
        LinkedList<NoteData> notes = new LinkedList<>();
        SongIterator iterator = noteList.iterator();
        int totalDelta = 0;
        while (iterator.hasNext()) {
            SongNote note = iterator.next();
            totalDelta += note.getDelta();
            notes.add(
                    new NoteData(
                            totalDelta,
                            note.getDuration(),
                            PitchUtils.noteNumToPitch(note.getNoteNum()),
                            note.getLyric(),
                            Optional.of(note.getEnvelope()),
                            Optional.of(note.getPitchbends()),
                            Optional.of(note.getConfigData())));
        }
        return notes;
    }

    public String getProjectName() {
        return projectName;
    }

    public File getOutputFile() {
        return outputFile;
    }

    public File getVoiceDir() {
        return voiceDirectory;
    }

    public Voicebank getVoicebank() {
        return voicebank;
    }

    public SongIterator getNoteIterator() {
        return noteList.iterator();
    }

    public String getFlags() {
        return flags;
    }

    public double getTempo() {
        return tempo;
    }

    public boolean getMode2() {
        return mode2;
    }

    public String getPitchString(int firstPitchStep, int lastPitchStep, int noteNum) {
        return pitchbends.renderPitchbends(firstPitchStep, lastPitchStep, noteNum);
    }

    public Optional<File> getSaveLocation() {
        return saveLocation;
    }

    public void setSaveLocation(File newPath) {
        this.saveLocation = Optional.of(newPath);
    }

    public String getSaveFormat() {
        return saveFormat;
    }

    public void setSaveFormat(String saveFormat) {
        this.saveFormat = saveFormat;
    }

    @Override
    public String toString() {
        // Crappy string representation of a Song object.
        String result = "";
        Iterator<SongNote> iterator = noteList.iterator();
        while (iterator.hasNext()) {
            result += iterator.next() + "\n";
        }
        result += voicebank + "\n";
        return result + tempo + projectName + outputFile + voiceDirectory + flags + mode2;
    }
}
