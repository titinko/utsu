package com.utsusynth.utsu.view.config;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public abstract class PreferencesEditor {
    public abstract String getDisplayName();

    protected abstract void setDisplayNameInternal(String displayName);

    public void setDisplayName(String displayName) {
        BorderPane borderPane = getView();
        Label title = (Label) borderPane.getTop();
        title.setText(displayName);
        setDisplayNameInternal(displayName);
    }

    public abstract BorderPane getView();

    protected abstract void setViewInternal(BorderPane view);

    protected abstract Node initializeInternal();

    /* Handles UI elements common to all preferences editors. */
    public void initialize() {
        BorderPane borderPane = new BorderPane();
        Label title = new Label(getDisplayName());
        title.setFont(Font.font("Verdana", FontWeight.BOLD, 12));
        BorderPane.setMargin(title, new Insets(0, 0, 10, 0));
        BorderPane.setAlignment(title, Pos.TOP_LEFT);
        borderPane.setTop(title);
        borderPane.setCenter(initializeInternal());
        setViewInternal(borderPane);
    }

    /* Returns whether the close should proceed. */
    public abstract boolean onCloseEditor(Stage stage);

    public abstract void savePreferences();

    /* Changes UI to reflect the preferences file instead of local unsaved changes. */
    public abstract void revertToPreferences();
}
