package com.utsusynth.utsu.common;

import java.util.LinkedList;

/** A class that keeps track of the most recent actions and how to undo them. */
public class UndoService {
    private static final int MAX_STACK_SIZE = 20;

    private LinkedList<Runnable> prevRedoActions;
    private LinkedList<Runnable> prevUndoActions;
    private LinkedList<Runnable> nextRedoActions;
    private LinkedList<Runnable> nextUndoActions;

    public UndoService() {
        prevRedoActions = new LinkedList<>();
        prevUndoActions = new LinkedList<>();
        nextRedoActions = new LinkedList<>();
        nextUndoActions = new LinkedList<>();
    }

    /** Undo the most recent action. */
    public void undo() {
        if (!prevRedoActions.isEmpty() && !prevUndoActions.isEmpty()) {
            prevUndoActions.getLast().run();
            nextRedoActions.addLast(prevRedoActions.pollLast());
            nextUndoActions.addLast(prevUndoActions.pollLast());
        }
    }

    /** Redo the most recent undo action. */
    public void redo() {
        if (!nextRedoActions.isEmpty() && !nextUndoActions.isEmpty()) {
            nextRedoActions.getLast().run();
            prevRedoActions.addLast(nextRedoActions.pollLast());
            prevUndoActions.addLast(nextUndoActions.pollLast());
        }
    }

    /**
     * Specifies the most recent action and how to undo it.
     */
    public void setMostRecentAction(Runnable mostRecentAction, Runnable undoMostRecentAction) {
        if (prevUndoActions.size() >= MAX_STACK_SIZE) {
            // Removes old actions when stack size becomes too large.
            prevRedoActions.removeFirst();
            prevUndoActions.removeFirst();
        }
        prevRedoActions.addLast(mostRecentAction);
        prevUndoActions.addLast(undoMostRecentAction);
        // Clear list of actions to be redone.
        nextRedoActions.clear();
        nextUndoActions.clear();
    }

    /**
     * Clears all memory of actions and how to undo/redo them.
     */
    public void clearActions() {
        prevRedoActions.clear();
        prevUndoActions.clear();
        nextRedoActions.clear();
        nextUndoActions.clear();
    }
}
