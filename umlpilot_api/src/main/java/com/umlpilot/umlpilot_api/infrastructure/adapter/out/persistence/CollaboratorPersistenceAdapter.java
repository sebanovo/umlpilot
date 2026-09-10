package com.umlpilot.umlpilot_api.infrastructure.adapter.out.persistence;

import com.umlpilot.umlpilot_api.domain.model.*;
import com.umlpilot.umlpilot_api.domain.repository.CollaboratorRepository;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;

@Component
public class CollaboratorPersistenceAdapter implements CollaboratorRepository {
    private final CollaboratorJpaRepository jpa;
    public CollaboratorPersistenceAdapter(CollaboratorJpaRepository jpa) { this.jpa = jpa; }

    @Override public Collaborator save(Collaborator collaborator) {
        CollaboratorJpaEntity.CollaboratorId jpaId = new CollaboratorJpaEntity.CollaboratorId(collaborator.getProjectId(), collaborator.getUserId());
        Optional<CollaboratorJpaEntity> existing = jpa.findById(jpaId);
        CollaboratorJpaEntity entity = existing.orElse(new CollaboratorJpaEntity());
        entity.setProjectId(collaborator.getProjectId());
        entity.setUserId(collaborator.getUserId());
        entity.setRole(collaborator.getRole());
        entity.setInvitationStatus(collaborator.getInvitationStatus());
        entity.setAcceptanceDate(collaborator.getAcceptanceDate());
        CollaboratorJpaEntity saved = jpa.save(entity);
        return Collaborator.reconstitute(new CollaboratorId(saved.getProjectId(), saved.getUserId()), saved.getRole(), saved.getInvitationStatus(), saved.getInvitationDate(), saved.getAcceptanceDate());
    }
    @Override public Optional<Collaborator> findById(CollaboratorId id) { return jpa.findById(new CollaboratorJpaEntity.CollaboratorId(id.projectId(), id.userId())).map(e -> Collaborator.reconstitute(new CollaboratorId(e.getProjectId(), e.getUserId()), e.getRole(), e.getInvitationStatus(), e.getInvitationDate(), e.getAcceptanceDate())); }
    @Override public List<Collaborator> findByProjectId(String projectId) { return jpa.findByProjectId(projectId).stream().map(e -> Collaborator.reconstitute(new CollaboratorId(e.getProjectId(), e.getUserId()), e.getRole(), e.getInvitationStatus(), e.getInvitationDate(), e.getAcceptanceDate())).toList(); }
    @Override public Optional<Collaborator> findByProjectIdAndUserId(String projectId, String userId) { CollaboratorJpaEntity e = jpa.findByProjectIdAndUserId(projectId, userId); return e != null ? Optional.of(Collaborator.reconstitute(new CollaboratorId(e.getProjectId(), e.getUserId()), e.getRole(), e.getInvitationStatus(), e.getInvitationDate(), e.getAcceptanceDate())) : Optional.empty(); }
    @Override public Optional<Collaborator> findAcceptedCollaborator(String projectId, String userId) { CollaboratorJpaEntity e = jpa.findAcceptedCollaborator(projectId, userId); return e != null ? Optional.of(Collaborator.reconstitute(new CollaboratorId(e.getProjectId(), e.getUserId()), e.getRole(), e.getInvitationStatus(), e.getInvitationDate(), e.getAcceptanceDate())) : Optional.empty(); }
    @Override public void delete(CollaboratorId id) { jpa.deleteById(new CollaboratorJpaEntity.CollaboratorId(id.projectId(), id.userId())); }
}
