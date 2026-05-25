package com.pyrosense.analysis.application.port.in;

import com.pyrosense.analysis.domain.model.BaselineProfile;
import com.pyrosense.analysis.domain.model.SignalWindow;
import com.pyrosense.shared.id.DeviceId;

public interface BuildBaselineUseCase {

    BaselineProfile buildOrUpdate(BuildBaselineCommand command);

    record BuildBaselineCommand(DeviceId deviceId, SignalWindow window) {
        public BuildBaselineCommand {
            if (deviceId == null) throw new IllegalArgumentException("deviceId must not be null");
            if (window == null) throw new IllegalArgumentException("window must not be null");
        }
    }
}
