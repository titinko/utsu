package com.utsusynth.utsu.controller.common;

import javafx.scene.image.Image;

/** Singleton class, supplier of icon images loaded from the assets folder. */
public class IconManager {
    public enum IconType {
        REWIND_NORMAL, REWIND_PRESSED, PLAY_NORMAL, PLAY_PRESSED, PAUSE_NORMAL, PAUSE_PRESSED, STOP_NORMAL, STOP_PRESSED,
    }

    private final Image rewindImage;
    private final Image rewindImagePressed;
    private final Image playImage;
    private final Image playImagePressed;
    private final Image pauseImage;
    private final Image pauseImagePressed;
    private final Image stopImage;
    private final Image stopImagePressed;

    public IconManager(
            String rewindPath,
            String rewindPathPressed,
            String playPath,
            String playPathPressed,
            String pausePath,
            String pausePathPressed,
            String stopPath,
            String stopPathPressed) {
        this.rewindImage = new Image(getClass().getResourceAsStream(rewindPath));
        this.rewindImagePressed = new Image(getClass().getResourceAsStream(rewindPathPressed));
        this.playImage = new Image(getClass().getResourceAsStream(playPath));
        this.playImagePressed = new Image(getClass().getResourceAsStream(playPathPressed));
        this.pauseImage = new Image(getClass().getResourceAsStream(pausePath));
        this.pauseImagePressed = new Image(getClass().getResourceAsStream(pausePathPressed));
        this.stopImage = new Image(getClass().getResourceAsStream(stopPath));
        this.stopImagePressed = new Image(getClass().getResourceAsStream(stopPathPressed));
    }

    public Image getImage(IconType type) {
        switch (type) {
            case REWIND_NORMAL:
                return rewindImage;
            case REWIND_PRESSED:
                return rewindImagePressed;
            case PLAY_NORMAL:
                return playImage;
            case PLAY_PRESSED:
                return playImagePressed;
            case PAUSE_NORMAL:
                return pauseImage;
            case PAUSE_PRESSED:
                return pauseImagePressed;
            case STOP_NORMAL:
                return stopImage;
            case STOP_PRESSED:
                return stopImagePressed;
            default:
                // TODO: Handle this better.
                return playImage;
        }
    }
}
