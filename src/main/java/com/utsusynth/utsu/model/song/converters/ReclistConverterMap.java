package com.utsusynth.utsu.model.song.converters;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.SetMultimap;
import com.google.inject.Inject;
import com.utsusynth.utsu.UtsuModule.ReclistConverters;
import com.utsusynth.utsu.common.enums.ReclistType;

import java.util.*;

public class ReclistConverterMap {
    private final SetMultimap<ReclistType, ReclistConverter> converterMap;

    @Inject
    public ReclistConverterMap(@ReclistConverters ImmutableSet<ReclistConverter> converters) {
        ImmutableSetMultimap.Builder<ReclistType, ReclistConverter> builder =
                ImmutableSetMultimap.builder();
        for (ReclistConverter converter : converters) {
            builder.put(converter.getFrom(), converter);
        }
        converterMap = builder.build();
    }

    public Set<ReclistType> keySet() {
        return converterMap.keySet();
    }

    public HashMap<ReclistType, List<ReclistConverter>> traverseReclists(ReclistType source) {
        HashSet<ReclistType> visited = new HashSet<>();
        HashMap<ReclistType, List<ReclistConverter>> result = new HashMap<>();
        traverseReclistsRecursive(source, visited, result, ImmutableList.of());
        return result;
    }

    private void traverseReclistsRecursive(
            ReclistType source,
            Set<ReclistType> visited,
            Map<ReclistType, List<ReclistConverter>> result,
            ImmutableList<ReclistConverter> path) {
        if (visited.contains(source)) {
            return;
        }
        visited.add(source);
        if (!path.isEmpty()) {
            result.put(source, path);
        }
        for (ReclistConverter converter : converterMap.get(source)) {
            ImmutableList<ReclistConverter> newPath = ImmutableList.<ReclistConverter>builder()
                    .addAll(path)
                    .add(converter)
                    .build();
            traverseReclistsRecursive(converter.getTo(), visited, result, newPath);
        }
    }
}
