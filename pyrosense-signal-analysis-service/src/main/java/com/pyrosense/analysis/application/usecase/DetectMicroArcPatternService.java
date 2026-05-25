package com.pyrosense.analysis.application.usecase;

import com.pyrosense.analysis.application.port.in.DetectMicroArcPatternUseCase;
import com.pyrosense.analysis.domain.detection.MicroArcPatternDetector;
import com.pyrosense.analysis.domain.model.DetectionThresholds;
import com.pyrosense.analysis.domain.model.SignalAnomaly;
import com.pyrosense.shared.util.ClockProvider;

import java.util.List;

public class DetectMicroArcPatternService implements DetectMicroArcPatternUseCase {

    private final DetectionThresholds thresholds;

    public DetectMicroArcPatternService(DetectionThresholds thresholds) {
        this.thresholds = thresholds;
    }

    @Override
    public List<SignalAnomaly> detect(DetectMicroArcCommand command) {
        return MicroArcPatternDetector.detect(command.window(), thresholds, ClockProvider.now());
    }
}
