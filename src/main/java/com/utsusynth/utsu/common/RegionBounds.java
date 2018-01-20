package com.utsusynth.utsu.common;

/**
 * A utility class that represents a 1-dimensional region and can determine whether a note is in
 * that region.
 */
public class RegionBounds {
    private final int minMs;
    private final int maxMs;

    public static RegionBounds WHOLE_SONG = new RegionBounds(0, Integer.MAX_VALUE);

    public RegionBounds(int minMs, int maxMs) {
        this.minMs = minMs;
        this.maxMs = maxMs;
    }

    public int getMinMs() {
        return minMs;
    }

    public int getMaxMs() {
        return maxMs;
    }

    public boolean intersects(RegionBounds other) {
        return !(minMs >= other.maxMs) && !(maxMs <= other.minMs);
    }

    public boolean intersects(int otherMinMs, int otherMaxMs) {
        return intersects(new RegionBounds(otherMinMs, otherMaxMs));
    }

    public RegionBounds mergeWith(RegionBounds other) {
        // Can be merged even if they don't intersect.
        return new RegionBounds(Math.min(minMs, other.minMs), Math.max(maxMs, other.maxMs));
    }
}
