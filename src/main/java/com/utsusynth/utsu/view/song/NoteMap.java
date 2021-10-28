package com.utsusynth.utsu.view.song;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.google.inject.Inject;
import com.utsusynth.utsu.common.utils.RegionBounds;
import com.utsusynth.utsu.common.data.EnvelopeData;
import com.utsusynth.utsu.common.data.PitchbendData;
import com.utsusynth.utsu.common.exception.NoteAlreadyExistsException;
import com.utsusynth.utsu.view.song.note.Note;
import com.utsusynth.utsu.view.song.note.envelope.Envelope;
import com.utsusynth.utsu.view.song.note.envelope.EnvelopeCallback;
import com.utsusynth.utsu.view.song.note.envelope.EnvelopeFactory;
import com.utsusynth.utsu.view.song.note.pitch.Pitchbend;
import com.utsusynth.utsu.view.song.note.pitch.PitchbendCallback;
import com.utsusynth.utsu.view.song.note.pitch.PitchbendFactory;
import com.utsusynth.utsu.view.song.track.Track;
import javafx.beans.property.BooleanProperty;

public class NoteMap {
    private final EnvelopeFactory envelopeFactory;
    private final PitchbendFactory pitchbendFactory;

    // Maps absolute position (in ms) to track note's data.
    private Map<Integer, Note> noteMap;
    private Map<Integer, Envelope> envelopeMap;
    private Map<Integer, Pitchbend> pitchbendMap;

    private Track track;

    @Inject
    public NoteMap(
            EnvelopeFactory envelopeFactory, PitchbendFactory pitchbendFactory) {
        this.envelopeFactory = envelopeFactory;
        this.pitchbendFactory = pitchbendFactory;
        clear();
    }

    void clear() {
        noteMap = new HashMap<>();
        envelopeMap = new HashMap<>();
        pitchbendMap = new HashMap<>();
    }

    void setTrack(Track track) {
        this.track = track;
    }

    boolean hasNote(int position) {
        return noteMap.containsKey(position);
    }

    Note getNote(int position) {
        return noteMap.get(position);
    }

    int getFirstPosition(RegionBounds region) {
        int minPosition = Integer.MAX_VALUE;
        for (int position : noteMap.keySet()) {
            if (noteMap.get(position).getValidBounds().intersects(region)
                    && position < minPosition) {
                minPosition = position;
            }
        }
        return minPosition;
    }

    int getLastPosition(RegionBounds region) {
        int maxPosition = Integer.MIN_VALUE;
        for (int position : noteMap.keySet()) {
            if (noteMap.get(position).getValidBounds().intersects(region)
                    && position > maxPosition) {
                maxPosition = position;
            }
        }
        return maxPosition;
    }

    Collection<Note> getAllValidNotes() {
        return noteMap.values();
    }

    void putNote(int position, Note note) throws NoteAlreadyExistsException {
        if (noteMap.containsKey(position)) {
            throw new NoteAlreadyExistsException();
        }
        addNoteElement(note);
        noteMap.put(position, note);
    }

    void addNoteElement(Note note) {
        track.insertItem(track.getNoteTrack(), note);
        track.insertItem(track.getNoteTrack(), note.getLyricTrackItem());
    }

    void removeNoteElement(Note note) {
        track.removeItem(track.getNoteTrack(), note);
        track.removeItem(track.getNoteTrack(), note.getLyricTrackItem());
    }

    void removeFullNote(int position) {
        if (noteMap.containsKey(position)) {
            noteMap.remove(position);
        } else {
            // TODO: Handle this better.
            System.out.println("Could not find note in map of track notes :(");
        }
        if (envelopeMap.containsKey(position)) {
            track.removeItem(track.getDynamicsTrack(), envelopeMap.get(position));
            envelopeMap.remove(position);
        }
        if (pitchbendMap.containsKey(position)) {
            track.removeItem(track.getNoteTrack(), pitchbendMap.get(position));
            pitchbendMap.remove(position);
        }
    }

    void putEnvelope(int position, EnvelopeData envelopeData, EnvelopeCallback callback) {
        // Track note must exist before envelope is added.
        if (noteMap.containsKey(position)) {
            Envelope envelope =
                    envelopeFactory.createEnvelope(noteMap.get(position), envelopeData, callback);
            // Overrides are expected here.
            if (envelopeMap.containsKey(position)) {
                track.removeItem(track.getDynamicsTrack(), envelopeMap.get(position));
                envelopeMap.remove(position);
            }
            envelopeMap.put(position, envelope);
            track.insertItem(track.getDynamicsTrack(), envelope);
        }
    }

    boolean hasEnvelope(int position) {
        return envelopeMap.containsKey(position);
    }

    Envelope getEnvelope(int position) {
        return envelopeMap.get(position);
    }

    void putPitchbend(
            int position,
            String prevPitch,
            PitchbendData pitchData,
            PitchbendCallback callback,
            BooleanProperty vibratoEditor,
            BooleanProperty showPitchbend) {
        if (noteMap.containsKey(position)) {
            Pitchbend pitchbend = pitchbendFactory.createPitchbend(
                    noteMap.get(position),
                    prevPitch,
                    pitchData,
                    callback,
                    vibratoEditor,
                    showPitchbend);
            // Overrides are expected here.
            if (pitchbendMap.containsKey(position)) {
                track.removeItem(track.getNoteTrack(), pitchbendMap.get(position));
                pitchbendMap.remove(position);
            }
            pitchbendMap.put(position, pitchbend);
            track.insertItem(track.getNoteTrack(), pitchbend);
        }
    }

    boolean hasPitchbend(int position) {
        return pitchbendMap.containsKey(position);
    }

    Pitchbend getPitchbend(int position) {
        return pitchbendMap.get(position);
    }
}
