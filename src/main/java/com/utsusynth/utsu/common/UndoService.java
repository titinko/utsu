package com.utsusynth.utsu.common;

import java.util.Stack;
import com.google.common.base.Optional;

/** A class that keeps track of the most recent actions and how to undo them. */
public class UndoService {
    private static final int MAX_STACK_SIZE = 20;

    private Stack<Runnable> redoActions;
    private Stack<Runnable> undoActions;
    private Optional<Runnable> nextRedoAction;

    public UndoService() {
        redoActions = new Stack<Runnable>();
        undoActions = new Stack<Runnable>();
        nextRedoAction = Optional.absent();
    }

    /** Undo the most recent action. */
    public void undo() {
        undoActions.pop().run();
        nextRedoAction = Optional.of(redoActions.pop());
    }

    /** Redo the most recent undo action. */
    public void redo() {
        if (nextRedoAction.isPresent()) {
            nextRedoAction.get().run();
            nextRedoAction = Optional.absent();
        }
    }

    /**
     * Specifies the most recent action and how to undo it.
     */
    public void setMostRecentAction(Runnable mostRecentAction, Runnable undoMostRecentAction) {
        if (undoActions.size() >= MAX_STACK_SIZE) {
            // Removes old actions when stack size becomes too large.
            redoActions.remove(0);
            undoActions.remove(0);
        }
        redoActions.push(mostRecentAction);
        undoActions.push(undoMostRecentAction);
    }

    /**
     * Clears all memory of actions and how to undo/redo them.
     */
    public void clearActions() {
        redoActions.clear();
        undoActions.clear();
        nextRedoAction = Optional.absent();
    }

    /** Returns true if any undo actions are in memory, false otherwise. */
    public boolean detectChanges() {
        return !undoActions.isEmpty();
    }
}
