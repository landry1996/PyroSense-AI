package com.pyrosense.maintenance.application.port.in;

import com.pyrosense.maintenance.domain.model.FieldDiagnostic;
import com.pyrosense.maintenance.domain.model.Intervention;
import com.pyrosense.maintenance.domain.model.InterventionResult;
import com.pyrosense.maintenance.domain.model.RiskImpact;
import com.pyrosense.shared.id.UserId;

import java.time.Instant;
import java.util.UUID;

public interface ManageInterventionUseCase {

    Intervention schedule(UUID interventionId, Instant scheduledAt);

    Intervention assign(UUID interventionId, UserId electricianId, Instant scheduledAt);

    Intervention start(UUID interventionId);

    Intervention addDiagnostic(UUID interventionId, FieldDiagnostic diagnostic);

    Intervention complete(UUID interventionId, InterventionResult result);

    Intervention recordRiskImpact(UUID interventionId, RiskImpact riskImpact);

    Intervention cancel(UUID interventionId);
}
