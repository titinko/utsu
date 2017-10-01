package com.utsusynth.utsu.model;

import java.util.Iterator;
import java.util.LinkedList;

import com.google.common.base.Optional;
import com.utsusynth.utsu.common.PitchUtils;
import com.utsusynth.utsu.common.QuantizedAddRequest;
import com.utsusynth.utsu.common.QuantizedAddResponse;
import com.utsusynth.utsu.common.QuantizedNeighbor;
import com.utsusynth.utsu.common.QuantizedNote;
import com.utsusynth.utsu.common.exception.NoteAlreadyExistsException;
import com.utsusynth.utsu.model.pitch.PitchCurve;
import com.utsusynth.utsu.model.voicebank.LyricConfig;
import com.utsusynth.utsu.model.voicebank.Voicebank;
import com.utsusynth.utsu.model.voicebank.VoicebankReader;

/**
 * In-code representation of a song. Compatible with UST versions 1.2 and 2.0.
 */
public class Song {
	private static final int DEFAULT_NOTE_DURATION = 480;
	private final VoicebankReader voicebankReader;

	// Settings. (Anything marked with [#SETTING])
	// Insert Time signatures here
	private double tempo;
	private String projectName;
	private String outputFile;
	private String voiceDirectory; // Pulled directly from the oto file.
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

		public Builder setOutputFile(String outputFile) {
			newSong.outputFile = outputFile;
			return this;
		}

		public Builder setVoiceDirectory(String voiceDirectory) {
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
			newSong.voicebank = newSong.voicebankReader.loadFromDirectory(newSong.voiceDirectory);
			newSong.noteList = noteListBuilder.build();
			return newSong;
		}
	}

	public Song(VoicebankReader voicebankReader, SongNoteList songNoteList, PitchCurve pitchbends) {
		this.voicebankReader = voicebankReader;
		this.noteList = songNoteList;
		this.pitchbends = pitchbends;
		this.voiceDirectory = "${DEFAULT}";
		this.voicebank = voicebankReader.loadFromDirectory(this.voiceDirectory);
		this.tempo = 125.0;
		this.projectName = "(no title)";
		this.flags = "";
	}

	public Builder toBuilder() {
		// Returns the builder of a new Song with this one's attributes.
		// The old Song's noteList and pitchbends objects are used in the new Song.
		return new Builder(new Song(this.voicebankReader, this.noteList, this.pitchbends))
				.setTempo(this.tempo)
				.setProjectName(this.projectName)
				.setOutputFile(this.outputFile)
				.setVoiceDirectory(this.voiceDirectory)
				.setFlags(this.flags)
				.setMode2(this.mode2);
	}

	/**
	 * Adds a note to the song object at the specified location
	 * 
	 * @param beat
	 * @param percentThroughBeat
	 * @param fullPitch
	 * @return the lyric of the new note.
	 * @throws NoteAlreadyExistsException
	 */
	public QuantizedAddResponse addNote(QuantizedAddRequest request)
			throws NoteAlreadyExistsException {
		QuantizedNote toAdd = request.getNote();
		SongNote note = new SongNote();
		// New note's delta/length may be overridden while inserting into note list.
		note.setDelta(toAdd.getStart() * DEFAULT_NOTE_DURATION / toAdd.getQuantization());
		note.safeSetDuration(toAdd.getDuration() * DEFAULT_NOTE_DURATION / toAdd.getQuantization());
		note.safeSetLength(toAdd.getDuration() * DEFAULT_NOTE_DURATION / toAdd.getQuantization());
		note.setLyric(request.getLyric());
		note.setNoteNum(PitchUtils.pitchToNoteNum(request.getPitch()));
		note.setNoteFlags("B0");

		int positionMs = toAdd.getStart() * DEFAULT_NOTE_DURATION / toAdd.getQuantization();
		SongNode insertedNode = this.noteList.insertNote(note, positionMs);

		// Find neighbors to newly added note.
		Optional<QuantizedNeighbor> prevNote = Optional.absent();
		if (insertedNode.getPrev().isPresent()) {
			int quantizedDelta = note.getDelta() / (DEFAULT_NOTE_DURATION / 32);
			prevNote = Optional.of(new QuantizedNeighbor(quantizedDelta, 32));
		}
		Optional<QuantizedNeighbor> nextNote = Optional.absent();
		if (insertedNode.getNext().isPresent()) {
			int quantizedLength = note.getLength() / (DEFAULT_NOTE_DURATION / 32);
			nextNote = Optional.of(new QuantizedNeighbor(quantizedLength, 32));

			// Modify the next note's portamento.
			SongNote nextSongNote = insertedNode.getNext().get().getNote();
			int nextStart = positionMs + note.getLength();
			pitchbends.removePitchbends(nextStart, nextSongNote.getPitchbends());
			pitchbends.addPitchbends(
					nextStart,
					nextSongNote.getPitchbends(),
					note.getNoteNum(),
					nextSongNote.getNoteNum());
		}

		Optional<LyricConfig> lyricConfig = voicebank.getLyricConfig(request.getLyric());
		Optional<String> trueLyric = lyricConfig.isPresent()
				? Optional.of(lyricConfig.get().getTrueLyric()) : Optional.absent();

		// Adjust the envelopes to match overlap.
		if (lyricConfig.isPresent()) {
			note.setFadeIn(Math.max(note.getFadeIn(), lyricConfig.get().getOverlap()));
			// Case where there is an adjacent previous node.
			if (insertedNode.getPrev().isPresent()
					&& insertedNode.getPrev().get().getNote().getDuration() == note.getDelta()) {
				insertedNode.getPrev().get().getNote().setFadeOut(note.getFadeIn());
			}
			// Case where there is an adjacent next node.
			if (insertedNode.getNext().isPresent() && note.getDuration() == note.getLength()) {
				note.setFadeOut(insertedNode.getNext().get().getNote().getFadeIn());
			}
		}

		// Add this note's pitchbends.
		int prevNoteNum = insertedNode.getPrev().isPresent()
				? insertedNode.getPrev().get().getNote().getNoteNum() : note.getNoteNum();
		this.pitchbends
				.addPitchbends(positionMs, note.getPitchbends(), prevNoteNum, note.getNoteNum());

		return new QuantizedAddResponse(trueLyric, prevNote, nextNote);
	}

	public QuantizedAddResponse removeNote(QuantizedNote note) {
		int positionMs = note.getStart() * DEFAULT_NOTE_DURATION / note.getQuantization();
		SongNode removedNode = this.noteList.removeNote(positionMs);
		Optional<QuantizedNeighbor> prevNote = Optional.absent();
		if (removedNode.getPrev().isPresent()) {
			int quantizedDelta = removedNode.getNote().getDelta() / (DEFAULT_NOTE_DURATION / 32);
			prevNote = Optional.of(new QuantizedNeighbor(quantizedDelta, 32));

			// Adjust envelope of adjacent previous node, if present.
			if (removedNode.getPrev().get().getNote().getDuration() == removedNode
					.getNote()
					.getDelta()) {
				removedNode.getPrev().get().getNote().setFadeOut(35); // Default fade out.
			}
		}
		Optional<QuantizedNeighbor> nextNote = Optional.absent();
		if (removedNode.getNext().isPresent()) {
			int quantizedLength = removedNode.getNote().getLength() / (DEFAULT_NOTE_DURATION / 32);
			nextNote = Optional.of(new QuantizedNeighbor(quantizedLength, 32));

			// Modify the next note's portamento.
			SongNote nextSongNote = removedNode.getNext().get().getNote();
			int nextStart = positionMs + removedNode.getNote().getLength();
			int prevNoteNum = removedNode.getPrev().isPresent()
					? removedNode.getPrev().get().getNote().getNoteNum()
					: nextSongNote.getNoteNum();
			pitchbends.removePitchbends(nextStart, nextSongNote.getPitchbends());
			pitchbends.addPitchbends(
					nextStart,
					nextSongNote.getPitchbends(),
					prevNoteNum,
					nextSongNote.getNoteNum());
		}

		// Remove this note's pitchbends.
		this.pitchbends.removePitchbends(positionMs, removedNode.getNote().getPitchbends());

		return new QuantizedAddResponse(Optional.absent(), prevNote, nextNote);
	}

	public LinkedList<QuantizedAddRequest> getQuantizedNotes() {
		LinkedList<QuantizedAddRequest> notes = new LinkedList<>();
		Iterator<SongNote> iterator = noteList.iterator();
		int totalQuantizedDelta = 0;
		while (iterator.hasNext()) {
			SongNote note = iterator.next();
			totalQuantizedDelta += note.getDelta() / (DEFAULT_NOTE_DURATION / 32);
			int quantizedDuration = note.getDuration() / (DEFAULT_NOTE_DURATION / 32);
			Optional<LyricConfig> lyricConfig = voicebank.getLyricConfig(note.getLyric());
			Optional<String> trueLyric = lyricConfig.isPresent()
					? Optional.of(lyricConfig.get().getTrueLyric()) : Optional.absent();
			notes.add(
					new QuantizedAddRequest(
							new QuantizedNote(totalQuantizedDelta, quantizedDuration, 32),
							PitchUtils.noteNumToPitch(note.getNoteNum()),
							note.getLyric(),
							trueLyric));
		}
		return notes;
	}

	public String getProjectName() {
		return projectName;
	}

	public String getOutputFile() {
		return outputFile;
	}

	public String getVoiceDir() {
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
