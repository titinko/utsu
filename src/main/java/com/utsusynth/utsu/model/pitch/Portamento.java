package com.utsusynth.utsu.model.pitch;

// TODO: Move/fix this.
class PortamentoFactory {
	Portamento makePortamento(
			int noteMs, double x1, double y1, double x2, double y2, String shape) {
		// Corner cases.
		if (y1 == y2) {
			// A flat portamento may as well be linear.
			return new LinearPortamento(noteMs, x1, y1, x2, y2);
		} else if (x1 >= x2) {
			// TODO: Handle this better.
			System.out.println("Tried to enter a portamento of length 0. :(");
			return new LinearPortamento(noteMs, x2, y1, x1 + 0.1, y1);
		}
		
		if (shape.equalsIgnoreCase("s")) {
			return new LinearPortamento(noteMs, x1, y1, x2, y2);
		} else if (shape.equalsIgnoreCase("r")) {
			return new LogarithmicPortamento(noteMs, x1, y1, x2, y2);
		} else if (shape.equalsIgnoreCase("j")) {
			return new QuadraticPortamento(noteMs, x1, y1, x2, y2);
		} else if (shape.equals("")) {
			return new LogisticPortamento(noteMs, x1, y1, x2, y2);
		} else {
			// TODO: Handle this.
			System.out.println("Unrecognized portamento shape.");
			return null;
		}
	}
}

abstract class Portamento implements PitchMutation {
	// Gets the absolute start position (in ms) of the note linked with this portamento.
	abstract int getNoteMs();
	
	// Gets the first pitch in the portamento.
	abstract double getStartPitch();
	
	// Gets the last pitch in the portamento.
	abstract double getEndPitch();
}
