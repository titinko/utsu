package com.utsusynth.utsu.common.dialog;

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

import java.text.MessageFormat;
import java.util.ResourceBundle;

public class DeleteWarningDialog implements Localizable {
    public enum Decision {
        DELETE, CANCEL
    }

    private final Localizer localizer;
    private final Label displayMessage;
    private final Button deleteButton;
    private final Button cancelButton;

    private String fileName;
    private Decision decision;

    @Inject
    public DeleteWarningDialog(Localizer localizer) {
        this.localizer = localizer;
        decision = Decision.CANCEL;

        // Initialize values that need to be localized.
        displayMessage = new Label();
        deleteButton = new Button();
        deleteButton.setDefaultButton(true);
        cancelButton = new Button();
        cancelButton.setCancelButton(true);
    }

    @Override
    public void localize(ResourceBundle bundle) {
        if (fileName != null) {
            displayMessage.setText(
                    MessageFormat.format(bundle.getString("dialog.deleteWarning"), fileName));
        }
        deleteButton.setText(bundle.getString("menu.edit.delete"));
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
        dialogHBox.getChildren().add(cancelButton);
        dialogHBox.getChildren().add(deleteButton);

        deleteButton.setOnAction(event -> {
            decision = Decision.DELETE;
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
