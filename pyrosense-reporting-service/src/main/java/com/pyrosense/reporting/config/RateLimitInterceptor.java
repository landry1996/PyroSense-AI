package com.pyrosense.reporting.config;

import com.pyrosense.shared.security.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final int MAX_REPORT_GENERATIONS_PER_MINUTE = 5;
    private static final long WINDOW_MS = 60_000;

    private final Map<String, RateBucket> buckets = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!"POST".equals(request.getMethod())) {
            return true;
        }
        if (!request.getRequestURI().startsWith("/api/v1/reports")) {
            return true;
        }

        String key = resolveKey(request);
        RateBucket bucket = buckets.compute(key, (k, existing) -> {
            long now = System.currentTimeMillis();
            if (existing == null || now - existing.windowStart > WINDOW_MS) {
                return new RateBucket(now, new AtomicInteger(1));
            }
            existing.count.incrementAndGet();
            return existing;
        });

        if (bucket.count.get() > MAX_REPORT_GENERATIONS_PER_MINUTE) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", "60");
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\":\"Rate limit exceeded. Maximum %d report generations per minute.\"}".formatted(
                            MAX_REPORT_GENERATIONS_PER_MINUTE));
            return false;
        }
        return true;
    }

    private String resolveKey(HttpServletRequest request) {
        return TenantContext.get()
                .map(tid -> "tenant:" + tid.value().toString())
                .orElse("ip:" + request.getRemoteAddr());
    }

    private record RateBucket(long windowStart, AtomicInteger count) {}
}
