package com.utsusynth.utsu.model.song.pitch.portamento;

/** Represents an "r"-shaped portamento. */
class LogarithmicPortamento extends Portamento {
	private final double x1;
	private final double y1;
	private final double x2;
	private final double y2;
	private final double yStretch; // a in aln(bx) + c
	private final double xStretch; // b in aln(bx) + c
	private final double constant; // c in aln(bx) + c

	LogarithmicPortamento(double x1, double y1, double x2, double y2) {
		this.x1 = x1;
		this.y1 = y1;
		this.x2 = x2;
		this.y2 = y2;
		this.yStretch = (y2 - y1) / 6;
		this.xStretch = 20 / (x2 - x1);
		this.constant = (y2 - y1) / 2;
	}

	@Override
	public double apply(int positionMs) {
		if (positionMs < x1 || positionMs > x2) {
			// TODO: Handle this.
			System.out.println("Tried to apply a logarithmic portamento that doesn't exist here.");
			return 0.0;
		}
		// Don't get a divide-by-zero error and don't let pitch go beyond y1.
		double adjustedX = positionMs - x1;
		if (adjustedX == 0.0) {
			return y1;
		}
		double pitch = (yStretch * Math.log(adjustedX * xStretch)) + constant + y1;
		if ((y2 > y1 && y1 > pitch) || (y1 > y2 && pitch > y1)) {
			return y1;
		}
		return pitch;
	}

	@Override
	public double getStartPitch() {
		return y1;
	}

	@Override
	public double getEndPitch() {
		return y2;
	}
}
