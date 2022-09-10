package com.utsusynth.utsu.model.song;

import javafx.collections.ModifiableObservableListBase;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.scene.media.Media;
import javafx.scene.media.MediaMarkerEvent;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.function.Consumer;

public class Metronome {

    public Metronome(
        String clickUri,
        MediaPlayer songPlayer,
        Integer t
    ) {
        attach(
            songPlayer,
            new MediaPlayer(new Media(
                    clickUri
            )),
            generateMarkers(songPlayer.getTotalDuration().toSeconds(), t)
        );
    }

    public void attach(
            MediaPlayer songPlayer,
            MediaPlayer metronomePlayer,
            ObservableList<MediaMarkerEvent> markers
    ) {
        ObservableMap<String, Duration> mediaMarkers = songPlayer.getMedia().getMarkers();
        markers.forEach(mme ->
                mediaMarkers.put(
                        mme.getMarker().getKey(),
                        mme.getMarker().getValue()
                )
        );

        songPlayer.setOnMarker((MediaMarkerEvent mme) -> {
            metronomePlayer.stop();
            metronomePlayer.play();
        });
    }

    private ObservableList<MediaMarkerEvent> generateMarkers(
            double duration,
            int tempo
    ) {

        ArrayList<MediaMarkerEvent> events = new ArrayList<>();
        int steps = Math.floorDiv((int)duration, tempo);

        // Not thread safe
        return new ModifiableObservableListBase<>() {
            @Override
            public MediaMarkerEvent get(int i) {
                return events.get(i);
            }

            @Override
            public int size() {
                return events.size();
            }

            @Override
            protected void doAdd(int i, MediaMarkerEvent mediaMarkerEvent) {
                events.add(i, mediaMarkerEvent);
            }

            @Override
            protected MediaMarkerEvent doSet(int i, MediaMarkerEvent mediaMarkerEvent) {
                return events.set(i, mediaMarkerEvent);
            }

            @Override
            protected MediaMarkerEvent doRemove(int i) {
                return events.remove(i);
            }
        };
    }
}