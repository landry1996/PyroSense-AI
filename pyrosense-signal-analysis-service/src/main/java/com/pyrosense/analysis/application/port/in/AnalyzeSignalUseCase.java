package com.pyrosense.analysis.application.port.in;

import com.pyrosense.analysis.domain.model.AnalysisResult;
import com.pyrosense.analysis.domain.model.SignalWindow;

public interface AnalyzeSignalUseCase {

    AnalysisResult analyze(AnalyzeTelemetryWindowCommand command);

    record AnalyzeTelemetryWindowCommand(SignalWindow window) {
        public AnalyzeTelemetryWindowCommand {
            if (window == null) throw new IllegalArgumentException("window must not be null");
        }
    }
}
