package com.utsusynth.utsu.common.dialog;

import java.text.MessageFormat;
import java.util.ResourceBundle;
import com.google.inject.Inject;
import com.utsusynth.utsu.common.i18n.Localizable;
import com.utsusynth.utsu.common.i18n.Localizer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
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
        saveButton.setDefaultButton(true);
        closeWithoutSavingButton = new Button();
        cancelButton = new Button();
        cancelButton.setCancelButton(true);
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

        VBox dialogVBox = new VBox(18);
        dialogVBox.setPadding(new Insets(15));

        HBox dialogHBox = new HBox(10);
        dialogHBox.setAlignment(Pos.CENTER_RIGHT);

        dialogVBox.getChildren().addAll(displayMessage, dialogHBox);
        dialogHBox.getChildren().add(closeWithoutSavingButton);
        HBox.setMargin(closeWithoutSavingButton, new Insets(0, 15, 0, 0));
        dialogHBox.getChildren().add(cancelButton);
        dialogHBox.getChildren().add(saveButton);

        saveButton.setOnAction(event -> {
            decision = Decision.SAVE_AND_CLOSE;
            dialog.close();
        });
        closeWithoutSavingButton.setOnAction(event -> {
            decision = Decision.CLOSE_WITHOUT_SAVING;
            dialog.close();
        });
        cancelButton.setOnAction(event -> {
            decision = Decision.CANCEL;
            dialog.close();
        });

        Scene dialogScene = new Scene(dialogVBox);
        dialog.setScene(dialogScene);
        dialog.showAndWait();

        return decision;
    }
}
