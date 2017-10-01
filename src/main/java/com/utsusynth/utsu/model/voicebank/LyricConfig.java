package com.utsusynth.utsu.model.voicebank;

/**
 * Internal representation of the configuration of a single lyric in a voicebank. Parsed from an
 * oto.ini file. TODO: Support oto_ini.txt as well
 */
public class LyricConfig {
	private String pathToFile; // example: /Library/Iona.utau/C3/de.wav
	private String trueLyric; // example: de
	private double offset; // Time in wav file before note starts, in ms.
	private double consonant; // Time in wav file before consonant ends, in ms.
	private double cutoff; // Time in wav file before note ends, in ms.
	private double preutterance; // Number of ms that go before note officially starts.
	private double overlap; // Number of ms that overlap with previous note.

	LyricConfig(String pathToFile, String trueLyric, String[] configValues) {
		assert (configValues.length == 5);
		this.pathToFile = pathToFile;
		this.trueLyric = trueLyric;
		this.offset = Double.parseDouble(configValues[0]);
		this.consonant = Double.parseDouble(configValues[1]);
		this.cutoff = Double.parseDouble(configValues[2]);
		this.preutterance = Double.parseDouble(configValues[3]);
		this.overlap = Double.parseDouble(configValues[4]);
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

	public double getPreutterance() {
		return preutterance;
	}

	public double getOverlap() {
		return overlap;
	}

	public String getPathToFile() {
		return pathToFile;
	}

	public String getTrueLyric() {
		return trueLyric;
	}

	@Override
	public String toString() {
		return pathToFile + " " + offset + " " + consonant + " " + cutoff + " " + preutterance + " "
				+ overlap;
	}
}
