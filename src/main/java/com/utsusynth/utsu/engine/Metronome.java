package com.utsusynth.utsu.engine;

import javafx.beans.property.BooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableBooleanValue;
import javafx.collections.ObservableMap;
import javafx.scene.media.Media;
import javafx.scene.media.MediaMarkerEvent;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

public class Metronome {
    public Metronome(
        MediaPlayer songPlayer,
        Double t,
        String clickUri,
        BooleanProperty enabled
    ) {
        attach(
            songPlayer,
            new MediaPlayer(new Media(
                    clickUri
            )),
            generateMarkers(songPlayer.getTotalDuration().toSeconds(), t),
            enabled
        );
    }

    void attach(
            MediaPlayer songPlayer,
            MediaPlayer metronomePlayer,
            List<Duration> intervals,
            ObservableBooleanValue enabled
    ) {
        ObservableMap<String, Duration> mediaMarkers = songPlayer.getMedia().getMarkers();
        ThreadLocal<Integer> key = new ThreadLocal<>();
        key.set(0);

        intervals.forEach(mme -> {
            key.set(key.get() + 1);
            mediaMarkers.put(
                String.valueOf(key.get()),
                mme
            );
        });

        ChangeListener<Boolean> enabledListener = (observable, oldValue, newValue) -> {
            metronomePlayer.stop();
        };
        enabled.addListener(enabledListener);
        songPlayer.setOnMarker((MediaMarkerEvent mme) -> {
            if (enabled.get()) {
                metronomePlayer.stop();
                metronomePlayer.play();
            }
        });

        Runnable onStop = songPlayer.getOnStopped();
        songPlayer.setOnStopped(() -> {
            metronomePlayer.stop();
            metronomePlayer.dispose();

            enabled.removeListener(enabledListener);
            onStop.run();
        });
    }

    List<Duration> generateMarkers(
            double seconds,
            double bpm
    ) {

        ArrayList<Duration> events = new ArrayList<>();
        double bps = bpm * (1.0 / 60.0);
        double ms_beat = (1.0 / bps) * (1000.0);

        for (int i = 0; i < seconds * 1000; i+=ms_beat) {
            events.add(new Duration(i));
        }

        return events;
    }
}