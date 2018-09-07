package com.utsusynth.utsu.model.song;

import java.io.File;
import java.util.LinkedList;
import com.google.common.base.Optional;
import com.utsusynth.utsu.common.RegionBounds;
import com.utsusynth.utsu.common.data.MutateResponse;
import com.utsusynth.utsu.common.data.NoteUpdateData;
import com.utsusynth.utsu.common.data.NoteData;
import com.utsusynth.utsu.common.data.PitchbendData;
import com.utsusynth.utsu.common.exception.NoteAlreadyExistsException;
import com.utsusynth.utsu.common.utils.PitchUtils;
import com.utsusynth.utsu.model.song.pitch.PitchCurve;
import com.utsusynth.utsu.model.voicebank.Voicebank;
import com.utsusynth.utsu.model.voicebank.VoicebankContainer;

/**
 * In-code representation of a song. Compatible with UST versions 1.2 and 2.0.
 */
public class Song {
    private final VoicebankContainer voicebank;
    private final NoteStandardizer standardizer;

    // Settings. (Anything marked with [#SETTING])
    // TODO:Insert Time signatures here
    private double tempo;
    private String projectName;
    private File outputFile;
    private String flags;
    private boolean mode2 = true;

    // Notes. (Anything marked with [#0000]-[#9999], [#TRACKEND] marks the end of these)
    private NoteList noteList;

    // Pitchbends, kept in a format suitable for rendering.
    private PitchCurve pitchbends;

    public class Builder {
        private final Song newSong;
        private final NoteList.Builder noteListBuilder;

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
            newSong.voicebank.setVoicebank(voiceDirectory);
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

        public Builder addNote(Note note) {
            Optional<Note> prevNote = noteListBuilder.getLatestNote();

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

        public Builder addRestNote(Note note) {
            noteListBuilder.appendRestNote(note);
            return this;
        }

        public Builder addInvalidNote(Note note) {
            noteListBuilder.appendInvalidNote(note.getDelta(), note.getLength());
            return this;
        }

        public Song build() {
            noteListBuilder.standardize(newSong.standardizer, newSong.voicebank.get());
            newSong.noteList = noteListBuilder.build();
            return newSong;
        }
    }

    public Song(
            VoicebankContainer voicebankContainer,
            NoteStandardizer standardizer,
            NoteList songNoteList,
            PitchCurve pitchbends) {
        this.voicebank = voicebankContainer;
        this.standardizer = standardizer;
        this.noteList = songNoteList;
        this.pitchbends = pitchbends;
        this.outputFile = new File("outputFile");
        this.tempo = 125.0;
        this.projectName = "(no title)";
        this.flags = "";
    }

    public Builder toBuilder() {
        // Returns the builder of a new Song with this one's attributes.
        // The old Song's noteList and pitchbends objects are used in the new Song.
        return new Builder(
                new Song(this.voicebank, this.standardizer, this.noteList, this.pitchbends))
                        .setTempo(this.tempo).setProjectName(this.projectName)
                        .setOutputFile(this.outputFile).setFlags(this.flags).setMode2(this.mode2);
    }

    /**
     * Adds a note to the song object.
     * 
     * @throws NoteAlreadyExistsException
     */
    public MutateResponse addNote(NoteData toAdd) throws NoteAlreadyExistsException {
        Note note = new Note();
        // New note's delta/length may be overridden while inserting into note list.
        note.setDelta(toAdd.getPosition());
        note.safeSetDuration(toAdd.getDuration());
        note.safeSetLength(toAdd.getDuration());
        note.setLyric(toAdd.getLyric());
        note.setNoteNum(PitchUtils.pitchToNoteNum(toAdd.getPitch()));
        if (toAdd.getEnvelope().isPresent()) {
            note.setEnvelope(toAdd.getEnvelope().get());
        }
        if (toAdd.getPitchbend().isPresent()) {
            note.setPitchbends(toAdd.getPitchbend().get());
        }
        if (toAdd.getConfigData().isPresent()) {
            note.setConfigData(toAdd.getConfigData().get());
        }

        int positionMs = toAdd.getPosition();
        NoteNode insertedNode = this.noteList.insertNote(note, positionMs);

        // Standardize note lengths and envelopes, in last -> first order.
        if (insertedNode.getNext().isPresent()) {
            insertedNode.getNext().get().standardize(standardizer, voicebank.get());
        }
        insertedNode.standardize(standardizer, voicebank.get());
        if (insertedNode.getPrev().isPresent()) {
            insertedNode.getPrev().get().standardize(standardizer, voicebank.get());
        }

        // Find neighbors to newly added note.
        Optional<NoteUpdateData> prevNote = Optional.absent();
        if (insertedNode.getPrev().isPresent()) {
            Note prevSongNote = insertedNode.getPrev().get().getNote();
            prevNote = Optional.of(
                    new NoteUpdateData(
                            positionMs - note.getDelta(),
                            prevSongNote.getTrueLyric(),
                            prevSongNote.getEnvelope(),
                            prevSongNote.getPitchbends(),
                            prevSongNote.getConfigData()));
        }
        Optional<NoteUpdateData> nextNote = Optional.absent();
        if (insertedNode.getNext().isPresent()) {
            // Modify the next note's portamento.
            Note nextSongNote = insertedNode.getNext().get().getNote();
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
                    new NoteUpdateData(
                            nextStart,
                            nextSongNote.getTrueLyric(),
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

        return new MutateResponse(
                new NoteUpdateData(
                        positionMs,
                        note.getTrueLyric(),
                        note.getEnvelope(),
                        note.getPitchbends(),
                        note.getConfigData()),
                prevNote,
                nextNote);
    }

    /** Removes the note at the specified position from the song object. */
    public MutateResponse removeNote(int positionMs) {
        NoteNode removedNode = this.noteList.removeNote(positionMs);
        Note removedNote = removedNode.getNote();

        // Do standardization separately as it must happen in back -> front order.
        if (removedNode.getNext().isPresent()) {
            // Adjust envelope, preutterance, and length of next note.
            removedNode.getNext().get().standardize(standardizer, voicebank.get());
        }
        if (removedNode.getPrev().isPresent()) {
            // Adjust envelope, preutterance, and length of previous note.
            removedNode.getPrev().get().standardize(standardizer, voicebank.get());
        }

        Optional<NoteUpdateData> prevNote = Optional.absent();
        if (removedNode.getPrev().isPresent()) {
            Note prevSongNote = removedNode.getPrev().get().getNote();
            prevNote = Optional.of(
                    new NoteUpdateData(
                            positionMs - removedNote.getDelta(),
                            prevSongNote.getTrueLyric(),
                            prevSongNote.getEnvelope(),
                            prevSongNote.getPitchbends(),
                            prevSongNote.getConfigData()));
        }

        Optional<NoteUpdateData> nextNote = Optional.absent();
        if (removedNode.getNext().isPresent()) {
            // Modify the next note's portamento.
            Note nextSongNote = removedNode.getNext().get().getNote();
            int nextStart = positionMs + removedNote.getLength();
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
                    new NoteUpdateData(
                            nextStart,
                            nextSongNote.getTrueLyric(),
                            nextSongNote.getEnvelope(),
                            nextSongNote.getPitchbends(),
                            nextSongNote.getConfigData()));
        }

        // Remove this note's pitchbends.
        this.pitchbends
                .removePitchbends(positionMs, removedNote.getLength(), removedNote.getPitchbends());

        return new MutateResponse(
                // Returns note data of note that was removed.
                new NoteUpdateData(
                        positionMs,
                        removedNote.getTrueLyric(),
                        removedNote.getEnvelope(),
                        removedNote.getPitchbends(),
                        removedNote.getConfigData()),
                prevNote,
                nextNote);
    }

    /** Modifies a note in-place without changing its lyric, position, or duration. */
    public void modifyNote(NoteData toModify) {
        int positionMs = toModify.getPosition();
        NoteNode node = this.noteList.getNote(positionMs);
        Note note = node.getNote();
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
        NoteIterator iterator = noteList.iterator();
        int totalDelta = 0;
        while (iterator.hasNext()) {
            Note note = iterator.next();
            totalDelta += note.getDelta();
            notes.add(
                    new NoteData(
                            totalDelta,
                            note.getDuration(),
                            PitchUtils.noteNumToPitch(note.getNoteNum()),
                            note.getLyric(),
                            Optional.of(note.getTrueLyric()),
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
        return voicebank.getLocation();
    }

    public Voicebank getVoicebank() {
        return voicebank.get();
    }

    public NoteIterator getNoteIterator() {
        return noteList.iterator();
    }

    public NoteIterator getNoteIterator(RegionBounds bounds) {
        return noteList.boundedIterator(bounds);
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

    public int getNumNotes() {
        return noteList.getSize();
    }

    public String getPitchString(int firstPitchStep, int lastPitchStep, int noteNum) {
        return pitchbends.renderPitchbends(firstPitchStep, lastPitchStep, noteNum);
    }
}
