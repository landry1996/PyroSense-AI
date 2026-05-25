package com.pyrosense.analysis.adapter.out.ml;

import com.pyrosense.analysis.application.port.out.MachineLearningInferencePort;
import com.pyrosense.analysis.domain.model.SignalAnomaly;
import com.pyrosense.analysis.domain.model.SignalWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class NoOpMachineLearningAdapter implements MachineLearningInferencePort {

    private static final Logger log = LoggerFactory.getLogger(NoOpMachineLearningAdapter.class);
    private static final String VERSION = "noop-v1.0.0";

    @Override
    public Optional<List<SignalAnomaly>> infer(SignalWindow window) {
        return Optional.empty();
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public String modelVersion() {
        return VERSION;
    }
}
