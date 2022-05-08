package com.utsusynth.utsu.controller;

import com.utsusynth.utsu.common.exception.FileAlreadyOpenException;
import com.utsusynth.utsu.controller.common.MenuItemManager;
import com.utsusynth.utsu.controller.song.BulkEditorController.BulkEditorType;
import com.utsusynth.utsu.controller.song.LyricEditorController.LyricEditorType;
import javafx.scene.input.KeyEvent;

import java.io.File;
import java.util.Optional;

/**
 * Used to signify a controller of some sort of file editor.
 */
public interface EditorController {
    /**
     * Initializes an editor with a callback to the menu.
     */
    void openEditor(EditorCallback callback);

    /**
     * Closes an editor and deletes stored data.
     */
    void closeEditor();

    void refreshView();

    File getOpenFile();

    MenuItemManager getMenuItems();

    /**
     * Called whenever a key is pressed, excluding text input. Can override default key press
     * behaviors. Accelerators should be used instead when overrides are not needed.
     *
     * @return true if an override behavior for this key was found, false otherwise
     */
    boolean onKeyPressed(KeyEvent keyEvent);

    /**
     * Called whenever the current theme changes. Useful for when a theme's color is used outside
     * of CSS which is handled automatically.
     */
    void onThemeChanged();

    /**
     * Opens a file/directory in this tab
     *
     * @param fileType Allowed file types for te open file dialog.
     * @return What to name the tab, or nothing if open was canceled.
     * @throws FileAlreadyOpenException if file is open in another tab.
     */
    Optional<String> open(String... fileType) throws FileAlreadyOpenException;

    /**
     * Opens the specified file/directory in this tab.
     *
     * @throws FileAlreadyOpenException if file is open in another tab.
     */
    void open(File file) throws FileAlreadyOpenException;

    /**
     * Saves a tab to a pre-existing file/directory.
     *
     * @return What to name to tab, only set if file name has changed.
     */
    Optional<String> save();

    /**
     * Saves a tab to a user-specified file/directory.
     *
     * @return What to name the tab, only set if file name has changed.
     */
    Optional<String> saveAs();

    void exportToWav();

    void undo();

    void redo();

    void cut();

    void copy();

    void paste();

    void delete();

    void selectAll();

    void openProperties();

    /**
     * Song only for now. Open bulk editor.
     */
    void openBulkEditor(BulkEditorType editorType);

    /**
     * Song only for now. Open lyric editor.
     */
    void openLyricEditor(LyricEditorType editorType);

    /**
     * Song only. Opens config for a note.
     */
    void openNoteProperties();

    /**
     * Voicebank only. Highlights config for a lyric.
     */
    void showLyricConfig(String trueLyric);

    /**
     * Opens a legacy UTAU plugin. Returns the file of the plugin opened, if any.
     */
    Optional<File> openPlugin();

    /**
     * Calls a legacy UTAU plugin.
     */
    void invokePlugin(File plugin);
}
