package com.pyrosense.analysis.application.port.out;

import com.pyrosense.analysis.domain.model.SignalAnomaly;
import com.pyrosense.analysis.domain.model.SignalWindow;

import java.util.List;
import java.util.Optional;

public interface MachineLearningInferencePort {

    Optional<List<SignalAnomaly>> infer(SignalWindow window);

    boolean isAvailable();

    String modelVersion();
}
