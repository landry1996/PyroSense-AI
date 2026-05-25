package com.pyrosense.alerting.application.port.in;

import com.pyrosense.alerting.domain.model.Alert;
import com.pyrosense.shared.id.AlertId;
import com.pyrosense.shared.id.UserId;

public interface ManageAlertUseCase {

    Alert acknowledge(AlertId alertId, UserId userId);

    Alert assign(AlertId alertId, UserId assignee, UserId assignedBy);

    Alert resolve(AlertId alertId, UserId userId, String resolutionNote);

    Alert markFalsePositive(AlertId alertId, UserId userId, String reason);

    Alert addComment(AlertId alertId, UserId author, String content);
}
