package com.utsusynth.utsu.model.song;

import javafx.collections.ModifiableObservableListBase;
import javafx.collections.ObservableList;
import javafx.scene.media.Media;
import javafx.scene.media.MediaMarkerEvent;
import javafx.scene.media.MediaPlayer;

import java.util.ArrayList;

public class Metronome {
    Media click;
    MediaPlayer songPlayer;

    public Metronome(
        String clickUri,
        MediaPlayer mp,
        Integer t
    ) {
        this.click = new Media(
                clickUri
        );
        this.songPlayer = mp;
        attach(generateMarkers(mp.getTotalDuration().toSeconds(), t));
    }

    public void attach(
            ObservableList<MediaMarkerEvent> markers
    ) {
//        songPlayer.getMarkers().put(markers);
        songPlayer.setOnMarker((MediaMarkerEvent mme) -> {
//            this.click.boop

        });
    }

    private ObservableList<MediaMarkerEvent> generateMarkers(
            double duration,
            int tempo
    ) {

        ArrayList<MediaMarkerEvent> events = new ArrayList<MediaMarkerEvent>();
        int steps = Math.floorDiv((int)duration, tempo);

        // Not thread safe
        return new ModifiableObservableListBase<MediaMarkerEvent>() {
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