package com.utsusynth.utsu.view.note;

import com.google.common.collect.ImmutableList;
import com.utsusynth.utsu.common.PitchUtils;
import com.utsusynth.utsu.common.quantize.QuantizedPitchbend;
import com.utsusynth.utsu.view.note.pitch.Pitch;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;

public class TrackPitchbend {
	private static final int ROW_HEIGHT = 20;
	private static final int COL_WIDTH = 96;

	private final ImmutableList<Pitch> lines;
	private final ImmutableList<Rectangle> squares; // Control points.
	private final Group group;

	TrackPitchbend(ImmutableList<Pitch> lines, TrackPitchbendCallback callback) {
		ImmutableList.Builder<Rectangle> squareBuilder = ImmutableList.builder();
		if (!lines.isEmpty()) {
			// Add all control points except the last.
			for (int i = 0; i < lines.size(); i++) {
				Pitch pitch = lines.get(i);
				Rectangle square =
						new Rectangle(pitch.getStartX() - 2, pitch.getStartY() - 2, 4, 4);
				if (i > 0) {
					lines.get(i - 1).bindEnd(square.xProperty(), square.yProperty());
				}
				pitch.bindStart(square.xProperty(), square.yProperty());
				squareBuilder.add(square);
			}
			// Add last control point.
			Pitch last = lines.get(lines.size() - 1);
			Rectangle endSquare = new Rectangle(last.getEndX() - 2, last.getEndY() - 2, 4, 4);
			last.bindEnd(endSquare.xProperty(), endSquare.yProperty());
			squareBuilder.add(endSquare);
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
		for (int i = 0; i < squares.size(); i++) {
			Rectangle square = squares.get(i);
			square.setStroke(Color.DARKSLATEBLUE);
			square.setFill(Color.TRANSPARENT);
			final int index = i;
			square.setOnMouseDragged(event -> {
				boolean changed = false;
				double newX = event.getX();
				if (index == 0) {
					if (newX > 0 && newX < squares.get(index + 1).getX() + 2) {
						changed = true;
						square.setX(newX - 2);
					}
				} else if (index == squares.size() - 1) {
					if (newX > squares.get(index - 1).getX() + 2) {
						changed = true;
						square.setX(newX - 2);
					}
				} else if (newX > squares.get(index - 1).getX() + 2
						&& newX < squares.get(index + 1).getX() + 2) {
					changed = true;
					square.setX(newX - 2);
				}

				if (index > 0 && index < squares.size() - 1) {
					double newY = event.getY();
					if (newY > 0 && newY < ROW_HEIGHT * 12 * 7) {
						changed = true;
						square.setY(newY - 2);
					}
				}
				if (changed) {
					callback.modifySongPitchbend(this);
				}
			});
			this.group.getChildren().add(square);
		}
	}

	public Group getElement() {
		return group;
	}

	public QuantizedPitchbend getQuantizedPitchbend(int notePos) {
		assert (lines.size() > 0);
		int pitchQuantSize = COL_WIDTH / QuantizedPitchbend.QUANTIZATION;
		String prevPitch = PitchUtils.rowNumToPitch((int) (lines.get(0).getStartY() / ROW_HEIGHT));
		int start = (int) ((lines.get(0).getStartX() - notePos) / pitchQuantSize);
		double endY = lines.get(lines.size() - 1).getEndY();
		ImmutableList.Builder<Integer> widths = ImmutableList.builder();
		ImmutableList.Builder<Double> heights = ImmutableList.builder();
		ImmutableList.Builder<String> shapes = ImmutableList.builder();
		for (int i = 0; i < lines.size(); i++) {
			Pitch line = lines.get(i);
			widths.add((int) (line.getEndX() - line.getStartX()) / pitchQuantSize);
			if (i < lines.size() - 1) {
				heights.add((endY - line.getEndY()) / ROW_HEIGHT * 10);
			}
			shapes.add(line.getType());
		}
		return new QuantizedPitchbend(
				prevPitch,
				start,
				widths.build(),
				heights.build(),
				shapes.build());
	}
}
