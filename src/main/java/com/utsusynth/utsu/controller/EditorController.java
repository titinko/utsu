package com.utsusynth.utsu.controller;

/** Used to signify a controller of some sort of file editor. */
interface EditorController {
    /** Initializes an editor with a callback to the menu. */
    void openEditor(EditorCallback callback);

    void refreshView();

    String openFile();

    String saveFile();

    String saveFileAs();

    void openProperties();
}
