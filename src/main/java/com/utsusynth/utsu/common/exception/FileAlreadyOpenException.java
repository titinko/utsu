package com.utsusynth.utsu.common.exception;

/**
 * This exception is thrown when user attempts to load/save to a file that is already open on
 * another tab.
 */
public class FileAlreadyOpenException extends Exception {
    private static final long serialVersionUID = 2L;
}
