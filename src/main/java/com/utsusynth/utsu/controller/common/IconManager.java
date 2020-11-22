package com.utsusynth.utsu.controller.common;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;

/** Singleton class, supplier of icon shapes and settings. */
public class IconManager {
    public void setRewindIcon(Pane parent) {
        Rectangle leftBar = new Rectangle(7.0, 7.0, 6.0, 18.0);
        leftBar.getStyleClass().addAll("playback-icon", "not-selected");
        Polygon rightPolygon = new Polygon();
        rightPolygon.getPoints().addAll(
                14.0, 16.0,
                14.0, 15.0,
                22.0, 7.0,
                24.0, 7.0,
                24.0, 24.0,
                22.0, 24.0);
        rightPolygon.getStyleClass().addAll("playback-icon", "not-selected");
        parent.getChildren().clear();
        parent.getChildren().add(new Group(leftBar, rightPolygon));
    }

    public void setPlayIcon(Pane parent) {
        Polygon playIcon = new Polygon();
        playIcon.getPoints().addAll(
                8.0, 5.0,
                8.0, 26.0,
                26.0, 16.0);
        playIcon.getStyleClass().addAll("playback-icon", "not-selected");
        parent.getChildren().clear();
        parent.getChildren().add(playIcon);
    }

    public void setPauseIcon(Pane parent) {
        Rectangle leftBar = new Rectangle(7.0, 7.0, 6.0, 18.0);
        leftBar.getStyleClass().addAll("playback-icon", "not-selected");
        Rectangle rightBar = new Rectangle(19.0, 7.0, 6.0, 18.0);
        rightBar.getStyleClass().addAll("playback-icon", "not-selected");
        parent.getChildren().clear();
        parent.getChildren().add(new Group(leftBar, rightBar));
    }

    public void setStopIcon(Pane parent) {
        Rectangle stopIcon = new Rectangle(7.0, 7.0, 18.0, 18.0);
        stopIcon.getStyleClass().addAll("playback-icon", "not-selected");
        parent.getChildren().clear();
        parent.getChildren().add(stopIcon);
    }

    public void selectIcon(Pane selectMe) {
        Node child = selectMe.getChildren().get(0);
        if (child instanceof Group) {
            Group childGroup = (Group) child;
            for (Node node : childGroup.getChildren()) {
                node.getStyleClass().clear();
                node.getStyleClass().addAll("playback-icon", "selected");
            }
        } else {
            child.getStyleClass().clear();
            child.getStyleClass().addAll("playback-icon", "selected");
        }
    }

    public void deselectIcon(Pane deselectMe) {
        Node child = deselectMe.getChildren().get(0);
        if (child instanceof Group) {
            Group childGroup = (Group) child;
            for (Node node : childGroup.getChildren()) {
                node.getStyleClass().clear();
                node.getStyleClass().addAll("playback-icon", "not-selected");
            }
        } else {
            child.getStyleClass().clear();
            child.getStyleClass().addAll("playback-icon", "not-selected");
        }
    }
}
