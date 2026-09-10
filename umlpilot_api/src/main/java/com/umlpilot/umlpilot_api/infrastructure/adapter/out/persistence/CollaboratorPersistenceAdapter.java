package com.umlpilot.umlpilot_api.infrastructure.adapter.out.persistence;

import com.umlpilot.umlpilot_api.domain.model.*;
import com.umlpilot.umlpilot_api.domain.repository.CollaboratorRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class CollaboratorPersistenceAdapter implements CollaboratorRepository {

    private final CollaboratorJpaRepository jpaRepository;

    public CollaboratorPersistenceAdapter(CollaboratorJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Collaborator save(Collaborator collaborator) {
        CollaboratorJpaEntity entity = new CollaboratorJpaEntity(
                collaborator.getProjectId(), collaborator.getUserId(), collaborator.getRole(),
                collaborator.getInvitationStatus(), collaborator.getInvitationDate(), collaborator.getAcceptanceDate()
        );
        CollaboratorJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Collaborator> findById(CollaboratorId id) {
        return jpaRepository.findById(new CollaboratorJpaEntity.CollaboratorId(id.projectId(), id.userId())).map(this::toDomain);
    }

    @Override
    public List<Collaborator> findByProjectId(String projectId) {
        return jpaRepository.findByProjectId(projectId).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Collaborator> findByProjectIdAndUserId(String projectId, String userId) {
        CollaboratorJpaEntity entity = jpaRepository.findByProjectIdAndUserId(projectId, userId);
        return entity != null ? Optional.of(toDomain(entity)) : Optional.empty();
    }

    @Override
    public Optional<Collaborator> findAcceptedCollaborator(String projectId, String userId) {
        CollaboratorJpaEntity entity = jpaRepository.findAcceptedCollaborator(projectId, userId);
        return entity != null ? Optional.of(toDomain(entity)) : Optional.empty();
    }

    @Override
    public void delete(CollaboratorId id) {
        jpaRepository.deleteById(new CollaboratorJpaEntity.CollaboratorId(id.projectId(), id.userId()));
    }

    private Collaborator toDomain(CollaboratorJpaEntity entity) {
        return Collaborator.reconstitute(
                new CollaboratorId(entity.getProjectId(), entity.getUserId()),
                entity.getRole(), entity.getInvitationStatus(),
                entity.getInvitationDate(), entity.getAcceptanceDate()
        );
    }
}
