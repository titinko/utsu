package com.utsusynth.utsu.controller;

import java.io.File;
import javafx.scene.image.Image;

/** Singleton class, supplier of icon images loaded from the assets folder. */
public class IconManager {
    public enum IconState {
        NORMAL, HOVER, DISABLED,
    }

    private final Image rewindImage;
    private final Image playImage;
    private final Image pauseImage;
    private final Image stopImage;

    public IconManager(File rewindPath, File playPath, File pausePath, File stopPath) {
        this.rewindImage = new Image("file:" + rewindPath.getAbsolutePath());
        this.playImage = new Image("file:" + playPath.getAbsolutePath());
        this.pauseImage = new Image("file:" + pausePath.getAbsolutePath());
        this.stopImage = new Image("file:" + stopPath.getAbsolutePath());
    }

    Image getRewindImage() {
        return rewindImage;
    }

    Image getPlayImage() {
        return playImage;
    }

    Image getPauseImage() {
        return pauseImage;
    }

    Image getStopImage() {
        return stopImage;
    }
}
