package com.pyrosense.analysis.application.port.in;

import com.pyrosense.analysis.domain.model.SignalAnomaly;
import com.pyrosense.analysis.domain.model.SignalWindow;

import java.util.List;

public interface DetectMicroArcPatternUseCase {

    List<SignalAnomaly> detect(DetectMicroArcCommand command);

    record DetectMicroArcCommand(SignalWindow window) {
        public DetectMicroArcCommand {
            if (window == null) throw new IllegalArgumentException("window must not be null");
        }
    }
}
