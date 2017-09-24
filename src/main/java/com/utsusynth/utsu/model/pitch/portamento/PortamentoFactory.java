package com.utsusynth.utsu.model.pitch.portamento;

public class PortamentoFactory {
	public Portamento makePortamento(double x1, double y1, double x2, double y2, String shape) {
		// Corner cases.
		if (y1 == y2) {
			// A flat portamento may as well be linear.
			return new LinearPortamento(x1, y1, x2, y2);
		} else if (x1 >= x2) {
			// TODO: Handle this better.
			System.out.println("Tried to enter a portamento of length 0. :(");
			return new LinearPortamento(x2, y1, x1 + 0.1, y1);
		}

		if (shape.equalsIgnoreCase("s")) {
			return new LinearPortamento(x1, y1, x2, y2);
		} else if (shape.equalsIgnoreCase("r")) {
			return new LogarithmicPortamento(x1, y1, x2, y2);
		} else if (shape.equalsIgnoreCase("j")) {
			return new QuadraticPortamento(x1, y1, x2, y2);
		} else if (shape.equals("")) {
			return new LogisticPortamento(x1, y1, x2, y2);
		} else {
			// TODO: Handle this.
			System.out.println("Unrecognized portamento shape.");
			return null;
		}
	}
}
