package com.utsusynth.utsu.controller.common;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

/** A class that keeps track of which menu items are enabled/disabled for each tab. */
public class MenuItemManager {
    private final BooleanProperty saveEnabled;
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
    public void initializeSong() {
        saveEnabled.set(false);
        undoEnabled.set(false);
        redoEnabled.set(false);
        cutEnabled.set(false);
        copyEnabled.set(false);
        pasteEnabled.set(false); // ???
        deleteEnabled.set(false);
        propertiesEnabled.set(true);
        notePropertiesEnabled.set(false);
    }

    /** Initialize with default voicebank settings. */
    public void initializeVoicebank() {
        saveEnabled.set(false);
        undoEnabled.set(false);
        redoEnabled.set(false);
        cutEnabled.set(false);
        copyEnabled.set(false);
        pasteEnabled.set(false); // ???
        deleteEnabled.set(false);
        propertiesEnabled.set(false);
        notePropertiesEnabled.set(false);
    }
}
