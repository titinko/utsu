package com.utsusynth.utsu.common.utils;

/**
 * Utitily class representing a complex number.
 */
public class Complex {
    private final double real;
    private final double imaginary;

    public Complex(double real, double imaginary) {
        this.real = real;
        this.imaginary = imaginary;
    }

    public double getReal() {
        return real;
    }

    public double getImaginary() {
        return imaginary;
    }

    public Complex add(Complex other) {
        return new Complex(this.real + other.real, this.imaginary + other.imaginary);
    }

    public Complex subtract(Complex other) {
        return new Complex(this.real - other.real, this.imaginary - other.imaginary);
    }

    public Complex multiply(Complex other) {
        double real = (this.real * other.real) - (this.imaginary * other.imaginary);
        double imaginary = (this.real * other.imaginary) + (this.imaginary * other.real);
        return new Complex(real, imaginary);
    }

    public Complex divide(Complex other) {
        // Multiply by the reciprocal.
        double scale = (other.real * other.real) + (other.imaginary * other.imaginary);
        return this.multiply(
                new Complex(other.real / scale, -other.imaginary / scale));
    }

    @Override
    public String toString() {
        if (imaginary == 0) {
            return Double.toString(real);
        } else if (real == 0) {
            return imaginary + "i";
        } else {
            return real + " " + (imaginary > 0 ? "+ " : "-") + imaginary + "i";
        }
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Complex)) {
            return false;
        }
        Complex otherComplex = (Complex) other;
        return this.real == otherComplex.real && this.imaginary == otherComplex.imaginary;
    }
}
