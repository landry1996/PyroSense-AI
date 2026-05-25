package com.pyrosense.analysis.application.port.in;

import com.pyrosense.analysis.domain.model.SignalAnomaly;
import com.pyrosense.analysis.domain.model.SignalWindow;
import com.pyrosense.shared.id.DeviceId;

import java.util.List;

public interface DetectSignalDriftUseCase {

    List<SignalAnomaly> detectDrift(DetectDriftCommand command);

    record DetectDriftCommand(DeviceId deviceId, SignalWindow window) {
        public DetectDriftCommand {
            if (deviceId == null) throw new IllegalArgumentException("deviceId must not be null");
            if (window == null) throw new IllegalArgumentException("window must not be null");
        }
    }
}
