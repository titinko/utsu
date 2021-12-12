package com.utsusynth.utsu.model.song.converters;

import com.utsusynth.utsu.common.enums.ReclistType;

/** Converter of notes from one voicebank recording style to another. */
public interface ReclistConverter extends Converter {
    /** The reclist this converter expects. */
    ReclistType getFrom();

    /** The reclist this converter converts to. */
    ReclistType getTo();
}
