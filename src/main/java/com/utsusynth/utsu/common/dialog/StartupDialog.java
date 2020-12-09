package com.utsusynth.utsu.common.dialog;

import com.google.inject.Inject;
import com.utsusynth.utsu.common.i18n.Localizable;
import com.utsusynth.utsu.common.i18n.Localizer;
import com.utsusynth.utsu.common.i18n.NativeLocale;
import com.utsusynth.utsu.files.PreferencesManager;
import com.utsusynth.utsu.files.ThemeManager;
import com.utsusynth.utsu.model.config.Theme;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.util.ResourceBundle;

public class StartupDialog implements Localizable {
    public enum Decision {
        APPLY, CANCEL
    }

    private final PreferencesManager preferencesManager;
    private final ThemeManager themeManager;
    private final Localizer localizer;

    private final Label languageLabel;
    private final Label colorSchemeLabel;
    private final Button applyButton;
    private final Button cancelButton;

    private Scene dialogScene;
    private GridPane gridPane;
    private ChoiceBox<NativeLocale> languageChoiceBox;
    private ChoiceBox<Theme> colorSchemeChoiceBox;

    private Decision decision;

    @Inject
    public StartupDialog(
            PreferencesManager preferencesManager,
            ThemeManager themeManager,
            Localizer localizer) {
        this.preferencesManager = preferencesManager;
        this.themeManager = themeManager;
        this.localizer = localizer;
        decision = Decision.CANCEL;

        // Initialize values that need to be localized.
        languageLabel = new Label("Language");
        colorSchemeLabel = new Label("Color scheme");
        applyButton = new Button();
        cancelButton = new Button();
    }

    @Override
    public void localize(ResourceBundle bundle) {
        languageLabel.setText(bundle.getString("preferences.editor.language"));
        colorSchemeLabel.setText(bundle.getString("preferences.colorScheme"));
        themeManager.renameDefaultThemes(
                bundle.getString("preferences.colorScheme.defaultLight"),
                bundle.getString("preferences.colorScheme.defaultDark"));
        // Reload choice box to make new names appear.
        if (gridPane != null && colorSchemeChoiceBox != null) {
            initializeColorSchemeChoiceBox();
            gridPane.add(colorSchemeChoiceBox, 1, 1);
        }
        applyButton.setText(bundle.getString("general.apply"));
        cancelButton.setText(bundle.getString("general.cancel"));
    }

    public Decision popup() {
        Stage dialog = new Stage();

        languageChoiceBox = new ChoiceBox<>();
        languageChoiceBox.setPrefWidth(150);
        languageChoiceBox.setItems(FXCollections.observableArrayList(localizer.getAllLocales()));
        languageChoiceBox
                .setOnAction((action) -> localizer.setLocale(languageChoiceBox.getValue()));
        languageChoiceBox.setValue(localizer.getCurrentLocale());

        initializeColorSchemeChoiceBox();

        gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        gridPane.add(languageLabel, 0, 0);
        gridPane.add(languageChoiceBox, 1, 0);
        gridPane.add(colorSchemeLabel, 0, 1);
        gridPane.add(colorSchemeChoiceBox, 1, 1);

        BorderPane dialogPane = new BorderPane(gridPane);
        BorderPane.setMargin(gridPane, new Insets(10));

        ButtonBar buttonBar = new ButtonBar();
        buttonBar.setPrefHeight(40);
        buttonBar.setPadding(new Insets(0, 5, 0, 5));
        ButtonBar.setButtonData(applyButton, ButtonData.APPLY);
        ButtonBar.setButtonData(cancelButton, ButtonData.CANCEL_CLOSE);
        buttonBar.getButtons().addAll(cancelButton, applyButton);
        dialogPane.setBottom(buttonBar);

        applyButton.setDefaultButton(true);
        applyButton.setOnAction(event -> {
            decision = Decision.APPLY;
            preferencesManager.setTheme(themeManager.getCurrentTheme().get());
            preferencesManager.setLocale(localizer.getCurrentLocale());
            preferencesManager.saveToFile();
            dialog.close();
        });
        cancelButton.setCancelButton(true);
        cancelButton.setOnAction(event -> {
            decision = Decision.CANCEL;
            dialog.close();
        });

        localizer.localize(this);
        dialogScene = new Scene(dialogPane);
        dialog.setScene(dialogScene);
        dialog.showAndWait();

        return decision;
    }

    private void initializeColorSchemeChoiceBox() {
        colorSchemeChoiceBox = new ChoiceBox<>();
        themeManager.populateChoiceBox(colorSchemeChoiceBox);
        colorSchemeChoiceBox.setPrefWidth(150);
        colorSchemeChoiceBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Theme theme) {
                return theme.getName();
            }

            @Override
            public Theme fromString(String displayName) {
                return null; // Never used.
            }
        });
        colorSchemeChoiceBox.valueProperty().bindBidirectional(themeManager.getCurrentTheme());
        colorSchemeChoiceBox.valueProperty().addListener(
                obs -> themeManager.applyToScene(dialogScene));
    }
}
