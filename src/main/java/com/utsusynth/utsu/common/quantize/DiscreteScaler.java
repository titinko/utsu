package com.utsusynth.utsu.common.quantize;

import com.google.common.collect.ImmutableList;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;

public class DiscreteScaler implements Scaler {
    public static final ImmutableList<Double> HORIZONTAL_SCALES =
            ImmutableList.of(0.05, 0.1, 0.15, 0.2, 0.25, 0.3);

    public static final ImmutableList<Double> VERTICAL_SCALES =
            ImmutableList.of(0.85, 1.0, 1.15, 1.3, 1.75);

    private final IntegerProperty horizontalRank;
    private final DoubleProperty horizontalScale;
    private final IntegerProperty verticalRank;
    private final DoubleProperty verticalScale;

    public DiscreteScaler(int defaultHorizontalRank, int defaultVerticalRank) {
        horizontalRank = new SimpleIntegerProperty(defaultHorizontalRank);
        horizontalScale = new SimpleDoubleProperty(HORIZONTAL_SCALES.get(horizontalRank.get()));
        horizontalRank.addListener(obs -> {
            horizontalScale.set(HORIZONTAL_SCALES.get(horizontalRank.getValue()));
        });
        verticalRank = new SimpleIntegerProperty(defaultVerticalRank);
        verticalScale = new SimpleDoubleProperty(VERTICAL_SCALES.get(verticalRank.get()));
        verticalRank.addListener(obs -> {
            verticalScale.set(VERTICAL_SCALES.get(verticalRank.getValue()));
        });
    }
    
    @Override
    public DoubleBinding scaleX(int scaleMe) {
        return horizontalScale.multiply(scaleMe);
    }

    @Override
    public DoubleBinding scaleX(double scaleMe) {
        return horizontalScale.multiply(scaleMe);
    }

    @Override
    public DoubleBinding scalePos(int scaleMe) {
        return horizontalScale.multiply(Quantizer.COL_WIDTH * 4 + scaleMe);
    }

    @Override
    public DoubleBinding scalePos(double scaleMe) {
        return horizontalScale.multiply(Quantizer.COL_WIDTH * 4 + scaleMe);
    }

    @Override
    public DoubleBinding scaleY(int scaleMe) {
        return verticalScale.multiply(scaleMe);
    }

    @Override
    public DoubleBinding scaleY(double scaleMe) {
        return verticalScale.multiply(scaleMe);
    }

    @Override
    public double unscaleX(double unscaleMe) {
        return unscaleMe / horizontalScale.get();
    }

    @Override
    public double unscalePos(double unscaleMe) {
        return (unscaleMe / horizontalScale.get()) - (Quantizer.COL_WIDTH * 4);
    }

    @Override
    public double unscaleY(double unscaleMe) {
        return unscaleMe / verticalScale.get();
    }

    /**
     * Create a derivative scaler by applying multipliers to the current scales.
     */
    @Override
    public ContinuousScaler derive(double horizontalMultiplier, double verticalMultiplier) {
        return new ContinuousScaler(
                horizontalScale.get() * horizontalMultiplier,
                verticalScale.get() * verticalMultiplier);
    }

    public int getHorizontalRank() {
        return horizontalRank.get();
    }

    /** Updates horizontal scale and returns whether update was successful. */
    public boolean changeHorizontalScale(int oldRank, int newRank) {
        if (oldRank != horizontalRank.get()) {
            // TODO: Handle this better.
            System.out.println("ERROR: Data race when changing horizontal scale!");
        } else if (newRank < 0 || newRank >= HORIZONTAL_SCALES.size()) {
            return false;
        }
        horizontalRank.set(newRank);
        return true;
    }

    public int getVerticalRank() {
        return verticalRank.get();
    }

    /** Updates vertical scale and returns whether update was successful. */
    public boolean changeVerticalScale(int oldRank, int newRank) {
        if (oldRank != verticalRank.get()) {
            // TODO: Handle this better.
            System.out.println("ERROR: Data race when changing vertical scale!");
        } else if (newRank < 0 || newRank >= VERTICAL_SCALES.size()) {
            return false;
        }
        verticalRank.set(newRank);
        return true;
    }
}
