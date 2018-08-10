package com.utsusynth.utsu.controller;

import java.io.File;
import com.google.common.base.Optional;

/** Used to signify a controller of some sort of file editor. */
interface EditorController {
    /** Initializes an editor with a callback to the menu. */
    void openEditor(EditorCallback callback);

    /** Closes an editor and deletes stored data. */
    void closeEditor();

    void refreshView();

    String getFileName();

    boolean hasPermanentLocation();

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
