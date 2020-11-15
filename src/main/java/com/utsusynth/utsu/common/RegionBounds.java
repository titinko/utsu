package com.utsusynth.utsu.common;

/**
 * A utility class that represents a 1-dimensional region and can determine whether a note is in
 * that region.
 */
public class RegionBounds {
    private final int minMs;
    private final int maxMs;

    public static RegionBounds WHOLE_SONG = new RegionBounds(0, Integer.MAX_VALUE);
    public static RegionBounds INVALID = new RegionBounds(Integer.MIN_VALUE, Integer.MIN_VALUE);

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

    public boolean contains(int otherMs) {
        return minMs <= otherMs && maxMs >= otherMs;
    }

    /**
     * Merges two regions into one. If one region is invalid, returns the other region.
     *
     * @param other the region to merge with.
     * @return the combination of the two regions.
     */
    public RegionBounds mergeWith(RegionBounds other) {
        if (this.equals(INVALID)) {
            return other;
        } else if (other.equals(INVALID)) {
            return this;
        }
        // Can be merged even if they don't intersect.
        return new RegionBounds(Math.min(minMs, other.minMs), Math.max(maxMs, other.maxMs));
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof RegionBounds)) {
            return false;
        }
        RegionBounds otherBounds = (RegionBounds) other;
        return this.minMs == otherBounds.minMs && this.maxMs == otherBounds.maxMs;
    }
}
