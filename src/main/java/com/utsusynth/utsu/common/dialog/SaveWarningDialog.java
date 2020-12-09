package com.utsusynth.utsu.common.dialog;

import java.text.MessageFormat;
import java.util.ResourceBundle;
import com.google.inject.Inject;
import com.utsusynth.utsu.common.i18n.Localizable;
import com.utsusynth.utsu.common.i18n.Localizer;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class SaveWarningDialog implements Localizable {
    public enum Decision {
        SAVE_AND_CLOSE, CLOSE_WITHOUT_SAVING, CANCEL
    }

    private final Localizer localizer;
    private final Label displayMessage;
    private final Button saveButton;
    private final Button closeWithoutSavingButton;
    private final Button cancelButton;

    private String fileName;
    private Decision decision;

    @Inject
    public SaveWarningDialog(Localizer localizer) {
        this.localizer = localizer;
        decision = Decision.CANCEL;

        // Initialize values that need to be localized.
        displayMessage = new Label();
        saveButton = new Button();
        closeWithoutSavingButton = new Button();
        cancelButton = new Button();
    }

    @Override
    public void localize(ResourceBundle bundle) {
        if (fileName != null) {
            displayMessage.setText(
                    MessageFormat.format(bundle.getString("dialog.saveWarning"), fileName));
        }
        saveButton.setText(bundle.getString("general.save"));
        closeWithoutSavingButton.setText(bundle.getString("dialog.closeWithoutSaving"));
        cancelButton.setText(bundle.getString("general.cancel"));
    }

    public Decision popup(Stage parent, String fileName) {
        this.fileName = fileName;
        localizer.localize(this);

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(parent);

        BorderPane dialogPane = new BorderPane(displayMessage);
        BorderPane.setMargin(displayMessage, new Insets(15));

        ButtonBar buttonBar = new ButtonBar();
        buttonBar.setPrefHeight(40);
        buttonBar.setPadding(new Insets(0, 5, 0, 5));
        ButtonBar.setButtonData(saveButton, ButtonData.APPLY);
        ButtonBar.setButtonData(cancelButton, ButtonData.CANCEL_CLOSE);
        ButtonBar.setButtonData(closeWithoutSavingButton, ButtonData.LEFT);
        buttonBar.getButtons().addAll(closeWithoutSavingButton, cancelButton, saveButton);
        dialogPane.setBottom(buttonBar);

        saveButton.setDefaultButton(true);
        saveButton.setOnAction(event -> {
            decision = Decision.SAVE_AND_CLOSE;
            dialog.close();
        });
        closeWithoutSavingButton.setOnAction(event -> {
            decision = Decision.CLOSE_WITHOUT_SAVING;
            dialog.close();
        });
        cancelButton.setCancelButton(true);
        cancelButton.setOnAction(event -> {
            decision = Decision.CANCEL;
            dialog.close();
        });

        Scene dialogScene = new Scene(dialogPane);
        dialog.setScene(dialogScene);
        dialog.showAndWait();

        return decision;
    }
}
