package com.utsusynth.utsu.view.config;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;

public abstract class PreferencesEditor {
    public abstract String getDisplayName();

    protected abstract void setDisplayNameInternal(String displayName);

    public void setDisplayName(String displayName) {
        BorderPane borderPane = getView();
        borderPane.setTop(new Label(displayName));
        setDisplayNameInternal(displayName);
    }

    public abstract BorderPane getView();

    protected abstract void setViewInternal(BorderPane view);

    protected abstract Node initializeInternal();

    /* Handles UI elements common to all preferences editors. */
    public void initialize() {
        BorderPane borderPane = new BorderPane();
        borderPane.setTop(new Label(getDisplayName()));
        borderPane.setCenter(initializeInternal());
        setViewInternal(borderPane);
    }

    public abstract void savePreferences();

    /* Changes UI to reflect the preferences file instead of local unsaved changes. */
    public abstract void revertToPreferences();
}
