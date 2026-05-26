package com.pyrosense.maintenance.application.port.in;

import com.pyrosense.maintenance.domain.model.Intervention;
import com.pyrosense.shared.id.UserId;

import java.util.UUID;

public interface AddInterventionCommentUseCase {

    Intervention addComment(UUID interventionId, UserId authorId, String content);
}
