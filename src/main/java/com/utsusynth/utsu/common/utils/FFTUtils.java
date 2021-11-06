package com.utsusynth.utsu.common.utils;

public class FFTUtils {
    /**
     * Compute forward FFT of a signal, with frequency on the real axis and amplitude on the
     * imaginary axis.
     */
    public static Complex[] fft(Complex[] signal) {
        if (signal.length == 1) {
            return new Complex[] {signal[0]};
        }

        if (signal.length % 2 != 0) {
            System.out.println("Error: called FFT for a signal that is not a power of 2.");
        }

        // Compute FFT on all even samples.
        int halfLength = signal.length / 2;
        Complex[] evenSignal = new Complex[halfLength];
        for (int i = 0; i < halfLength; i++) {
            evenSignal[i] = signal[2 * i];
        }
        Complex[] evenResult = fft(evenSignal);

        // Compute FFT on all odd samples.
        Complex[] oddSignal = new Complex[halfLength];
        for (int i = 0; i < halfLength; i++) {
            oddSignal[i] = signal[2 * i + 1];
        }
        Complex[] oddResult = fft(oddSignal);

        // Combine even and odd results.
        Complex[] result = new Complex[signal.length];
        for (int i = 0; i < halfLength; i++) {
            double twiddle = -2 * Math.PI * i / signal.length; // Twiddle factor.
            Complex coefficient = new Complex(Math.cos(twiddle), Math.sin(twiddle));
            result[i] = evenResult[i].add(coefficient.multiply(oddResult[i]));
            result[halfLength + i] = evenResult[i].subtract(coefficient.multiply(oddResult[i]));
        }
        return result;
    }

    /**
     * Apply a standard hamming window to a discrete signal so FFT can read frequencies better.
     */
    public static double[] hammingWindow(double[] signal, int startIndex, int length) {
        double[] result = new double[length];
        for (int i = startIndex; i < startIndex + length; i++) {
            result[i - startIndex] =
                    signal[i] * (0.54 - (0.46 * Math.cos(2.0 * i * Math.PI / length)));
        }
        return result;
    }

    /**
     * Convert a double array to a Complex array.
     */
    public static Complex[] toComplex(double[] signal) {
        Complex[] result = new Complex[signal.length];
        for (int i = 0; i < signal.length; i++) {
            result[i] = new Complex(signal[i], 0);
        }
        return result;
    }
}
