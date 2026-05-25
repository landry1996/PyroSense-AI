package com.pyrosense.identity.config;

import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.id.UserId;
import com.pyrosense.shared.security.AuditEntry;
import com.pyrosense.shared.security.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

@Aspect
@Component
public class AuditConfig {

    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT");

    @Around("@annotation(audited)")
    public Object auditAction(ProceedingJoinPoint joinPoint, Audited audited) throws Throwable {
        Object result = joinPoint.proceed();

        try {
            String action = audited.value();
            UserId userId = extractUserId();
            TenantId tenantId = TenantContext.get().orElse(null);
            String ipAddress = extractIpAddress();
            String userAgent = extractUserAgent();

            AuditEntry entry = AuditEntry.create(action, audited.resourceType(), "",
                    userId, tenantId, ipAddress, userAgent, null);

            auditLog.info("AUDIT | action={} | user={} | tenant={} | ip={} | ua={}",
                    entry.action(),
                    entry.userId() != null ? entry.userId().value() : "anonymous",
                    entry.tenantId() != null ? entry.tenantId().value() : "none",
                    entry.ipAddress(),
                    entry.userAgent());
        } catch (Exception e) {
            auditLog.warn("Failed to record audit entry: {}", e.getMessage());
        }

        return result;
    }

    private UserId extractUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            String sub = jwtAuth.getToken().getSubject();
            if (sub != null) {
                try {
                    return new UserId(UUID.fromString(sub));
                } catch (IllegalArgumentException ignored) {}
            }
        }
        return null;
    }

    private String extractIpAddress() {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletAttrs) {
            HttpServletRequest request = servletAttrs.getRequest();
            String xff = request.getHeader("X-Forwarded-For");
            return xff != null ? xff.split(",")[0].trim() : request.getRemoteAddr();
        }
        return null;
    }

    private String extractUserAgent() {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletAttrs) {
            return servletAttrs.getRequest().getHeader("User-Agent");
        }
        return null;
    }
}
