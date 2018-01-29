package com.utsusynth.utsu.controller;

/** Used to signify a controller of some sort of file editor. */
interface EditorController {
    /** Initializes an editor with a callback to the menu. */
    void openEditor(EditorCallback callback);

    /** Closes an editor and deletes stored data. */
    // TODO: implement closeEditor

    void refreshView();

    String open();

    String save();

    String saveAs();

    void openProperties();
}
