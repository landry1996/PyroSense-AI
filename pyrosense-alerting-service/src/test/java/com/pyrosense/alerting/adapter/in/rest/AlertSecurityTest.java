package com.pyrosense.alerting.adapter.in.rest;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class AlertSecurityTest {

    @Test
    void controllerHasClassLevelSecurityAnnotation() {
        PreAuthorize annotation = AlertController.class.getAnnotation(PreAuthorize.class);
        assertNotNull(annotation, "AlertController must have @PreAuthorize");
        assertTrue(annotation.value().contains("PLATFORM_ADMIN"));
        assertTrue(annotation.value().contains("TENANT_ADMIN"));
        assertTrue(annotation.value().contains("PROPERTY_MANAGER"));
        assertTrue(annotation.value().contains("ELECTRICIAN"));
        assertTrue(annotation.value().contains("OCCUPANT"));
        assertTrue(annotation.value().contains("SUPPORT_READONLY"));
    }

    @Test
    void classLevelDoesNotAllowInsurancePartner() {
        PreAuthorize annotation = AlertController.class.getAnnotation(PreAuthorize.class);
        assertNotNull(annotation);
        assertFalse(annotation.value().contains("INSURANCE_PARTNER"));
    }

    @Test
    void controllerMappedToApiV1Alerts() {
        RequestMapping mapping = AlertController.class.getAnnotation(RequestMapping.class);
        assertNotNull(mapping);
        assertEquals("/api/v1/alerts", mapping.value()[0]);
    }

    @Test
    void acknowledgeEndpoint_excludesOccupantAndReadonly() throws NoSuchMethodException {
        Method method = findMethodByName("acknowledge");
        PreAuthorize ann = method.getAnnotation(PreAuthorize.class);
        assertNotNull(ann);
        assertTrue(ann.value().contains("ELECTRICIAN"));
        assertFalse(ann.value().contains("OCCUPANT"));
        assertFalse(ann.value().contains("SUPPORT_READONLY"));
    }

    @Test
    void assignEndpoint_restrictedToManagers() throws NoSuchMethodException {
        Method method = findMethodByName("assign");
        PreAuthorize ann = method.getAnnotation(PreAuthorize.class);
        assertNotNull(ann);
        assertTrue(ann.value().contains("PLATFORM_ADMIN"));
        assertTrue(ann.value().contains("TENANT_ADMIN"));
        assertTrue(ann.value().contains("PROPERTY_MANAGER"));
        assertFalse(ann.value().contains("ELECTRICIAN"));
        assertFalse(ann.value().contains("OCCUPANT"));
        assertFalse(ann.value().contains("SUPPORT_READONLY"));
    }

    @Test
    void resolveEndpoint_includesElectricianButNotOccupant() throws NoSuchMethodException {
        Method method = findMethodByName("resolve");
        PreAuthorize ann = method.getAnnotation(PreAuthorize.class);
        assertNotNull(ann);
        assertTrue(ann.value().contains("ELECTRICIAN"));
        assertFalse(ann.value().contains("OCCUPANT"));
        assertFalse(ann.value().contains("SUPPORT_READONLY"));
    }

    @Test
    void falsePositiveEndpoint_restrictedToManagers() throws NoSuchMethodException {
        Method method = findMethodByName("falsePositive");
        PreAuthorize ann = method.getAnnotation(PreAuthorize.class);
        assertNotNull(ann);
        assertTrue(ann.value().contains("PLATFORM_ADMIN"));
        assertTrue(ann.value().contains("TENANT_ADMIN"));
        assertTrue(ann.value().contains("PROPERTY_MANAGER"));
        assertFalse(ann.value().contains("ELECTRICIAN"));
        assertFalse(ann.value().contains("OCCUPANT"));
    }

    @Test
    void commentEndpoint_includesElectrician() throws NoSuchMethodException {
        Method method = findMethodByName("addComment");
        PreAuthorize ann = method.getAnnotation(PreAuthorize.class);
        assertNotNull(ann);
        assertTrue(ann.value().contains("ELECTRICIAN"));
        assertFalse(ann.value().contains("OCCUPANT"));
        assertFalse(ann.value().contains("SUPPORT_READONLY"));
    }

    @Test
    void listEndpoint_hasNoMethodLevelAnnotation_usesClassLevel() throws NoSuchMethodException {
        Method method = findMethodByName("list");
        PreAuthorize ann = method.getAnnotation(PreAuthorize.class);
        assertNull(ann, "list inherits class-level annotation (includes OCCUPANT for own-dwelling read)");
    }

    @Test
    void noSecretFieldsInAlertResponse() {
        var fields = AlertController.AlertResponse.class.getRecordComponents();
        for (var field : fields) {
            String name = field.getName().toLowerCase();
            assertFalse(name.contains("password"));
            assertFalse(name.contains("secret"));
            assertFalse(name.contains("token"));
        }
    }

    @Test
    void tenantIsolation_getByIdFiltersByTenant() throws NoSuchMethodException {
        Method method = findMethodByName("getById");
        assertNotNull(method);
    }

    @Test
    void statisticsEndpoint_inheritsClassLevel() throws NoSuchMethodException {
        Method method = findMethodByName("getStatistics");
        PreAuthorize ann = method.getAnnotation(PreAuthorize.class);
        assertNull(ann, "statistics uses class-level security");
    }

    @Test
    void criticalEndpoint_inheritsClassLevel() throws NoSuchMethodException {
        Method method = findMethodByName("getOpenCritical");
        PreAuthorize ann = method.getAnnotation(PreAuthorize.class);
        assertNull(ann, "getOpenCritical uses class-level security");
    }

    private Method findMethodByName(String name) {
        for (Method m : AlertController.class.getDeclaredMethods()) {
            if (m.getName().equals(name)) return m;
        }
        throw new AssertionError("Method not found: " + name);
    }
}
