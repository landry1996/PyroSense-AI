package com.pyrosense.notification.adapter.in.rest;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class NotificationSecurityTest {

    @Test
    void listEndpoint_restrictedToAdminAndManagerRoles() throws NoSuchMethodException {
        Method method = NotificationController.class.getDeclaredMethod("list", String.class, int.class, int.class);
        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
        assertNotNull(annotation);
        assertTrue(annotation.value().contains("PLATFORM_ADMIN"));
        assertTrue(annotation.value().contains("TENANT_ADMIN"));
        assertTrue(annotation.value().contains("PROPERTY_MANAGER"));
        assertTrue(annotation.value().contains("SUPPORT_READONLY"));
        assertFalse(annotation.value().contains("ELECTRICIAN"));
        assertFalse(annotation.value().contains("OCCUPANT"));
        assertFalse(annotation.value().contains("INSURANCE_PARTNER"));
    }

    @Test
    void recipientEndpoint_allowsElectricianAndOccupant() throws NoSuchMethodException {
        Method method = NotificationController.class.getDeclaredMethod("listByRecipient",
                String.class, int.class, int.class);
        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
        assertNotNull(annotation);
        assertTrue(annotation.value().contains("ELECTRICIAN"));
        assertTrue(annotation.value().contains("OCCUPANT"));
    }

    @Test
    void retryEndpoint_restrictedToAdminOnly() throws NoSuchMethodException {
        Method method = NotificationController.class.getDeclaredMethod("retry", java.util.UUID.class);
        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
        assertNotNull(annotation);
        assertTrue(annotation.value().contains("PLATFORM_ADMIN"));
        assertTrue(annotation.value().contains("TENANT_ADMIN"));
        assertFalse(annotation.value().contains("ELECTRICIAN"));
        assertFalse(annotation.value().contains("OCCUPANT"));
        assertFalse(annotation.value().contains("SUPPORT_READONLY"));
    }

    @Test
    void statisticsEndpoint_excludesOccupantAndInsurer() throws NoSuchMethodException {
        Method method = NotificationController.class.getDeclaredMethod("statistics");
        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
        assertNotNull(annotation);
        assertTrue(annotation.value().contains("SUPPORT_READONLY"));
        assertFalse(annotation.value().contains("OCCUPANT"));
        assertFalse(annotation.value().contains("INSURANCE_PARTNER"));
    }

    @Test
    void getByIdEndpoint_excludesElectricianAndOccupant() throws NoSuchMethodException {
        Method method = NotificationController.class.getDeclaredMethod("getById", java.util.UUID.class);
        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
        assertNotNull(annotation);
        assertFalse(annotation.value().contains("ELECTRICIAN"));
        assertFalse(annotation.value().contains("OCCUPANT"));
    }

    @Test
    void noSecretFieldsInResponse() {
        var fields = NotificationController.NotificationResponse.class.getRecordComponents();
        for (var field : fields) {
            String name = field.getName().toLowerCase();
            assertFalse(name.contains("password"));
            assertFalse(name.contains("secret"));
            assertFalse(name.contains("token"));
        }
    }

    @Test
    void preferencesEndpoints_requireAuthentication() throws NoSuchMethodException {
        Method getMe = NotificationPreferencesController.class.getDeclaredMethod(
                "getMyPreferences", org.springframework.security.core.Authentication.class);
        PreAuthorize ann = getMe.getAnnotation(PreAuthorize.class);
        assertNotNull(ann);
        assertTrue(ann.value().contains("isAuthenticated()"));
    }

    @Test
    void preferencesAdminEndpoints_restrictedToAdmin() throws NoSuchMethodException {
        Method getOther = NotificationPreferencesController.class.getDeclaredMethod(
                "getPreferences", String.class);
        PreAuthorize ann = getOther.getAnnotation(PreAuthorize.class);
        assertNotNull(ann);
        assertTrue(ann.value().contains("PLATFORM_ADMIN"));
        assertTrue(ann.value().contains("TENANT_ADMIN"));
        assertFalse(ann.value().contains("PROPERTY_MANAGER"));
        assertFalse(ann.value().contains("ELECTRICIAN"));
    }

    @Test
    void tenantPolicyEndpoints_restrictedToAdmin() throws NoSuchMethodException {
        Method getPolicy = NotificationPreferencesController.class.getDeclaredMethod(
                "getTenantPolicy", String.class);
        PreAuthorize ann = getPolicy.getAnnotation(PreAuthorize.class);
        assertNotNull(ann);
        assertTrue(ann.value().contains("PLATFORM_ADMIN"));
        assertTrue(ann.value().contains("TENANT_ADMIN"));
    }
}
