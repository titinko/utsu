package com.utsusynth.utsu.common;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.StringProperty;

/** Singleton object representing the contents of a status bar. */
public class StatusBar {
    private StringProperty statusText;
    private DoubleProperty curProgress; // A value from 0 to 1, inclusive.

    public void initialize(StringProperty statusText, DoubleProperty progress) {
        this.statusText = statusText;
    }

    public void setStatus(String status) {
        if (statusText != null) {
            statusText.set(status);
        }
    }

    public void setProgress(double progress) {
        if (curProgress != null) {
            curProgress.set(progress);
        }
    }
}
