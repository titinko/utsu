package com.utsusynth.utsu.controller.song;

import com.google.inject.Inject;
import com.utsusynth.utsu.common.RegionBounds;
import com.utsusynth.utsu.common.data.EnvelopeData;
import com.utsusynth.utsu.common.data.PitchbendData;
import com.utsusynth.utsu.common.enums.FilterType;
import com.utsusynth.utsu.common.i18n.Localizable;
import com.utsusynth.utsu.common.i18n.Localizer;
import com.utsusynth.utsu.common.utils.RoundUtils;
import com.utsusynth.utsu.files.BulkEditorConfigManager;
import com.utsusynth.utsu.view.song.BulkEditor;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.prefs.Preferences;
import java.util.ResourceBundle;

public class PrefixSuffixController implements Localizable {
    private static final double LIST_HEIGHT = 260;

    // private final BulkEditorConfigManager configManager;
    // private final BulkEditor view;
    private final Localizer localizer;

    // private BulkEditorCallback callback;
    private RegionBounds highlightedRegion;
    private DoubleProperty listHeight;

    @FXML
    private BorderPane root;
    @FXML
    private Label actionLabel;
    @FXML
    private RadioButton addRadioButton;
    @FXML
    private RadioButton removeRadioButton;
    @FXML
    private Label targetLabel;
    @FXML
    private RadioButton prefixRadioButton;
    @FXML
    private RadioButton suffixRadioButton;
    @FXML
    private Label textLabel;
    @FXML
    private TextField prefixSuffixTextField;
    @FXML
    private AnchorPane prefixSuffixListAnchor;
    @FXML
    private Button applyAllButton;
    @FXML
    private Button applySelectionButton;
    @FXML
    private Button cancelButton;

    @Inject
    public PrefixSuffixController(
            BulkEditorConfigManager configManager, BulkEditor view, Localizer localizer) {
        // this.configManager = configManager;
        // this.view = view;
        this.localizer = localizer;
    }

    public void initialize() {
        listHeight = new SimpleDoubleProperty(LIST_HEIGHT);

        // Common setup for all bulk editors.
        // view.initialize(editorWidth, editorHeight);

        // Initialize action elements.
        ToggleGroup actionToggle = new ToggleGroup();
        addRadioButton.setToggleGroup(actionToggle);
        removeRadioButton.setToggleGroup(actionToggle);
        Preferences prefixSuffixPreferences = Preferences.userRoot().node("utsu/prefixSuffix");
        String actionChoice = prefixSuffixPreferences.get("action", "add");
        if (actionChoice.equals("add")) {
            addRadioButton.setSelected(true);
        } else {
            removeRadioButton.setSelected(true);
        }
        actionToggle.selectedToggleProperty().addListener(
                obs -> {
                    if (addRadioButton.isSelected()) {
                        prefixSuffixPreferences.put("action", "add");
                    } else {
                        prefixSuffixPreferences.put("action", "remove");
                    }
                    // view.setCurrentFilters(getFilters());
                });

        // Initialize target elements.
        ToggleGroup targetToggle = new ToggleGroup();
        prefixRadioButton.setToggleGroup(targetToggle);
        suffixRadioButton.setToggleGroup(targetToggle);
        String targetChoice = prefixSuffixPreferences.get("target", "prefix");
        if (targetChoice.equals("prefix")) {
            prefixRadioButton.setSelected(true);
        } else {
            suffixRadioButton.setSelected(true);
        }
        targetToggle.selectedToggleProperty().addListener(
                obs -> {
                    if (prefixRadioButton.isSelected()) {
                        prefixSuffixPreferences.put("target", "prefix");
                    } else {
                        prefixSuffixPreferences.put("target", "suffix");
                    }
                    // view.setCurrentFilters(getFilters());
                });

        // ObservableList<EnvelopeData> envelopeConfig = configManager.getEnvelopeConfig();
        // envelopeVBox.getChildren().add(0, view.createEnvelopeEditor(envelopeConfig.get(0)));
        // envelopeListAnchor.getChildren().add(view.createEnvelopeList(envelopeConfig, listHeight));

        localizer.localize(this);
    }

    @Override
    public void localize(ResourceBundle bundle) {
        applySelectionButton.setText(bundle.getString("bulkEditor.applySelection"));
        applyAllButton.setText(bundle.getString("bulkEditor.applyAll"));
        cancelButton.setText(bundle.getString("general.cancel"));
    }

    /** Secondary initialization. */
    void openEditor(
            RegionBounds region,
            Stage window,
            BulkEditorCallback callback) {
        // this.callback = callback;
        highlightedRegion = region;

        // Bind list height strictly to window size.
        InvalidationListener heightUpdate = new InvalidationListener() {
            @Override
            public void invalidated(Observable observable) {
                if (window.getHeight() > 0) {
                    double listHeightDifference = window.getHeight() - LIST_HEIGHT;
                    listHeight.bind(Bindings.max(
                            LIST_HEIGHT, window.heightProperty().subtract(listHeightDifference)));

                    window.heightProperty().removeListener(this);
                }
            }
        };
        window.heightProperty().addListener(heightUpdate);
    }

    @FXML
    public void addPrefixSuffix(ActionEvent event) {
        // view.saveToPortamentoList();
    }

    @FXML
    public void applyToSelection(ActionEvent event) {
        applyToNotes(highlightedRegion);
        Stage currentStage = (Stage) root.getScene().getWindow();
        currentStage.close();
    }

    @FXML
    public void applyToAllNotes(ActionEvent event) {
        applyToNotes(RegionBounds.WHOLE_SONG);
        Stage currentStage = (Stage) root.getScene().getWindow();
        currentStage.close();
    }

    private void applyToNotes(RegionBounds regionToUpdate) {
        // callback.updatePortamento(view.getPortamentoData(), regionToUpdate, filters);
    }

    @FXML
    public void cancelAndClose(ActionEvent event) {
        Stage currentStage = (Stage) root.getScene().getWindow();
        currentStage.close();
    }
}
