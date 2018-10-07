package com.utsusynth.utsu.controller.common;

import java.io.File;
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
            File rewindPath,
            File rewindPathPressed,
            File playPath,
            File playPathPressed,
            File pausePath,
            File pausePathPressed,
            File stopPath,
            File stopPathPressed) {
        this.rewindImage = new Image("file:" + rewindPath.getAbsolutePath());
        this.rewindImagePressed = new Image("file:" + rewindPathPressed.getAbsolutePath());
        this.playImage = new Image("file:" + playPath.getAbsolutePath());
        this.playImagePressed = new Image("file:" + playPathPressed.getAbsolutePath());
        this.pauseImage = new Image("file:" + pausePath.getAbsolutePath());
        this.pauseImagePressed = new Image("file:" + pausePathPressed.getAbsolutePath());
        this.stopImage = new Image("file:" + stopPath.getAbsolutePath());
        this.stopImagePressed = new Image("file:" + stopPathPressed.getAbsolutePath());
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
