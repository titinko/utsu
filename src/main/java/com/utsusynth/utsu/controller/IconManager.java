package com.utsusynth.utsu.controller;

import java.io.File;
import javafx.scene.image.Image;

/** Singleton class, supplier of icon images loaded from the assets folder. */
public class IconManager {
    public enum IconState {
        NORMAL, PRESSED, DISABLED,
    }

    private final Image rewindImage;
    private final Image rewindImagePressed;
    private final Image playImage;
    private final Image playImagePressed;
    private final Image playImageDisabled;
    private final Image pauseImage;
    private final Image pauseImagePressed;
    private final Image stopImage;
    private final Image stopImagePressed;

    public IconManager(
            File rewindPath,
            File rewindPathPressed,
            File playPath,
            File playPathPressed,
            File playPathDisabled,
            File pausePath,
            File pausePathPressed,
            File stopPath,
            File stopPathPressed) {
        this.rewindImage = new Image("file:" + rewindPath.getAbsolutePath());
        this.rewindImagePressed = new Image("file:" + rewindPathPressed.getAbsolutePath());
        this.playImage = new Image("file:" + playPath.getAbsolutePath());
        this.playImagePressed = new Image("file:" + playPathPressed.getAbsolutePath());
        this.playImageDisabled = new Image("file:" + playPathDisabled.getAbsolutePath());
        this.pauseImage = new Image("file:" + pausePath.getAbsolutePath());
        this.pauseImagePressed = new Image("file:" + pausePathPressed.getAbsolutePath());
        this.stopImage = new Image("file:" + stopPath.getAbsolutePath());
        this.stopImagePressed = new Image("file:" + stopPathPressed.getAbsolutePath());
    }

    Image getRewindImage(IconState state) {
        switch (state) {
            case NORMAL:
                return rewindImage;
            case PRESSED:
                return rewindImagePressed;
            default:
                // TODO: Handle this better
                return rewindImage;
        }
    }

    Image getPlayImage(IconState state) {
        switch (state) {
            case NORMAL:
                return playImage;
            case PRESSED:
                return playImagePressed;
            case DISABLED:
                return playImageDisabled;
            default:
                System.out.println("Warning: Tried to get a pause icon in state " + state);
                return playImage;
        }
    }

    Image getPauseImage(IconState state) {
        switch (state) {
            case NORMAL:
                return pauseImage;
            case PRESSED:
                return pauseImagePressed;
            default:
                System.out.println("Warning: Tried to get a pause icon in state " + state);
                return pauseImage;
        }
    }

    Image getStopImage(IconState state) {
        switch (state) {
            case NORMAL:
                return stopImage;
            case PRESSED:
                return stopImagePressed;
            default:
                System.out.println("Warning: Tried to get a stop icon in state " + state);
                return stopImage;
        }
    }
}
