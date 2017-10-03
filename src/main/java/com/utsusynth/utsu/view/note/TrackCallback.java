package com.utsusynth.utsu.view.note;

import com.google.common.base.Optional;
import com.utsusynth.utsu.UtsuController.Mode;
import com.utsusynth.utsu.common.exception.NoteAlreadyExistsException;
import com.utsusynth.utsu.common.quantize.QuantizedNote;

/**
 * A way of communicating TrackNote information back to its parent Track.
 */
public interface TrackCallback {
	void setHighlighted(TrackNote note, boolean highlighted);

	boolean isHighlighted(TrackNote note);

	boolean isInBounds(int rowNum);

	Optional<String> addSongNote(TrackNote note, QuantizedNote toAdd, int rowNum, String lyric)
			throws NoteAlreadyExistsException;

	void removeSongNote(QuantizedNote toRemove);

	void removeTrackNote(TrackNote trackNote);

	Mode getCurrentMode();
}
