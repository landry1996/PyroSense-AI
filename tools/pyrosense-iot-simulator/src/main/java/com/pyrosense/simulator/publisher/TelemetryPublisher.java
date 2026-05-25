package com.pyrosense.simulator.publisher;

import com.pyrosense.simulator.domain.TelemetryReading;

public interface TelemetryPublisher {
    void publish(TelemetryReading reading);
    void connect();
    void disconnect();
    String name();
}
