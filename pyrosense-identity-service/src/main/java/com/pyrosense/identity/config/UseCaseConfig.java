package com.pyrosense.identity.config;

import com.pyrosense.identity.application.port.out.AuditLogRepository;
import com.pyrosense.identity.application.port.out.DeviceCredentialRepository;
import com.pyrosense.identity.application.port.out.TenantRepository;
import com.pyrosense.identity.application.port.out.UserRepository;
import com.pyrosense.identity.application.usecase.ManageDeviceCredentialService;
import com.pyrosense.identity.application.usecase.ManageTenantService;
import com.pyrosense.identity.application.usecase.RegisterUserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {

    @Bean
    public RegisterUserService registerUserService(UserRepository userRepository,
                                                    TenantRepository tenantRepository,
                                                    AuditLogRepository auditLog) {
        return new RegisterUserService(userRepository, tenantRepository, auditLog);
    }

    @Bean
    public ManageTenantService manageTenantService(TenantRepository tenantRepository) {
        return new ManageTenantService(tenantRepository);
    }

    @Bean
    public ManageDeviceCredentialService manageDeviceCredentialService(DeviceCredentialRepository repository) {
        return new ManageDeviceCredentialService(repository);
    }
}
