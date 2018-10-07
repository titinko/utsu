package com.utsusynth.utsu.controller.common;

import java.util.LinkedList;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

/** A class that keeps track of the most recent actions and how to undo them. */
public class UndoService {
    private static final int MAX_STACK_SIZE = 20;

    // Keeps track of whether there are any tasks that can be undone/redone.
    private BooleanProperty canUndo;
    private BooleanProperty canRedo;

    private LinkedList<Runnable> prevRedoActions;
    private LinkedList<Runnable> prevUndoActions;
    private LinkedList<Runnable> nextRedoActions;
    private LinkedList<Runnable> nextUndoActions;

    public UndoService() {
        canUndo = new SimpleBooleanProperty(false);
        canRedo = new SimpleBooleanProperty(false);
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
        canUndo.set(!prevRedoActions.isEmpty() && !prevUndoActions.isEmpty());
        canRedo.set(!nextRedoActions.isEmpty() && !nextUndoActions.isEmpty());
    }

    /** Redo the most recent undo action. */
    public void redo() {
        if (!nextRedoActions.isEmpty() && !nextUndoActions.isEmpty()) {
            nextRedoActions.getLast().run();
            prevRedoActions.addLast(nextRedoActions.pollLast());
            prevUndoActions.addLast(nextUndoActions.pollLast());
        }
        canUndo.set(!prevRedoActions.isEmpty() && !prevUndoActions.isEmpty());
        canRedo.set(!nextRedoActions.isEmpty() && !nextUndoActions.isEmpty());
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
        canUndo.set(true);
        // Clear list of actions to be redone.
        nextRedoActions.clear();
        nextUndoActions.clear();
        canRedo.set(false);
    }

    /**
     * Clears all memory of actions and how to undo/redo them.
     */
    public void clearActions() {
        canUndo.set(false);
        canRedo.set(false);
        prevRedoActions.clear();
        prevUndoActions.clear();
        nextRedoActions.clear();
        nextUndoActions.clear();
    }

    /** Returns property for whether there are any actions to undo. */
    public BooleanProperty canUndoProperty() {
        return canUndo;
    }

    /** Returns property for whether there are any actions to redo. */
    public BooleanProperty canRedoProperty() {
        return canRedo;
    }
}
