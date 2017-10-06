package com.utsusynth.utsu.view.note;

import com.google.common.collect.ImmutableList;
import com.utsusynth.utsu.view.note.pitch.Pitch;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;

public class TrackPitchbend {
	private final ImmutableList<Pitch> lines;
	private final ImmutableList<Rectangle> squares; // Control points.
	private final Group group;

	TrackPitchbend(ImmutableList<Pitch> lines) {
		ImmutableList.Builder<Rectangle> squareBuilder = ImmutableList.builder();
		if (!lines.isEmpty()) {
			// Add start and end control points.
			Pitch first = lines.get(0);
			Rectangle startSquare =
					new Rectangle(first.getStartX() - 2, first.getStartY() - 2, 4, 4);
			first.bindStart(startSquare.xProperty(), startSquare.yProperty());
			squareBuilder.add(startSquare);
			Pitch last = lines.get(lines.size() - 1);
			Rectangle endSquare = new Rectangle(last.getEndX() - 2, last.getEndY() - 2, 4, 4);
			last.bindEnd(endSquare.xProperty(), endSquare.yProperty());
			squareBuilder.add(endSquare);
			if (lines.size() > 1) {
				// Add middle control points.
				for (int i = 1; i < lines.size(); i++) {
					Pitch pitch = lines.get(i);
					Rectangle square =
							new Rectangle(pitch.getStartX() - 2, pitch.getStartY() - 2, 4, 4);
					lines.get(i - 1).bindEnd(square.xProperty(), square.yProperty());
					pitch.bindStart(square.xProperty(), square.yProperty());
					squareBuilder.add(square);
				}
			}
		}
		this.lines = lines;
		this.squares = squareBuilder.build();
		this.group = new Group();
		for (Pitch line : this.lines) {
			Shape shape = line.getElement();
			shape.setStroke(Color.DARKSLATEBLUE);
			shape.setFill(Color.TRANSPARENT);
			this.group.getChildren().add(shape);
		}
		for (Rectangle square : squares) {
			square.setStroke(Color.DARKSLATEBLUE);
			square.setFill(Color.TRANSPARENT);
			this.group.getChildren().add(square);
		}
	}

	public Group getElement() {
		return group;
	}
}
