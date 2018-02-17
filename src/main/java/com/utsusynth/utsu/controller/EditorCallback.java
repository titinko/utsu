package com.utsusynth.utsu.controller;

public interface EditorCallback {
    void markChanged();

    void enableSave(boolean enabled);
}
