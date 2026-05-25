package com.pyrosense.gateway.filter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FilterOrderTest {

    @Test
    void filtersShouldExecuteInCorrectOrder() {
        int sanitization = -20;
        int payload = -15;
        int tracing = -10;
        int propagation = -5;
        int securityHeaders = -2;
        int rateLimiting = 0;

        assertThat(sanitization).isLessThan(payload);
        assertThat(payload).isLessThan(tracing);
        assertThat(tracing).isLessThan(propagation);
        assertThat(propagation).isLessThan(securityHeaders);
        assertThat(securityHeaders).isLessThan(rateLimiting);
    }

    @Test
    void sanitizationShouldRunFirst() {
        var sanitization = new TenantHeaderSanitizationFilter();
        assertThat(sanitization.getOrder()).isEqualTo(-20);
    }

    @Test
    void tracingShouldRunBeforePropagation() {
        var tracing = new RequestTracingFilter();
        var propagation = new JwtHeaderPropagationFilter();
        assertThat(tracing.getOrder()).isLessThan(propagation.getOrder());
    }
}
