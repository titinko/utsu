package com.utsusynth.utsu.common.data;

import com.utsusynth.utsu.common.quantize.QuantizedEnvelope;
import com.utsusynth.utsu.common.quantize.Quantizer;

public class EnvelopeData {
    private final double[] widths;
    private final double[] heights;

    public static EnvelopeData fromQuantized(QuantizedEnvelope qEnvelope) {
        int quantSize = Quantizer.DEFAULT_NOTE_DURATION / QuantizedEnvelope.QUANTIZATION;
        double[] envWidths = new double[5];
        double[] envHeights = new double[5];
        for (int i = 0; i < 5; i++) {
            envWidths[i] = qEnvelope.getWidth(i) * quantSize;
            envHeights[i] = qEnvelope.getHeight(i);
        }
        return new EnvelopeData(envWidths, envHeights);
    }

    public EnvelopeData(double[] envWidths, double[] envHeights) {
        this.widths = envWidths;
        this.heights = envHeights;
    }

    public double[] getWidths() {
        return widths;
    }

    public double[] getHeights() {
        return heights;
    }

    public QuantizedEnvelope quantize(double envPreutter, double envLength) {
        int quantSize = Quantizer.DEFAULT_NOTE_DURATION / QuantizedEnvelope.QUANTIZATION;
        double[] quantizedWidth = new double[5];
        for (int i = 0; i < 5; i++) {
            quantizedWidth[i] = widths[i] / quantSize;
        }
        int preutter = (int) Math.round(envPreutter / quantSize);
        int length = (int) Math.round(envLength / quantSize);
        return new QuantizedEnvelope(preutter, length, quantizedWidth, heights);
    }
}
