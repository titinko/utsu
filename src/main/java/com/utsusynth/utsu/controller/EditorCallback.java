package com.utsusynth.utsu.controller;

import java.io.File;

public interface EditorCallback {
    void markChanged(boolean hasUnsavedChanges);

    void openVoicebank(File location);
}
