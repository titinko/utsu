package com.utsusynth.utsu.model.pitch;

/** Represents an "s"-shaped portamento. */
class LogisticPortamento extends Portamento {
	private final int noteMs;
	private final double x1;
	private final double y1;
	private final double x2;
	private final double y2;
	private final double halfX; // Halfway point along logistic curve.
	private final double steepness; // Steepness of logistic curve.
	private final double maxY; // Upper bound of logistic curve.
	
	LogisticPortamento(int noteMs, double x1, double y1, double x2, double y2) {
		this.noteMs = noteMs;
		this.x1 = x1;
		this.y1 = y1;
		this.x2 = x2;
		this.y2 = y2;
		this.halfX = (x2 - x1) / 2;
		this.steepness = 5 / this.halfX;
		this.maxY = y2 - y1;
	}
	
	@Override
	public double apply(int positionMs) {
		if (positionMs < x1 || positionMs > x2) {
			// TODO: Handle this.
			System.out.println("Tried to apply a logistic portamento that doesn't exist here.");
			return 0.0;
		}
		double adjustedX = positionMs - x1;
		return maxY / (1 + Math.exp(-1 * steepness * (adjustedX - halfX))) + y1;
	}

	@Override
	int getNoteMs() {
		return noteMs;
	}

	@Override
	double getStartPitch() {
		return y1;
	}

	@Override
	double getEndPitch() {
		return y2;
	}
}