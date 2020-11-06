package com.utsusynth.utsu.view.config;

import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;

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

    protected abstract Pane initializeInternal();

    /* Handles UI elements common to all preferences editors. */
    public void initialize() {
        BorderPane borderPane = new BorderPane();
        borderPane.setTop(new Label(getDisplayName()));
        borderPane.setCenter(initializeInternal());
        setViewInternal(borderPane);
    }
}
