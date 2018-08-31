package com.utsusynth.utsu.common;

import javafx.beans.property.StringProperty;

/** Singleton object representing the contents of a status bar. */
public class StatusBar {
    private StringProperty statusText;

    public void initialize(StringProperty statusText) {
        this.statusText = statusText;
    }

    public void setStatus(String status) {
        if (statusText != null) {
            statusText.set(status);
        }
    }
}
