package com.utsusynth.utsu.common;

import com.google.inject.Inject;
import com.utsusynth.utsu.common.i18n.Localizable;
import com.utsusynth.utsu.common.i18n.Localizer;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.StringProperty;

import java.text.MessageFormat;
import java.util.ResourceBundle;

/** Singleton object representing the contents of a status bar. */
public class StatusBar implements Localizable {
    private final Localizer localizer;

    private StringProperty statusText;
    private DoubleProperty curProgress; // A value from 0 to 1, inclusive.

    private String key = ""; // Key of current status message, if present.
    private String param = ""; // Param of current status message, if present.

    @Inject
    public StatusBar(Localizer localizer) {
        this.localizer = localizer;
    }

    public void initialize(StringProperty statusText, DoubleProperty progress) {
        this.statusText = statusText;
        this.curProgress = progress;
        localizer.localize(this);
    }

    @Override
    public void localize(ResourceBundle bundle) {
        if (key.isEmpty() || !bundle.containsKey(key)) {
            return;
        }
        if (param.isEmpty()) {
            statusText.set(bundle.getString(key));
        } else {
            statusText.set(MessageFormat.format(localizer.getMessage(key), param));
        }
    }

    public void setStatus(String key) {
        // Equivalent to setting a status key with no param.
        setStatus(key, "");
    }

    public void setStatus(String key, String param) {
        this.key = key;
        this.param = param;
        if (param.isEmpty()) {
            statusText.set(localizer.getMessage(key));
        } else {
            statusText.set(MessageFormat.format(localizer.getMessage(key), param));
        }
    }

    public void setStatusAsync(String key, String param) {
        Platform.runLater(() -> setStatus(key, param));
    }

    public void setText(String text) {
        if (statusText != null) {
            key = "";
            param = "";
            statusText.set(text);
        }
    }

    /** Should be called from backend threads. */
    public void setTextAsync(String text) {
        Platform.runLater(() -> setText(text));
    }

    public void setProgress(double progress) {
        if (curProgress != null) {
            curProgress.set(progress);
        }
    }

    public void setProgressAsync(double progress) {
        Platform.runLater(() -> setProgress(progress));
    }
}
