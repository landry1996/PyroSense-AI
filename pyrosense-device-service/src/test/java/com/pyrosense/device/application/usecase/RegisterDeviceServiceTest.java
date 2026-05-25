package com.pyrosense.device.application.usecase;

import com.pyrosense.device.application.port.in.RegisterDeviceUseCase.RegisterDeviceCommand;
import com.pyrosense.device.application.port.out.DeviceEventPublisherPort;
import com.pyrosense.device.application.port.out.DeviceRepositoryPort;
import com.pyrosense.device.application.port.out.EnrollmentKeyGeneratorPort;
import com.pyrosense.device.application.port.out.EnrollmentKeyGeneratorPort.EnrollmentKeyPair;
import com.pyrosense.device.domain.model.ConnectivityType;
import com.pyrosense.device.domain.model.Device;
import com.pyrosense.shared.exception.BusinessException;
import com.pyrosense.shared.util.ClockProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterDeviceServiceTest {

    @Mock
    private DeviceRepositoryPort repository;
    @Mock
    private DeviceEventPublisherPort eventPublisher;
    @Mock
    private EnrollmentKeyGeneratorPort keyGenerator;

    private RegisterDeviceService service;

    @BeforeEach
    void setUp() {
        ClockProvider.setClock(Clock.fixed(Instant.parse("2025-01-15T10:00:00Z"), ZoneId.of("UTC")));
        service = new RegisterDeviceService(repository, eventPublisher, keyGenerator);
    }

    @AfterEach
    void tearDown() {
        ClockProvider.reset();
    }

    @Test
    void shouldRegisterDeviceSuccessfully() {
        when(repository.existsBySerialNumber("SN-001")).thenReturn(false);
        when(keyGenerator.generate()).thenReturn(new EnrollmentKeyPair("plain-key", "hashed-key"));
        when(repository.save(any(Device.class))).thenAnswer(inv -> inv.getArgument(0));

        var command = new RegisterDeviceCommand("SN-001", "1.0.0", "rev-A", ConnectivityType.WIFI);
        var result = service.execute(command, "admin");

        assertThat(result.deviceId()).isNotNull();
        assertThat(result.enrollmentKey()).isEqualTo("plain-key");

        var deviceCaptor = ArgumentCaptor.forClass(Device.class);
        verify(repository).save(deviceCaptor.capture());
        var savedDevice = deviceCaptor.getValue();
        assertThat(savedDevice.getSerialNumber()).isEqualTo("SN-001");
        assertThat(savedDevice.getEnrollmentKeyHash()).isEqualTo("hashed-key");

        verify(eventPublisher).publish(any());
    }

    @Test
    void shouldRejectDuplicateSerialNumber() {
        when(repository.existsBySerialNumber("SN-001")).thenReturn(true);

        var command = new RegisterDeviceCommand("SN-001", "1.0.0", "rev-A", ConnectivityType.WIFI);

        assertThatThrownBy(() -> service.execute(command, "admin"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already exists");

        verify(repository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }
}
