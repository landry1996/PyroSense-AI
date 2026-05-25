package com.pyrosense.shared.valueobject;

import com.pyrosense.shared.domain.ValueObject;

/**
 * A percentage value between 0.0 and 100.0 (inclusive).
 */
public record Percentage(double value) implements ValueObject, Comparable<Percentage> {

    public Percentage {
        if (value < 0.0 || value > 100.0) {
            throw new IllegalArgumentException(
                    "Percentage must be between 0.0 and 100.0, got: " + value);
        }
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException("Percentage must be a finite number");
        }
    }

    public static Percentage of(double value) {
        return new Percentage(value);
    }

    public static Percentage zero() {
        return new Percentage(0.0);
    }

    public static Percentage full() {
        return new Percentage(100.0);
    }

    /**
     * Returns the percentage as a fraction (0.0 to 1.0).
     */
    public double asFraction() {
        return value / 100.0;
    }

    @Override
    public int compareTo(Percentage other) {
        return Double.compare(this.value, other.value);
    }
}
