package com.utsusynth.utsu.common.exception;

/**
 * This exception is thrown when the user attempts to add two notes to the same exact location in a
 * single track of a song.
 */
public class NoteAlreadyExistsException extends Exception {
    private static final long serialVersionUID = 1L;
}
