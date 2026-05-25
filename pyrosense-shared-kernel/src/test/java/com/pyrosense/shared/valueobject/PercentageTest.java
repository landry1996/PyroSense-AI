package com.pyrosense.shared.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PercentageTest {

    @Test
    void shouldCreateValidPercentage() {
        var pct = Percentage.of(55.5);
        assertThat(pct.value()).isEqualTo(55.5);
    }

    @Test
    void shouldAllowBoundaryValues() {
        assertThat(Percentage.of(0.0).value()).isZero();
        assertThat(Percentage.of(100.0).value()).isEqualTo(100.0);
    }

    @ParameterizedTest
    @ValueSource(doubles = {-0.001, -1.0, 100.001, 200.0})
    void shouldRejectOutOfRange(double invalid) {
        assertThatThrownBy(() -> Percentage.of(invalid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Percentage must be between");
    }

    @Test
    void shouldRejectNaN() {
        assertThatThrownBy(() -> Percentage.of(Double.NaN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("finite number");
    }

    @Test
    void shouldRejectInfinity() {
        assertThatThrownBy(() -> Percentage.of(Double.POSITIVE_INFINITY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldConvertToFraction() {
        assertThat(Percentage.of(50.0).asFraction()).isEqualTo(0.5);
        assertThat(Percentage.of(100.0).asFraction()).isEqualTo(1.0);
        assertThat(Percentage.of(0.0).asFraction()).isZero();
    }

    @Test
    void shouldCompare() {
        assertThat(Percentage.of(80.0)).isGreaterThan(Percentage.of(50.0));
        assertThat(Percentage.of(25.0)).isLessThan(Percentage.of(75.0));
    }

    @Test
    void factoryMethodsShouldWork() {
        assertThat(Percentage.zero().value()).isZero();
        assertThat(Percentage.full().value()).isEqualTo(100.0);
    }
}
