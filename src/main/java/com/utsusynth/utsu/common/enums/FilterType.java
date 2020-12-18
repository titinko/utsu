package com.utsusynth.utsu.common.enums;

/* All available ways to filter notes when editing. */
public enum FilterType {
    SILENCE_BEFORE, // Does not consider preutterance.
    SILENCE_AFTER, // Does not connsider preutterance.
    RISING_NOTE,
    FALLING_NOTE,
    // Note length filters.
    GREATER_THAN_2ND,
    GREATER_THAN_4TH,
    GREATER_THAN_8TH,
}
