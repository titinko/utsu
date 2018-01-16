package com.utsusynth.utsu.model.voicebank;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A disjoint set used to do automatic hiragana-romaji-katakana.
 */
public class DisjointLyricSet {
    private final Map<String, Set<String>> disjointSet;

    public DisjointLyricSet() {
        disjointSet = new HashMap<>();
    }

    public DisjointLyricSet addGroup(String... members) {
        if (members.length == 0) {
            return this;
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
        return this;
    }

    public HashSet<String> getGroup(String member) {
        HashSet<String> group = new HashSet<>();
        if (disjointSet.containsKey(member)) {
            group.addAll(disjointSet.get(member));
        }
        return group;
    }

    private void merge(Set<String> oldGroup, Set<String> newGroup) {
        for (String member : oldGroup) {
            newGroup.add(member);
            disjointSet.put(member, newGroup);
        }
    }

}
