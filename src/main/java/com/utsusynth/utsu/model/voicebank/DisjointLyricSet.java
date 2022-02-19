package com.utsusynth.utsu.model.voicebank;

import com.google.common.collect.ImmutableSet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A disjoint set used to do automatic hiragana-romaji-katakana.
 */
public class DisjointLyricSet {
    private final Map<String, Set<String>> disjointSet;

    public static class Reader {
        private final Map<String, Set<String>> readonlyDisjointSet;
        private Reader(Map<String, Set<String>> readonlyDisjointSet) {
            this.readonlyDisjointSet = readonlyDisjointSet;
        }

        public ImmutableSet<String> getGroup(String member) {
            if (readonlyDisjointSet.containsKey(member)) {
                return ImmutableSet.copyOf(readonlyDisjointSet.get(member));
            }
            return ImmutableSet.of(member);
        }
    }

    public DisjointLyricSet() {
        disjointSet = new HashMap<>();
    }

    public void addGroup(ImmutableSet<String> members) {
        if (members.isEmpty()) {
            return;
        }
        HashSet<String> group = new HashSet<>();
        for (String member : members) {
            if (disjointSet.containsKey(member)) {
                merge(disjointSet.get(member), group);
            } else {
                group.add(member);
                disjointSet.put(member, group);
            }
        }
    }

    public ImmutableSet<String> getGroup(String member) {
        if (disjointSet.containsKey(member)) {
            return ImmutableSet.copyOf(disjointSet.get(member));
        }
        return ImmutableSet.of(member);
    }

    /** Returns a readonly view of a disjoint lyric set, useful for plugins. */
    public Reader getReader() {
        return new Reader(disjointSet);
    }

    /** Creates a deepcopy of this lyric set. */
    public DisjointLyricSet deepcopy() {
        DisjointLyricSet deepcopy = new DisjointLyricSet();
        for (Set<String> group : disjointSet.values()) {
            deepcopy.addGroup(ImmutableSet.copyOf(group));
        }
        return deepcopy;
    }

    /** Removes all groups from this lyric set. */
    public void clear() {
        disjointSet.clear();
    }

    private void merge(Set<String> oldGroup, Set<String> newGroup) {
        for (String member : oldGroup) {
            newGroup.add(member);
            disjointSet.put(member, newGroup);
        }
    }

}
