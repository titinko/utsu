package com.utsusynth.utsu.model;

import com.google.common.collect.ImmutableList;
import com.utsusynth.utsu.common.quantize.Quantizer;
import com.utsusynth.utsu.model.pitch.PitchbendData;

/**
 * Represents one note in a song. This is primarily a data storage class, so it can be instantiated
 * directly and not injected.
 */
public class SongNote {
	// Values the user has control over. These are saved to file.
	private int delta; // In ms, corresponds with 125 bpm tempo.
	private int duration; // In ms, corresponds with 125 bpm tempo.
	private int length; // In ms, corresponds with 125 bpm tempo.
	private String lyric;
	private int noteNum; // Encapsulates both key and note.
	private double velocity;
	private double startPoint;
	private int intensity;
	private int modulation;
	private String noteFlags;
	private ImmutableList<Double> pbs; // Pitch bend start.
	private ImmutableList<Double> pbw; // Pitch bend widths
	private ImmutableList<Double> pby; // Pitch bend shifts
	private ImmutableList<String> pbm; // Pitch bend curves
	private double[] envelopeWidth; // "p" in ms
	private double[] envelopeHeight; // "v" in % of total intensity (0-100)
	private double envelopeOverlap; // This value is meaningless.
	private int[] vibrato;

	// Values calculated in-program and not saved to any file.
	private double realPreutter;
	private double realDuration;
	private double autoStartPoint; // This is added to the user-added startPoint.

	public SongNote() {
		// Set every required field to its default.
		this.delta = -1; // Must be set in builder.
		this.duration = -1; // Must be set in builder.
		this.length = -1; // Must be set in builder.
		this.lyric = ""; // Must be set in builder.
		this.noteNum = -1; // Must be set in builder.
		this.velocity = 100;
		this.startPoint = 0;
		this.intensity = 100;
		this.modulation = 0;
		this.noteFlags = "";
		this.pbs = ImmutableList.of(-40.0, 0.0);
		this.pbw = ImmutableList.of(80.0);
		this.pby = ImmutableList.of();
		this.pbm = ImmutableList.of();
		this.envelopeWidth = new double[5];
		this.envelopeHeight = new double[5];
		this.setEnvelope(
				new String[] { "5", "1", "1", "100", "100", "100", "100", "7", "35", "1", "100" });
		this.vibrato = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

		this.realPreutter = 0;
		this.realDuration = -1; // Should be ignored if not explicitly set.
		this.autoStartPoint = 0;
	}

	public void setDelta(int delta) {
		int roundedDelta = delta / Quantizer.SMALLEST * Quantizer.SMALLEST;
		this.delta = roundedDelta;
	}

	public int getDelta() {
		return this.delta;
	}

	public void setDuration(int duration) {
		int roundedDuration = duration / Quantizer.SMALLEST * Quantizer.SMALLEST;
		this.duration = roundedDuration;
	}

	public void safeSetDuration(int duration) {
		int roundedDuration = duration / Quantizer.SMALLEST * Quantizer.SMALLEST;
		if (this.length != -1 && this.length < roundedDuration) {
			this.duration = this.length;
		} else {
			this.duration = roundedDuration;
		}
	}

	public int getDuration() {
		return this.duration;
	}

	public void setLength(int length) {
		int roundedLength = length / Quantizer.SMALLEST * Quantizer.SMALLEST;
		this.length = roundedLength;
	}

	public void safeSetLength(int length) {
		int roundedLength = length / Quantizer.SMALLEST * Quantizer.SMALLEST;
		if (this.duration != -1 && roundedLength < this.duration) {
			this.duration = roundedLength;
		}
		this.length = roundedLength;
	}

	public int getLength() {
		return this.length;
	}

	public void setLyric(String lyric) {
		this.lyric = lyric;
	}

	public String getLyric() {
		return this.lyric;
	}

	public void setNoteNum(int noteNum) {
		this.noteNum = noteNum;
	}

	public int getNoteNum() {
		return this.noteNum;
	}

	public void setVelocity(double velocity) {
		this.velocity = velocity;
	}

	public double getVelocity() {
		return this.velocity;
	}

	public void setStartPoint(double startPoint) {
		this.startPoint = startPoint;
	}

	public double getStartPoint() {
		return this.startPoint;
	}

	public void setIntensity(int intensity) {
		this.intensity = intensity;
	}

	public int getIntensity() {
		return this.intensity;
	}

	public void setModulation(int modulation) {
		this.modulation = modulation;
	}

	public int getModulation() {
		return this.modulation;
	}

	public void setNoteFlags(String noteFlags) {
		this.noteFlags = noteFlags;
	}

	public String getNoteFlags() {
		return this.noteFlags;
	}

	public PitchbendData getPitchbends() {
		return new PitchbendData(pbs, pbw, pby, pbm, vibrato);
	}

	public void setPitchbends(PitchbendData pitchbends) {
		this.pbs = pitchbends.getPBS();
		this.pbw = pitchbends.getPBW();
		this.pby = pitchbends.getPBY();
		this.pbm = pitchbends.getPBM();
		this.vibrato = pitchbends.getVibrato();
	}

	public void setPBS(String[] pbsValues) {
		ImmutableList.Builder<Double> builder = ImmutableList.builder();
		for (String value : pbsValues) {
			builder.add(Double.parseDouble(value));
		}
		pbs = builder.build();
	}

	public ImmutableList<Double> getPBS() {
		return pbs;
	}

	public void setPBW(String[] pbwValues) {
		ImmutableList.Builder<Double> builder = ImmutableList.builder();
		for (String value : pbwValues) {
			builder.add(Double.parseDouble(value));
		}
		pbw = builder.build();
	}

	public ImmutableList<Double> getPBW() {
		return pbw;
	}

	public void setPBY(String[] pbyValues) {
		ImmutableList.Builder<Double> builder = ImmutableList.builder();
		for (String value : pbyValues) {
			builder.add(Double.parseDouble(value));
		}
		pby = builder.build();
	}

	public ImmutableList<Double> getPBY() {
		return pby;
	}

	public void setPBM(String[] pbmValues) {
		pbm = ImmutableList.copyOf(pbmValues);
	}

	public ImmutableList<String> getPBM() {
		return pbm;
	}

	public void setEnvelope(String[] envelopeValues) {
		if (envelopeValues.length > 0) {
			envelopeWidth[0] = Double.parseDouble(envelopeValues[0]); // p1
			envelopeWidth[1] = Double.parseDouble(envelopeValues[1]); // p2
			envelopeWidth[2] = Double.parseDouble(envelopeValues[2]); // p3
			envelopeHeight[0] = Double.parseDouble(envelopeValues[3]); // v1
			envelopeHeight[1] = Double.parseDouble(envelopeValues[4]); // v2
			envelopeHeight[2] = Double.parseDouble(envelopeValues[5]); // v3
		}
		if (envelopeValues.length > 6) {
			envelopeHeight[3] = Double.parseDouble(envelopeValues[6]); // v4
		}
		if (envelopeValues.length > 7) {
			envelopeOverlap = Double.parseDouble(envelopeValues[7]); // overlap
			envelopeWidth[3] = Double.parseDouble(envelopeValues[8]); // p4
		}
		if (envelopeValues.length > 9) {
			envelopeWidth[4] = Double.parseDouble(envelopeValues[9]); // p5
			envelopeHeight[4] = Double.parseDouble(envelopeValues[10]); // v5
		}
	}

	public String[] getFullEnvelope() {
		String[] envelope = new String[11];
		envelope[0] = Double.toString(envelopeWidth[0]); // p1
		envelope[1] = Double.toString(envelopeWidth[1]); // p2
		envelope[2] = Double.toString(envelopeWidth[2]); // p3
		envelope[3] = Double.toString(envelopeHeight[0]); // v1
		envelope[4] = Double.toString(envelopeHeight[1]); // v2
		envelope[5] = Double.toString(envelopeHeight[2]); // v3
		envelope[6] = Double.toString(envelopeHeight[3]); // v4
		envelope[7] = Double.toString(envelopeOverlap); // overlap
		envelope[8] = Double.toString(envelopeWidth[3]); // p4
		envelope[9] = Double.toString(envelopeWidth[4]); // p5
		envelope[10] = Double.toString(envelopeHeight[4]); // v5
		return envelope;
	}

	public void setEnvelope(EnvelopeData envelopeData) {
		envelopeWidth = envelopeData.getWidths();
		envelopeHeight = envelopeData.getHeights();
	}

	public EnvelopeData getEnvelope() {
		return new EnvelopeData(envelopeWidth, envelopeHeight);
	}

	public double getFadeIn() {
		return envelopeWidth[0];
	}

	public void setFadeIn(double newFadeIn) {
		envelopeWidth[0] = newFadeIn;
	}

	public void setFadeOut(double newFadeOut) {
		envelopeWidth[3] = newFadeOut;
	}

	public void setVibrato(String[] vibratoValues) {
		for (int i = 0; i < 10; i++) {
			vibrato[i] = Integer.parseInt(vibratoValues[i]);
		}
	}

	public String[] getVibrato() {
		String[] vibratoValues = new String[10];
		for (int i = 0; i < 10; i++) {
			vibratoValues[i] = Integer.toString(vibrato[i]);
		}
		return vibratoValues;
	}

	public double getRealPreutter() {
		return this.realPreutter;
	}

	public void setRealPreutter(double realPreutter) {
		this.realPreutter = realPreutter;
	}

	public double getRealDuration() {
		return this.realDuration;
	}

	public void setRealDuration(double realDuration) {
		this.realDuration = realDuration;
	}

	public double getAutoStartPoint() {
		return this.autoStartPoint;
	}

	public void setAutoStartPoint(double autoStartPoint) {
		this.autoStartPoint = autoStartPoint;
	}

	@Override
	public String toString() {
		// Crappy string representation of a Note object.
		return delta + " " + duration + " " + length + " " + lyric + " " + noteNum + " " + velocity
				+ " " + startPoint + " " + intensity + " " + modulation + " " + noteFlags;
	}
}
