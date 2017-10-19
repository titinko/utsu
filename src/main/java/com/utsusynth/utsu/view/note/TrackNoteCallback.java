package com.utsusynth.utsu.view.note;

import com.google.common.base.Optional;
import com.utsusynth.utsu.UtsuController.Mode;
import com.utsusynth.utsu.common.exception.NoteAlreadyExistsException;
import com.utsusynth.utsu.common.quantize.QuantizedEnvelope;
import com.utsusynth.utsu.common.quantize.QuantizedNote;
import com.utsusynth.utsu.common.quantize.QuantizedPitchbend;
import com.utsusynth.utsu.common.quantize.QuantizedPortamento;

/**
 * A way of communicating TrackNote information back to its parent Track.
 */
public interface TrackNoteCallback {
	void setHighlighted(TrackNote note, boolean highlighted);

	boolean isHighlighted(TrackNote note);

	boolean isInBounds(int rowNum);

	Optional<String> addSongNote(
			TrackNote note,
			QuantizedNote toAdd,
			Optional<QuantizedEnvelope> envelope,
			Optional<QuantizedPitchbend> pitchbend,
			int rowNum,
			String lyric) throws NoteAlreadyExistsException;

	void removeSongNote(QuantizedNote toRemove);

	void modifySongVibrato(QuantizedNote toModify);

	void removeTrackNote(TrackNote trackNote);

	Optional<QuantizedEnvelope> getEnvelope(QuantizedNote note);

	Optional<QuantizedPortamento> getPortamento(QuantizedNote note);

	Mode getCurrentMode();
}
