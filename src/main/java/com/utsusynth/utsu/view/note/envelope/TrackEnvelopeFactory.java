package com.utsusynth.utsu.view.note.envelope;

import com.google.inject.Inject;
import com.utsusynth.utsu.common.quantize.QuantizedEnvelope;
import com.utsusynth.utsu.common.quantize.QuantizedNote;
import com.utsusynth.utsu.common.quantize.Quantizer;
import com.utsusynth.utsu.view.note.TrackNote;

import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;

public class TrackEnvelopeFactory {
	private final Quantizer quantizer;

	@Inject
	public TrackEnvelopeFactory(Quantizer quantizer) {
		this.quantizer = quantizer;
	}

	public TrackEnvelope createEnvelope(
			TrackNote note,
			QuantizedEnvelope qEnvelope,
			TrackEnvelopeCallback callback) {
		QuantizedNote qNote = note.getQuantizedNote();
		int noteQuantSize = quantizer.getColWidth() / qNote.getQuantization();
		int startPos = (qNote.getStart() - qEnvelope.getPreutterance()) * noteQuantSize;
		int endPos = startPos + (qEnvelope.getLength() * noteQuantSize);

		int envQuantSize = quantizer.getColWidth() / QuantizedEnvelope.QUANTIZATION;
		int p1 = qEnvelope.getWidth(0) * envQuantSize;
		int p2 = qEnvelope.getWidth(1) * envQuantSize;
		int p3 = qEnvelope.getWidth(2) * envQuantSize;
		int p4 = qEnvelope.getWidth(3) * envQuantSize;
		int p5 = qEnvelope.getWidth(4) * envQuantSize;

		double v1 = 100 - (qEnvelope.getHeight(0) / 2.0);
		double v2 = 100 - (qEnvelope.getHeight(1) / 2.0);
		double v3 = 100 - (qEnvelope.getHeight(2) / 2.0);
		double v4 = 100 - (qEnvelope.getHeight(3) / 2.0);
		double v5 = 100 - (qEnvelope.getHeight(4) / 2.0);

		return new TrackEnvelope(
				new MoveTo(startPos, 100),
				new LineTo(startPos + p1, v1),
				new LineTo(startPos + p1 + p2, v2),
				new LineTo(startPos + p1 + p2 + p5, v5),
				new LineTo(endPos - p4 - p3, v3),
				new LineTo(endPos - p4, v4),
				new LineTo(endPos, 100),
				callback,
				quantizer);
	}
}
