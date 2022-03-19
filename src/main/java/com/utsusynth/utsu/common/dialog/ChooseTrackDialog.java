package com.utsusynth.utsu.common.dialog;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.utsusynth.utsu.common.i18n.Localizable;
import com.utsusynth.utsu.common.i18n.Localizer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.ResourceBundle;

/** This dialog will show when opening a file with multiple tracks. */
public class ChooseTrackDialog implements Localizable {
    private final Localizer localizer;

    private final Label chooseTrackLabel;
    private final CheckBox allTracksCheckBox;
    private final Button openButton;
    private final Button cancelButton;

    // Which track to open. If set to -1, no tracks will be open.
    private ImmutableList<Integer> tracksToOpen;

    @Inject
    public ChooseTrackDialog(Localizer localizer) {
        this.localizer = localizer;
        tracksToOpen = ImmutableList.of();

        // Initialize values that need to be localized.
        chooseTrackLabel = new Label("Choose Track:");
        allTracksCheckBox = new CheckBox("Open all in tabs");
        openButton = new Button();
        cancelButton = new Button();
    }

    @Override
    public void localize(ResourceBundle bundle) {
        chooseTrackLabel.setText(bundle.getString("dialog.chooseTrack"));
        allTracksCheckBox.setText(bundle.getString("dialog.openAllTracks"));
        openButton.setText(bundle.getString("general.open"));
        cancelButton.setText(bundle.getString("general.cancel"));
    }

    public ImmutableList<Integer> popup(Stage parent, int numTracks) {
        if (numTracks <= 1) {
            System.out.println("Warning: Choose track popup called when # of tracks <= 1.");
            return ImmutableList.of(); // Case where incorrect number of tracks is passed in.
        }

        localizer.localize(this);
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(parent);

        ChoiceBox<Integer> trackChoiceBox = new ChoiceBox<>();
        trackChoiceBox.setPrefWidth(150);
        for (int i = 1; i <= numTracks; i++) {
            trackChoiceBox.getItems().add(i);
        }
        trackChoiceBox.setValue(1);
        HBox chooseTrackHBox = new HBox(10);
        chooseTrackHBox.getChildren().addAll(chooseTrackLabel, trackChoiceBox);
        chooseTrackLabel.setAlignment(Pos.CENTER_LEFT);

        allTracksCheckBox.setSelected(false);
        allTracksCheckBox.setAlignment(Pos.CENTER_LEFT);
        trackChoiceBox.disableProperty().bind(allTracksCheckBox.selectedProperty());

        VBox vBox = new VBox(10);
        vBox.getChildren().addAll(chooseTrackHBox, allTracksCheckBox);
        BorderPane dialogPane = new BorderPane(vBox);
        BorderPane.setMargin(vBox, new Insets(10));

        ButtonBar buttonBar = new ButtonBar();
        buttonBar.setPrefHeight(40);
        buttonBar.setPadding(new Insets(0, 5, 0, 5));
        ButtonBar.setButtonData(openButton, ButtonData.APPLY);
        ButtonBar.setButtonData(cancelButton, ButtonData.CANCEL_CLOSE);
        buttonBar.getButtons().addAll(cancelButton, openButton);
        dialogPane.setBottom(buttonBar);

        openButton.setDefaultButton(true);
        openButton.setOnAction(event -> {
            if (allTracksCheckBox.isSelected()) {
                tracksToOpen = ImmutableList.copyOf(trackChoiceBox.getItems());
            } else {
                tracksToOpen = ImmutableList.of(trackChoiceBox.getValue());
            }
            dialog.close();
        });
        cancelButton.setCancelButton(true);
        cancelButton.setOnAction(event -> {
            tracksToOpen = ImmutableList.of();
            dialog.close();
        });

        Scene dialogScene = new Scene(dialogPane);
        dialog.setScene(dialogScene);
        dialog.showAndWait();

        return tracksToOpen;
    }
}
