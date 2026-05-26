package com.pyrosense.reporting.config;

import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitInterceptorTest {

    private RateLimitInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new RateLimitInterceptor();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void allowsGetRequestsWithoutRateLimit() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/v1/reports");
        var response = new MockHttpServletResponse();
        assertTrue(interceptor.preHandle(request, response, new Object()));
    }

    @Test
    void allowsFirstFivePostRequests() throws Exception {
        TenantContext.set(new TenantId(UUID.randomUUID()));
        for (int i = 0; i < 5; i++) {
            var request = new MockHttpServletRequest("POST", "/api/v1/reports/monthly-health");
            var response = new MockHttpServletResponse();
            assertTrue(interceptor.preHandle(request, response, new Object()),
                    "Request " + (i + 1) + " should be allowed");
        }
    }

    @Test
    void blocksSixthPostRequest() throws Exception {
        TenantId tenant = new TenantId(UUID.randomUUID());
        TenantContext.set(tenant);
        for (int i = 0; i < 5; i++) {
            var request = new MockHttpServletRequest("POST", "/api/v1/reports/monthly-health");
            var response = new MockHttpServletResponse();
            interceptor.preHandle(request, response, new Object());
        }

        var request = new MockHttpServletRequest("POST", "/api/v1/reports/monthly-health");
        var response = new MockHttpServletResponse();
        assertFalse(interceptor.preHandle(request, response, new Object()));
        assertEquals(429, response.getStatus());
        assertEquals("60", response.getHeader("Retry-After"));
    }

    @Test
    void differentTenantsHaveSeparateBuckets() throws Exception {
        TenantId tenantA = new TenantId(UUID.randomUUID());
        TenantId tenantB = new TenantId(UUID.randomUUID());

        TenantContext.set(tenantA);
        for (int i = 0; i < 5; i++) {
            var request = new MockHttpServletRequest("POST", "/api/v1/reports/monthly-health");
            interceptor.preHandle(request, new MockHttpServletResponse(), new Object());
        }

        TenantContext.set(tenantB);
        var request = new MockHttpServletRequest("POST", "/api/v1/reports/monthly-health");
        var response = new MockHttpServletResponse();
        assertTrue(interceptor.preHandle(request, response, new Object()));
    }

    @Test
    void nonReportPathsNotRateLimited() throws Exception {
        TenantContext.set(new TenantId(UUID.randomUUID()));
        for (int i = 0; i < 10; i++) {
            var request = new MockHttpServletRequest("POST", "/api/v1/other");
            var response = new MockHttpServletResponse();
            assertTrue(interceptor.preHandle(request, response, new Object()));
        }
    }
}
