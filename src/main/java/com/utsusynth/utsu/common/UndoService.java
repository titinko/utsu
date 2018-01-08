package com.utsusynth.utsu.common;

import com.google.inject.Singleton;

/** A singleton that keeps track of the most recent action and how to undo it. */
@Singleton
public class UndoService {

  private Runnable mostRecentAction;
  private Runnable undoMostRecentAction;

  /** True if the next {@link #undo()} would redo (undo an undo). */
  private boolean isRedo = false;

  /** Undo the most recent action. */
  private void undo() {
    undoMostRecentAction.run();

    // Swap the redo and undo.
    Runnable swap = mostRecentAction;
    mostRecentAction = undoMostRecentAction;
    undoMostRecentAction = swap;
    isRedo = !isRedo;
  }

  /**
   * Specifies the most recent action and how to undo it.
   *
   * <p>On undo, will call {@code undoMostRecentAction}. If undo is called twice in a row, it will
   * redo the action by calling {@code mostRecentAction}.
   */
  private void setMostRecentAction(Runnable redoMostRecentAction, Runnable undoMostRecentAction) {
    this.mostRecentAction = redoMostRecentAction;
    this.undoMostRecentAction = undoMostRecentAction;
    isRedo = false;
  }

  /** True if the next {@link #undo()} would redo (i.e. undo an undo). */
  private boolean wouldRedo() {
    return isRedo;
  }
}
