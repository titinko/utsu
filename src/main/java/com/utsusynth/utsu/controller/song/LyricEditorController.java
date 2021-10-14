package com.utsusynth.utsu.controller.song;

import com.google.inject.Inject;
import com.utsusynth.utsu.common.RegionBounds;
import com.utsusynth.utsu.common.i18n.Localizable;
import com.utsusynth.utsu.common.i18n.Localizer;
import com.utsusynth.utsu.common.utils.PitchUtils;
import com.utsusynth.utsu.files.LyricEditorConfigManager;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.text.MessageFormat;
import java.util.Optional;
import java.util.prefs.Preferences;
import java.util.ResourceBundle;

public class LyricEditorController implements Localizable {
    // All available editors.
    public enum LyricEditorType {
        INSERT_LYRICS,
        PREFIX_SUFFIX,
    }

    private static final double LIST_HEIGHT = 110;

    private final LyricEditorConfigManager configManager;
    private final Localizer localizer;

    private LyricEditorCallback callback;
    private RegionBounds highlightedRegion;
    private ListView<String> prefixSuffixList;
    private DoubleProperty listHeight;

    /* Common elements. */
    @FXML
    private BorderPane root;
    @FXML
    private TabPane tabPane;
    @FXML
    private Button applyAllButton;
    @FXML
    private Button applySelectionButton;
    @FXML
    private Button cancelButton;

    /* Lyric insert tool elements. */
    @FXML
    private Tab insertLyricsTab;
    @FXML
    private Label insertLyricsLabel;
    @FXML
    private TextArea lyricsTextArea;
    @FXML
    private Text validateResult;
    @FXML
    private Button validateLyricsButton;

    /* Prefix/suffix editor elements. */
    @FXML
    private Tab prefixSuffixTab;
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

    @Inject
    public LyricEditorController(
            LyricEditorConfigManager configManager, Localizer localizer) {
        this.configManager = configManager;
        this.localizer = localizer;
    }

    public void initialize() {
        listHeight = new SimpleDoubleProperty(LIST_HEIGHT);

        // Initialize prefix/suffix elements.
        ToggleGroup actionToggle = new ToggleGroup();
        addRadioButton.setToggleGroup(actionToggle);
        removeRadioButton.setToggleGroup(actionToggle);
        Preferences prefixSuffixPrefs =
                Preferences.userRoot().node("utsu/lyricEditor/prefixSuffix");
        String actionChoice = prefixSuffixPrefs.get("action", "add");
        if (actionChoice.equals("add")) {
            addRadioButton.setSelected(true);
        } else {
            removeRadioButton.setSelected(true);
        }
        actionToggle.selectedToggleProperty().addListener(
                obs -> {
                    if (addRadioButton.isSelected()) {
                        prefixSuffixPrefs.put("action", "add");
                    } else {
                        prefixSuffixPrefs.put("action", "remove");
                    }
                });

        ToggleGroup targetToggle = new ToggleGroup();
        prefixRadioButton.setToggleGroup(targetToggle);
        suffixRadioButton.setToggleGroup(targetToggle);
        String targetChoice = prefixSuffixPrefs.get("target", "prefix");
        if (targetChoice.equals("prefix")) {
            prefixRadioButton.setSelected(true);
        } else {
            suffixRadioButton.setSelected(true);
        }
        targetToggle.selectedToggleProperty().addListener(
                obs -> {
                    if (prefixRadioButton.isSelected()) {
                        prefixSuffixPrefs.put("target", "prefix");
                    } else {
                        prefixSuffixPrefs.put("target", "suffix");
                    }
                });

        ObservableList<String> prefixSuffixConfig = configManager.getPrefixSuffixConfig();
        prefixSuffixListAnchor.getChildren().add(createPrefixSuffixList(prefixSuffixConfig));

        localizer.localize(this);
    }

    private ListView<String> createPrefixSuffixList(ObservableList<String> prefixSuffix) {
        Preferences prefixSuffixPrefs =
                Preferences.userRoot().node("utsu/lyricEditor/prefixSuffix");
        prefixSuffixList = new ListView<>();
        prefixSuffixList.prefHeightProperty().bind(listHeight);
        prefixSuffixList.setPrefWidth(150);
        prefixSuffixList.setCellFactory(source -> {
            ListCell<String> listCell = new ListCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);

                    setText(null);
                    if (empty || item == null || item.isEmpty()) {
                        setGraphic(null);
                    } else {
                        BorderPane graphic = new BorderPane();
                        Text itemText = new Text(item);
                        itemText.getStyleClass().add("list-text");
                        graphic.setLeft(itemText);
                        Button closeButton = new Button("X");
                        closeButton.setOnAction(event -> {
                            if (getIndex() >= configManager.getNumDefaultPrefixSuffix()) {
                                getListView().getItems().remove(getIndex());
                                // Select index 0.
                                if (selectFromPrefixSuffixList(0)) {
                                    prefixSuffixPrefs.putInt("listIndex", 0);
                                    prefixSuffixList.getSelectionModel().select(0);
                                }
                            }
                        });
                        if (getIndex() < configManager.getNumDefaultPrefixSuffix()) {
                            closeButton.setDisable(true);
                        }
                        graphic.setRight(closeButton);
                        setGraphic(graphic);
                    }
                }
            };
            listCell.setOnMouseClicked(event -> {
                int index = listCell.getIndex();
                if (selectFromPrefixSuffixList(index)) {
                    prefixSuffixPrefs.putInt("listIndex", index); // Cache selected index.
                }
            });
            return listCell;
        });
        prefixSuffixList.setItems(prefixSuffix);
        // Select cached index if possible.
        int cachedIndex = prefixSuffixPrefs.getInt("listIndex", 0);
        if (selectFromPrefixSuffixList(cachedIndex)) {
            prefixSuffixList.getSelectionModel().select(cachedIndex);
        }
        return prefixSuffixList;
    }

    private boolean selectFromPrefixSuffixList(int index) {
        if (prefixSuffixList == null || prefixSuffixList.getItems().size() <= index) {
            return false;
        }
        String newItem = prefixSuffixList.getItems().get(index);
        if (newItem == null || newItem.isEmpty()) {
            return false;
        }
        if (index == 0) {
            // Special handling for custom prefix/suffix.
            prefixSuffixTextField.clear();
            prefixSuffixTextField.setEditable(true);
            prefixSuffixTextField.requestFocus();
        } else {
            prefixSuffixTextField.setText(newItem);
            prefixSuffixTextField.setEditable(false);
        }
        return true;
    }

    @Override
    public void localize(ResourceBundle bundle) {
        applySelectionButton.setText(bundle.getString("bulkEditor.applySelection"));
        applyAllButton.setText(bundle.getString("bulkEditor.applyAll"));
        cancelButton.setText(bundle.getString("general.cancel"));

        insertLyricsTab.setText(bundle.getString("menu.tools.lyricEditor.insertLyrics"));
        insertLyricsLabel.setText(bundle.getString("lyricEditor.insertLyrics.instructions"));
        validateLyricsButton.setText(bundle.getString("lyricEditor.insertLyrics.validate"));

        prefixSuffixTab.setText(bundle.getString("menu.tools.lyricEditor.prefixSuffix"));
        actionLabel.setText(bundle.getString("lyricEditor.prefixSuffix.action"));
        addRadioButton.setText(bundle.getString("lyricEditor.prefixSuffix.action.add"));
        removeRadioButton.setText(bundle.getString("lyricEditor.prefixSuffix.action.remove"));
        targetLabel.setText(bundle.getString("lyricEditor.prefixSuffix.target"));
        prefixRadioButton.setText(bundle.getString("lyricEditor.prefixSuffix.target.prefix"));
        suffixRadioButton.setText(bundle.getString("lyricEditor.prefixSuffix.target.suffix"));
        textLabel.setText(bundle.getString("lyricEditor.prefixSuffix.text"));
    }

    /** Secondary initialization. */
    void openEditor(
            LyricEditorType editorType,
            RegionBounds region,
            Stage window,
            LyricEditorCallback callback) {
        this.callback = callback;
        highlightedRegion = region;

        // Open the correct tab.
        if (editorType.equals(LyricEditorType.INSERT_LYRICS)) {
            tabPane.getSelectionModel().select(insertLyricsTab);
        } else if (editorType.equals(LyricEditorType.PREFIX_SUFFIX)) {
            tabPane.getSelectionModel().select(prefixSuffixTab);
        }

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
    public void validateLyrics(ActionEvent event) {
        String lyricText = lyricsTextArea.getText();
        String[] lyrics = lyricText.trim().split("\\s+");
        if (lyricText.trim().isEmpty() || lyrics.length == 0) {
            validateResult.getStyleClass().clear();
            validateResult.getStyleClass().add("failure");
            validateResult.setText(
                    localizer.getMessage("lyricEditor.insertLyrics.validateError"));
            return;
        }
        validateResult.getStyleClass().clear();
        validateResult.getStyleClass().add("success");
        validateResult.setText(MessageFormat.format(
                localizer.getMessage("lyricEditor.insertLyrics.validateSuccess"),
                lyrics.length));
    }

    @FXML
    public void addPrefixSuffix(ActionEvent event) {
        if (prefixSuffixList == null || !prefixSuffixList.getSelectionModel().isSelected(0)) {
            return; // Do nothing if "Custom" isn't the selected option.
        }
        if (!prefixSuffixTextField.getText().isEmpty())
        prefixSuffixList.getItems().add(prefixSuffixTextField.getText());
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
        if (tabPane.getSelectionModel().getSelectedItem() == insertLyricsTab) {
            String lyricText = lyricsTextArea.getText();
            String[] lyrics = lyricText.trim().split("\\s+");
            if (lyricText.trim().isEmpty() || lyrics.length == 0) {
                return;
            }
            callback.insertLyrics(lyrics, regionToUpdate);
        } else if (tabPane.getSelectionModel().getSelectedItem() == prefixSuffixTab) {
            if (prefixSuffixTextField.getText().isEmpty() || prefixSuffixList == null) {
                return;
            }
            String prefixSuffix = prefixSuffixTextField.getText();
            // Assumes that 2nd item in prefix suffix list is the special pitch one.
            boolean isPitch = prefixSuffix.equals(prefixSuffixList.getItems().get(1));

            if (addRadioButton.isSelected() && prefixRadioButton.isSelected()) {
                callback.transformLyric(noteData -> {
                    if (isPitch) {
                        return noteData; // No pitch prefixes allowed.
                    }
                    return noteData.withNewLyric(prefixSuffix + noteData.getLyric());
                }, regionToUpdate);
            } else if (removeRadioButton.isSelected() && prefixRadioButton.isSelected()) {
                callback.transformLyric(noteData -> {
                    if (isPitch) {
                        return noteData; // No pitch prefixes allowed.
                    }
                    if (noteData.getLyric().startsWith(prefixSuffix)) {
                        return noteData.withNewLyric(
                                noteData.getLyric().substring(prefixSuffix.length()));
                    }
                    return noteData;
                }, regionToUpdate);
            } else if (addRadioButton.isSelected() && suffixRadioButton.isSelected()) {
                callback.transformLyric(noteData -> {
                    if (isPitch) {
                        String pitchString = extractPitch(noteData.getTrueLyric());
                        if (!pitchString.isEmpty()
                                && extractPitch(Optional.of(noteData.getLyric())).isEmpty()) {
                            return noteData.withNewLyric(noteData.getLyric() + pitchString);
                        }
                        return noteData;
                    }
                    return noteData.withNewLyric(noteData.getLyric() + prefixSuffix);
                }, regionToUpdate);
            } else if (removeRadioButton.isSelected() && suffixRadioButton.isSelected()) {
                callback.transformLyric(noteData -> {
                    if (isPitch) {
                        String pitchString = extractPitch(Optional.of(noteData.getLyric()));
                        if (!pitchString.isEmpty()) {
                            return noteData.withNewLyric(noteData.getLyric().substring(
                                    0, noteData.getLyric().length() - pitchString.length()));
                        }
                    }
                    if (noteData.getLyric().endsWith(prefixSuffix)) {
                        return noteData.withNewLyric(noteData.getLyric().substring(
                                0, noteData.getLyric().length() - prefixSuffix.length()));
                    }
                    return noteData;
                }, regionToUpdate);
            }
        }
    }

    private static String extractPitch(Optional<String> lyric) {
        if (lyric.isEmpty()) {
            return "";
        }
        for (String pitch : PitchUtils.PITCHES) {
            for (int i = 1; i <= 7; i++) {
                String fullPitch = pitch + i;
                if (lyric.get().endsWith(fullPitch)
                        || lyric.get().endsWith(fullPitch.toLowerCase())) {
                    return fullPitch;
                }
            }
        }
        return "";
    }

    @FXML
    public void cancelAndClose(ActionEvent event) {
        Stage currentStage = (Stage) root.getScene().getWindow();
        currentStage.close();
    }
}
