package com.utsusynth.utsu.view.config;

import com.google.common.collect.ImmutableList;
import com.utsusynth.utsu.common.i18n.Localizable;
import com.utsusynth.utsu.common.i18n.Localizer;
import com.utsusynth.utsu.files.ThemeManager;
import com.utsusynth.utsu.model.config.Theme;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import javax.inject.Inject;
import java.util.List;
import java.util.ResourceBundle;

public class ThemeColorPicker implements Localizable {
    private final ThemeManager themeManager;
    private final Localizer localizer;

    private Theme theme;
    private TitledPane generalColors;
    private TitledPane noteColors;
    private TitledPane songColors;
    private TitledPane voicebankColors;

    @Inject
    public ThemeColorPicker(ThemeManager themeManager, Localizer localizer) {
        this.themeManager = themeManager;
        this.localizer = localizer;
    }

    public ScrollPane initialize(Theme theme) {
        this.theme = theme;
        Accordion accordion = new Accordion();
        generalColors = makeColorSection(
                "General",
                ImmutableList.of(
                        ImmutableList.of("BASE", "ACCENT"),
                        ImmutableList.of("HIGHLIGHTED", "HIGHLIGHTED_BORDER")));
        noteColors = makeColorSection(
                "Note",
                ImmutableList.of(
                        ImmutableList.of(
                            "NOTE_TEXT",
                            "NOTE",
                            "NOTE_BORDER",
                            "INVALID_NOTE",
                            "INVALID_NOTE_BORDER",
                            "PITCHBEND")));
        songColors = makeColorSection(
                "Song",
                ImmutableList.of(
                        ImmutableList.of("START_BAR", "END_BAR", "PLAYBACK_BAR"),
                        ImmutableList.of("SELECT_BOX_BORDER", "ADD_NOTE_BOX_BORDER"),
                        ImmutableList.of(
                                "TRACK_CELL_LIGHT",
                                "TRACK_CELL_DARK",
                                "TRACK_CELL_PREROLL",
                                "TRACK_BORDER_THICK",
                                "TRACK_BORDER_THIN"),
                        ImmutableList.of("ENVELOPE", "DYNAMICS_CELL", "DYNAMICS_BORDER"),
                        ImmutableList.of(
                                "PIANO_WHITE_KEY",
                                "PIANO_WHITE_KEY_TEXT",
                                "PIANO_BLACK_KEY",
                                "PIANO_BLACK_KEY_TEXT")));
        voicebankColors = makeColorSection(
                "Voicebank",
                ImmutableList.of(
                        ImmutableList.of(
                                "LYRIC_CONFIG_TEXT",
                                "FRQ_PRESENT",
                                "FRQ_ABSENT",
                                "FRQ_LOADING"),
                        ImmutableList.of("WAVEFORM_LINE", "FRQ_LINE"),
                        ImmutableList.of(
                                "OFFSET_CUTOFF_FILL",
                                "OFFSET_CUTOFF_LINE",
                                "OFFSET_CUTOFF_TEXT",
                                "CONSONANT_FILL",
                                "CONSONANT_LINE",
                                "CONSONANT_TEXT",
                                "VOWEL_FILL",
                                "PREUTTER_LINE",
                                "PREUTTER_TEXT",
                                "OVERLAP_LINE",
                                "OVERLAP_TEXT")));
        accordion.getPanes().addAll(generalColors, noteColors, songColors, voicebankColors);

        ScrollPane scrollPane = new ScrollPane(accordion);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setMaxHeight(310);

        localizer.localize(this);
        return scrollPane;
    }

    @Override
    public void localize(ResourceBundle bundle) {
        generalColors.setText("General");
        noteColors.setText("Note");
        songColors.setText("Song");
        voicebankColors.setText("Voicebank");
    }

    private TitledPane makeColorSection(String title, List<List<String>> widgetGroups) {
        VBox vBox = new VBox(10);
        for (int i = 0; i < widgetGroups.size(); i++) {
            if (i > 0) {
                vBox.getChildren().add(new Separator());
            }
            for (String widgetName : widgetGroups.get(i)) {
                ColorPicker colorPicker = new ColorPicker();
                if (theme.getColorMap().containsKey(widgetName)) {
                    colorPicker.setValue(theme.getColorMap().get(widgetName));
                }
                colorPicker.setOnAction(event -> {
                    theme.getColorMap().put(widgetName, colorPicker.getValue());
                    themeManager.reloadCurrentTheme();
                    themeManager.applyToScene(colorPicker.getScene());
                });
                Pane spacer = new Pane();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                HBox colorRow = new HBox(10);
                colorRow.setPrefWidth(300);
                colorRow.getChildren().addAll(new Label(widgetName), spacer, colorPicker);
                vBox.getChildren().add(colorRow);
            }
        }
        return new TitledPane(title, vBox);
    }
}
