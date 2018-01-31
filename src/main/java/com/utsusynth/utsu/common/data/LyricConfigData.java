package com.utsusynth.utsu.common.data;

public class LyricConfigData {
    private final String lyric;
    private final String fileName;
    private final double offset; // Time in wav file before note starts, in ms.
    private final double consonant; // Time in wav file before consonant ends, in ms.
    private final double cutoff; // Time in wav file before note ends, in ms.
    private final double preutter; // Number of ms that go before note officially starts.
    private final double overlap; // Number of ms that overlap with previous note.

    public LyricConfigData(
            String lyric,
            String fileName,
            double offset,
            double consonant,
            double cutoff,
            double preutter,
            double overlap) {
        this.lyric = lyric;
        this.fileName = fileName;
        this.offset = offset;
        this.consonant = consonant;
        this.cutoff = cutoff;
        this.preutter = preutter;
        this.overlap = overlap;
    }

    public String getLyric() {
        return lyric;
    }

    public String getFileName() {
        return fileName;
    }

    public double getOffset() {
        return offset;
    }

    public double getConsonant() {
        return consonant;
    }

    public double getCutoff() {
        return cutoff;
    }

    public double getPreutter() {
        return preutter;
    }

    public double getOverlap() {
        return overlap;
    }
}
