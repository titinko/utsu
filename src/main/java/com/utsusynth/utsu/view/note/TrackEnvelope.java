package com.utsusynth.utsu.view.note;

import com.utsusynth.utsu.common.quantize.QuantizedEnvelope;

import javafx.scene.Group;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;

public class TrackEnvelope {
	private static final int COL_WIDTH = 96;

	private final MoveTo start;
	private final LineTo[] lines;
	private final Circle[] circles; // Control points.
	private final LineTo end;
	private final Group group;

	TrackEnvelope(
			MoveTo start,
			LineTo l1,
			LineTo l2,
			LineTo l3,
			LineTo l4,
			LineTo l5,
			LineTo end,
			TrackEnvelopeCallback callback) {
		this.start = start;
		this.lines = new LineTo[] { l1, l2, l3, l4, l5 };
		this.circles = new Circle[5];
		for (int i = 0; i < 5; i++) {
			Circle circle = new Circle(lines[i].getX(), lines[i].getY(), 3);
			circle.setFill(Paint.valueOf("yellow"));
			lines[i].xProperty().bind(circle.centerXProperty());
			lines[i].yProperty().bind(circle.centerYProperty());
			final int index = i;
			circle.setOnMouseDragged((event) -> {
				boolean changed = false;
				// Set reasonable limits for where envelope can be dragged.
				if (index > 0 && index < 4) {
					double newX = event.getX();
					if (newX > lines[index - 1].getX() && newX < lines[index + 1].getX()) {
						changed = true;
						circle.setCenterX(newX);
					}
				}
				double newY = event.getY();
				if (newY >= 0 && newY <= 100) {
					changed = true;
					circle.setCenterY(newY);
				}

				if (changed) {
					callback.modifySongEnvelope(getQuantizedEnvelope());
				}
			});
			circles[i] = circle;
		}
		this.end = end;
		Path path = new Path(start, lines[0], lines[1], lines[2], lines[3], lines[4], end);
		path.setStroke(Paint.valueOf("yellow"));
		this.group = new Group(path, circles[0], circles[1], circles[2], circles[4], circles[3]);
	}

	public Group getElement() {
		return group;
	}

	public QuantizedEnvelope getQuantizedEnvelope() {
		int envQuantSize = COL_WIDTH / QuantizedEnvelope.QUANTIZATION;
		double[] widths = new double[5];
		widths[0] = (lines[0].getX() - start.getX()) / envQuantSize;
		widths[1] = (lines[1].getX() - lines[0].getX()) / envQuantSize;
		widths[2] = (lines[4].getX() - lines[3].getX()) / envQuantSize;
		widths[3] = (end.getX() - lines[4].getX()) / envQuantSize;
		widths[4] = (lines[2].getX() - lines[1].getX()) / envQuantSize;

		double[] heights = new double[5];
		heights[0] = 200 - (lines[0].getY() * 2);
		heights[1] = 200 - (lines[1].getY() * 2);
		heights[2] = 200 - (lines[3].getY() * 2);
		heights[3] = 200 - (lines[4].getY() * 2);
		heights[4] = 200 - (lines[2].getY() * 2);
		return new QuantizedEnvelope(widths, heights);
	}
}
