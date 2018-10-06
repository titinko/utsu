package com.utsusynth.utsu.controller.common;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

/** A class that keeps track of which menu items are enabled/disabled for each tab. */
public class MenuItemManager {
    private final BooleanProperty saveEnabled;
    private final BooleanProperty saveAsEnabled;
    private final BooleanProperty exportWavEnabled;
    private final BooleanProperty undoEnabled;
    private final BooleanProperty redoEnabled;
    private final BooleanProperty cutEnabled;
    private final BooleanProperty copyEnabled;
    private final BooleanProperty pasteEnabled;
    private final BooleanProperty deleteEnabled;
    private final BooleanProperty propertiesEnabled;
    private final BooleanProperty notePropertiesEnabled;

    public MenuItemManager() {
        saveEnabled = new SimpleBooleanProperty(false);
        saveAsEnabled = new SimpleBooleanProperty(false);
        exportWavEnabled = new SimpleBooleanProperty(false);
        undoEnabled = new SimpleBooleanProperty(false);
        redoEnabled = new SimpleBooleanProperty(false);
        cutEnabled = new SimpleBooleanProperty(false);
        copyEnabled = new SimpleBooleanProperty(false);
        pasteEnabled = new SimpleBooleanProperty(false);
        deleteEnabled = new SimpleBooleanProperty(false);
        propertiesEnabled = new SimpleBooleanProperty(false);
        notePropertiesEnabled = new SimpleBooleanProperty(false);
    }

    /** Initialize with default song settings. */
    public void initializeSong(
            BooleanProperty canUndo,
            BooleanProperty canRedo,
            BooleanProperty somethingIsHighlighted,
            BooleanProperty clipboardFilled) {
        saveEnabled.set(false);
        saveAsEnabled.set(true);
        exportWavEnabled.set(true);
        undoEnabled.bind(canUndo);
        redoEnabled.bind(canRedo);
        cutEnabled.bind(somethingIsHighlighted);
        copyEnabled.bind(somethingIsHighlighted);
        pasteEnabled.bind(clipboardFilled);
        deleteEnabled.bind(somethingIsHighlighted);
        propertiesEnabled.set(true);
        notePropertiesEnabled.bind(somethingIsHighlighted);
    }

    /** Initialize with default voicebank settings. */
    public void initializeVoicebank(BooleanProperty canUndo, BooleanProperty canRedo) {
        saveEnabled.set(false);
        saveAsEnabled.set(false);
        exportWavEnabled.set(false);
        undoEnabled.bind(canUndo);
        redoEnabled.bind(canRedo);
        cutEnabled.set(false);
        copyEnabled.set(false);
        pasteEnabled.set(false);
        deleteEnabled.set(false);
        propertiesEnabled.set(false);
        notePropertiesEnabled.set(false);
    }

    public void bindProperties(
            BooleanProperty saveDisabled,
            BooleanProperty saveAsDisabled,
            BooleanProperty exportWavDisabled,
            BooleanProperty undoDisabled,
            BooleanProperty redoDisabled,
            BooleanProperty cutDisabled,
            BooleanProperty copyDisabled,
            BooleanProperty pasteDisabled,
            BooleanProperty deleteDisabled,
            BooleanProperty propertiesDisabled,
            BooleanProperty notePropertiesDisabled) {
        saveDisabled.unbind();
        saveDisabled.bind(saveEnabled.not());
        saveAsDisabled.unbind();
        saveAsDisabled.bind(saveAsEnabled.not());
        exportWavDisabled.unbind();
        exportWavDisabled.bind(exportWavEnabled.not());
        undoDisabled.unbind();
        undoDisabled.bind(undoEnabled.not());
        redoDisabled.unbind();
        redoDisabled.bind(redoEnabled.not());
        cutDisabled.unbind();
        cutDisabled.bind(cutEnabled.not());
        copyDisabled.unbind();
        copyDisabled.bind(copyEnabled.not());
        pasteDisabled.unbind();
        pasteDisabled.bind(pasteEnabled.not());
        deleteDisabled.unbind();
        deleteDisabled.bind(deleteEnabled.not());
        propertiesDisabled.unbind();
        propertiesDisabled.bind(propertiesEnabled.not());
        notePropertiesDisabled.unbind();
        notePropertiesDisabled.bind(notePropertiesEnabled.not());
    }

    public void enableSave() {
        saveEnabled.set(true);
    }

    public void disableSave() {
        saveEnabled.set(false);
    }
}
