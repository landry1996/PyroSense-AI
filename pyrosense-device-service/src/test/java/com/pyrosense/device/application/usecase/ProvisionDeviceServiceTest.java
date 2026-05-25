package com.pyrosense.device.application.usecase;

import com.pyrosense.device.application.port.in.ProvisionDeviceUseCase.ProvisionDeviceCommand;
import com.pyrosense.device.application.port.out.DeviceEventPublisherPort;
import com.pyrosense.device.application.port.out.DeviceRepositoryPort;
import com.pyrosense.device.domain.model.ConnectivityType;
import com.pyrosense.device.domain.model.Device;
import com.pyrosense.shared.exception.NotFoundException;
import com.pyrosense.shared.id.BuildingId;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.ElectricalPanelId;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.util.ClockProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProvisionDeviceServiceTest {

    @Mock
    private DeviceRepositoryPort repository;
    @Mock
    private DeviceEventPublisherPort eventPublisher;

    private ProvisionDeviceService service;

    @BeforeEach
    void setUp() {
        ClockProvider.setClock(Clock.fixed(Instant.parse("2025-01-15T10:00:00Z"), ZoneId.of("UTC")));
        service = new ProvisionDeviceService(repository, eventPublisher);
    }

    @AfterEach
    void tearDown() {
        ClockProvider.reset();
    }

    @Test
    void shouldProvisionDevice() {
        var device = Device.register("SN-001", "1.0.0", "rev-A", ConnectivityType.WIFI, "hash", "admin");
        device.clearDomainEvents();
        when(repository.findById(device.getId())).thenReturn(Optional.of(device));
        when(repository.save(any(Device.class))).thenAnswer(inv -> inv.getArgument(0));

        var command = new ProvisionDeviceCommand(
                device.getId(), TenantId.generate(), BuildingId.generate(), ElectricalPanelId.generate());

        service.execute(command, "admin");

        verify(repository).save(any(Device.class));
        verify(eventPublisher).publish(any());
    }

    @Test
    void shouldThrowWhenDeviceNotFound() {
        var deviceId = DeviceId.generate();
        when(repository.findById(deviceId)).thenReturn(Optional.empty());

        var command = new ProvisionDeviceCommand(
                deviceId, TenantId.generate(), BuildingId.generate(), ElectricalPanelId.generate());

        assertThatThrownBy(() -> service.execute(command, "admin"))
                .isInstanceOf(NotFoundException.class);
    }
}
