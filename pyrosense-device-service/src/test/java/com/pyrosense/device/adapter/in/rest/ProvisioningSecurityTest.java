package com.pyrosense.device.adapter.in.rest;

import com.pyrosense.device.application.port.in.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.*;

class ProvisioningSecurityTest {

    @Test
    @DisplayName("createClaimToken requires ADMIN or DEVICE_MANAGER or PLATFORM_ADMIN or TENANT_ADMIN")
    void claimTokenRequiresAdminRoles() throws Exception {
        Method method = ProvisioningController.class.getMethod("createClaimToken",
                java.util.UUID.class, org.springframework.security.oauth2.jwt.Jwt.class);
        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).contains("ADMIN");
        assertThat(annotation.value()).contains("DEVICE_MANAGER");
    }

    @Test
    @DisplayName("provision endpoint has no @PreAuthorize (permit all for device)")
    void provisionEndpointIsPublic() throws Exception {
        Method method = ProvisioningController.class.getMethod("provision",
                com.pyrosense.device.adapter.in.rest.dto.ProvisionDeviceWithTokenRequest.class,
                jakarta.servlet.http.HttpServletRequest.class);
        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);

        assertThat(annotation).isNull();
    }

    @Test
    @DisplayName("rotateCredentials requires ADMIN or DEVICE_MANAGER or PLATFORM_ADMIN")
    void rotateRequiresAdminRoles() throws Exception {
        Method method = ProvisioningController.class.getMethod("rotateCredentials",
                java.util.UUID.class, org.springframework.security.oauth2.jwt.Jwt.class);
        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).contains("ADMIN");
        assertThat(annotation.value()).contains("PLATFORM_ADMIN");
    }

    @Test
    @DisplayName("DeviceController.revoke requires ADMIN or DEVICE_MANAGER or PLATFORM_ADMIN")
    void revokeRequiresAdminRoles() throws Exception {
        Method method = DeviceController.class.getMethod("revoke",
                java.util.UUID.class,
                com.pyrosense.device.adapter.in.rest.dto.RevokeDeviceRequest.class,
                org.springframework.security.oauth2.jwt.Jwt.class);
        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).contains("ADMIN");
        assertThat(annotation.value()).contains("PLATFORM_ADMIN");
    }

    @Test
    @DisplayName("ProvisionDeviceWithTokenRequest validates claimToken is not blank")
    void requestValidation() throws Exception {
        var fields = com.pyrosense.device.adapter.in.rest.dto.ProvisionDeviceWithTokenRequest.class.getRecordComponents();
        boolean hasClaimToken = false;
        for (var component : fields) {
            if ("claimToken".equals(component.getName())) {
                hasClaimToken = true;
                var notBlank = component.getAccessor().getAnnotation(jakarta.validation.constraints.NotBlank.class);
                assertThat(notBlank).isNotNull();
            }
        }
        assertThat(hasClaimToken).isTrue();
    }
}
