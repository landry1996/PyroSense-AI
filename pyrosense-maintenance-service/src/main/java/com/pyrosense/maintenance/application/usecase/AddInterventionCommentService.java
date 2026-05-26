package com.pyrosense.maintenance.application.usecase;

import com.pyrosense.maintenance.application.port.in.AddInterventionCommentUseCase;
import com.pyrosense.maintenance.application.port.out.InterventionRepositoryPort;
import com.pyrosense.maintenance.domain.model.Intervention;
import com.pyrosense.maintenance.domain.model.InterventionComment;
import com.pyrosense.shared.exception.NotFoundException;
import com.pyrosense.shared.id.UserId;

import java.util.UUID;

public class AddInterventionCommentService implements AddInterventionCommentUseCase {

    private final InterventionRepositoryPort repository;

    public AddInterventionCommentService(InterventionRepositoryPort repository) {
        this.repository = repository;
    }

    @Override
    public Intervention addComment(UUID interventionId, UserId authorId, String content) {
        Intervention intervention = repository.findById(interventionId)
                .orElseThrow(() -> new NotFoundException("Intervention", interventionId.toString()));

        InterventionComment comment = InterventionComment.create(authorId, content);
        intervention.addComment(comment);

        return repository.save(intervention);
    }
}
