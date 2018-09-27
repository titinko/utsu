package com.utsusynth.utsu.view.song.note;

/**
 * A way of communicating TrackNote information back to its parent Track.
 */
public interface NoteCallback {
    void highlightExclusive(Note note);

    void highlightInclusive(Note note);

    void realignHighlights();

    boolean isExclusivelyHighlighted(Note note);

    /** Only catches current note. */
    void updateNote(Note note);

    /** Catches all highlighted notes, if applicable. */
    void moveNote(Note note, int positionDelta, int rowDelta);

    /** Catches all highlighted notes, if applicable. */
    void recordNoteMovement(Note note, int positionDelta, int rowDelta);

    /** Catches all highlighted notes, if applicable. */
    void deleteNote(Note note);

    boolean hasVibrato(int position);

    void setHasVibrato(int position, boolean hasVibrato);

    void openNoteProperties(Note note);
}
