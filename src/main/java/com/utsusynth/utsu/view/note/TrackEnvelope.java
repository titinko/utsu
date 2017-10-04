package com.utsusynth.utsu.view.note;

import com.utsusynth.utsu.common.quantize.QuantizedEnvelope;

import javafx.scene.paint.Paint;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;

public class TrackEnvelope {
	private static final int COL_WIDTH = 96;

	private final MoveTo start;
	private final LineTo l1;
	private final LineTo l2;
	private final LineTo l3;
	private final LineTo l4;
	private final LineTo l5;
	private final LineTo end;
	private final Path path;

	TrackEnvelope(MoveTo start, LineTo l1, LineTo l2, LineTo l3, LineTo l4, LineTo l5, LineTo end) {
		this.start = start;
		this.l1 = l1;
		this.l2 = l2;
		this.l3 = l3;
		this.l4 = l4;
		this.l5 = l5;
		this.end = end;
		this.path = new Path(start, l1, l2, l3, l4, l5, end);
		path.setStroke(Paint.valueOf("yellow"));
	}

	public Path getElement() {
		return path;
	}

	public QuantizedEnvelope getQuantizedEnvelope() {
		int envQuantSize = COL_WIDTH / QuantizedEnvelope.QUANTIZATION;
		double[] widths = new double[5];
		widths[0] = (l1.getX() - start.getX()) / envQuantSize;
		widths[1] = (l2.getX() - l1.getX()) / envQuantSize;
		widths[2] = (l5.getX() - l4.getX()) / envQuantSize;
		widths[3] = (end.getX() - l5.getX()) / envQuantSize;
		widths[4] = (l3.getX() - l2.getX()) / envQuantSize;

		double[] heights = new double[5];
		heights[0] = 200 - (l1.getY() * 2);
		heights[1] = 200 - (l2.getY() * 2);
		heights[2] = 200 - (l4.getY() * 2);
		heights[3] = 200 - (l5.getY() * 2);
		heights[4] = 200 - (l3.getY() * 2);
		return new QuantizedEnvelope(widths, heights);
	}
}
