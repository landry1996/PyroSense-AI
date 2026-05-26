package com.pyrosense.notification.adapter.in.rest;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class AuditLogSecurityTest {

    @Test
    void controllerHasClassLevelSecurityAnnotation() {
        PreAuthorize annotation = AuditLogController.class.getAnnotation(PreAuthorize.class);
        assertNotNull(annotation, "AuditLogController must have @PreAuthorize");
        assertTrue(annotation.value().contains("PLATFORM_ADMIN"));
        assertTrue(annotation.value().contains("TENANT_ADMIN"));
    }

    @Test
    void controllerRequiresAdminRolesOnly() {
        PreAuthorize annotation = AuditLogController.class.getAnnotation(PreAuthorize.class);
        assertNotNull(annotation);
        assertFalse(annotation.value().contains("ELECTRICIAN"));
        assertFalse(annotation.value().contains("PROPERTY_MANAGER"));
        assertFalse(annotation.value().contains("OCCUPANT"));
    }

    @Test
    void listEndpointIsMappedToGetAuditLogs() throws NoSuchMethodException {
        RequestMapping classMapping = AuditLogController.class.getAnnotation(RequestMapping.class);
        assertNotNull(classMapping);
        assertEquals("/api/v1/audit-logs", classMapping.value()[0]);
    }

    @Test
    void listEndpointAcceptsFilterParameters() throws NoSuchMethodException {
        Method listMethod = AuditLogController.class.getDeclaredMethod("list",
                String.class, String.class, String.class, String.class, int.class, int.class);
        assertNotNull(listMethod);
        GetMapping getMapping = listMethod.getAnnotation(GetMapping.class);
        assertNotNull(getMapping);
    }

    @Test
    void getByIdEndpointExists() throws NoSuchMethodException {
        Method method = AuditLogController.class.getDeclaredMethod("getById", String.class);
        assertNotNull(method);
    }

    @Test
    void noSecretFieldsExposedInResponse() {
        var fields = AuditLogController.AuditLogResponse.class.getRecordComponents();
        for (var field : fields) {
            assertFalse(field.getName().toLowerCase().contains("password"));
            assertFalse(field.getName().toLowerCase().contains("secret"));
            assertFalse(field.getName().toLowerCase().contains("token"));
        }
    }

    @Test
    void responseDoesNotExposeUserAgent() {
        var fields = AuditLogController.AuditLogResponse.class.getRecordComponents();
        for (var field : fields) {
            assertNotEquals("userAgent", field.getName(),
                    "userAgent should not be in the response to minimize PII exposure");
        }
    }

    @Test
    void paginationResponseIncludesTotalCount() {
        var fields = AuditLogController.AuditLogPageResponse.class.getRecordComponents();
        boolean hasTotalCount = false;
        boolean hasTotalPages = false;
        for (var field : fields) {
            if (field.getName().equals("totalCount")) hasTotalCount = true;
            if (field.getName().equals("totalPages")) hasTotalPages = true;
        }
        assertTrue(hasTotalCount, "Page response must include totalCount");
        assertTrue(hasTotalPages, "Page response must include totalPages");
    }
}
