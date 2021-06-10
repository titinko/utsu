package com.utsusynth.utsu.view.song.track;

import com.google.common.collect.ImmutableList;
import com.utsusynth.utsu.view.song.track.TrackItem.TrackItemType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Abstraction of a group of TrackItems to make them easier to use by Track. */
public class TrackItemSet {
    private final Map<TrackItemType, Set<TrackItem>> items;

    TrackItemSet() {
        items = new HashMap<>();
    }

    private TrackItemSet(Map<TrackItemType, Set<TrackItem>> items) {
        this.items = items;
    }

    /** Returns all elements in the set in the order they should be rendered. */
    ImmutableList<TrackItem> asList() {
        ImmutableList.Builder<TrackItem> builder = ImmutableList.builder();
        if (items.containsKey(TrackItemType.NOTE)) {
            builder.addAll(items.get(TrackItemType.NOTE));
        }
        if (items.containsKey(TrackItemType.ENVELOPE)) {
            builder.addAll(items.get(TrackItemType.ENVELOPE));
        }
        if (items.containsKey(TrackItemType.PITCHBEND)) {
            builder.addAll(items.get(TrackItemType.PITCHBEND));
        }
        if (items.containsKey(TrackItemType.PLAYBACK)) {
            builder.addAll(items.get(TrackItemType.PLAYBACK));
        }
        if (items.containsKey(TrackItemType.LYRIC)) {
            builder.addAll(items.get(TrackItemType.LYRIC));
        }
        if (items.containsKey(TrackItemType.DRAWING)) {
            builder.addAll(items.get(TrackItemType.DRAWING));
        }
        return builder.build();
    }

    /** Returns a new class instance with the same underlying items map. */
    TrackItemSet withItem(TrackItem item) {
        if (items.containsKey(item.getType())) {
            items.get(item.getType()).add(item);
        } else {
            HashSet<TrackItem> newSet = new HashSet<>();
            newSet.add(item);
            items.put(item.getType(), newSet);
        }
        return new TrackItemSet(items);
    }

    TrackItemSet withoutItem(TrackItem item) {
        if (items.containsKey(item.getType())) {
            items.get(item.getType()).remove(item);
        }
        return new TrackItemSet(items);
    }

    boolean hasItem(TrackItem item) {
        return items.containsKey(item.getType()) && items.get(item.getType()).contains(item);
    }
}
