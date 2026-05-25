package com.pyrosense.ingestion.application.port.out;

import com.pyrosense.ingestion.domain.model.TelemetryReading;

public interface TelemetryRepositoryPort {

    void save(TelemetryReading reading);
}
