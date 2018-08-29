package com.utsusynth.utsu.controller;

import java.io.File;
import com.google.common.base.Optional;
import javafx.scene.input.KeyEvent;

/** Used to signify a controller of some sort of file editor. */
interface EditorController {
    /** Initializes an editor with a callback to the menu. */
    void openEditor(EditorCallback callback);

    /** Closes an editor and deletes stored data. */
    void closeEditor();

    void refreshView();

    String getFileName();

    boolean hasPermanentLocation();

    /**
     * Called whenever a key is pressed, excluding text input. Can override default key press
     * behaviors. Accelerators should be used instead when overrides are not needed.
     * 
     * @return true if an override behavior for this key was found, false otherwise
     */
    boolean onKeyPressed(KeyEvent keyEvent);

    void open();

    void save();

    void saveAs();

    void selectAll();

    void openProperties();

    /** Opens a legacy UTAU plugin. Returns the file of the plugin opened, if any. */
    Optional<File> openPlugin();

    /** Calls a legacy UTAU plugin. */
    void invokePlugin(File plugin);
}
