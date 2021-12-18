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

    public class Reader {
        private final Map<String, Set<String>> readonlyDisjointSet;
        private Reader(Map<String, Set<String>> readonlyDisjointSet) {
            this.readonlyDisjointSet = readonlyDisjointSet;
        }

        public ImmutableSet<String> getGroup(String member) {
            if (readonlyDisjointSet.containsKey(member)) {
                return ImmutableSet.copyOf(readonlyDisjointSet.get(member));
            }
            return ImmutableSet.of();
        }
    }

    public DisjointLyricSet() {
        disjointSet = new HashMap<>();
    }

    public void addGroup(String... members) {
        if (members.length == 0) {
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

    public HashSet<String> getGroup(String member) {
        HashSet<String> group = new HashSet<>();
        if (disjointSet.containsKey(member)) {
            group.addAll(disjointSet.get(member));
        }
        return group;
    }

    /** Returns a readonly view of a disjoint lyric set, useful for plugins. */
    public Reader getReader() {
        return new Reader(disjointSet);
    }

    private void merge(Set<String> oldGroup, Set<String> newGroup) {
        for (String member : oldGroup) {
            newGroup.add(member);
            disjointSet.put(member, newGroup);
        }
    }

}
