package com.umlpilot.umlpilot_api.domain.repository;

import com.umlpilot.umlpilot_api.domain.model.Collaborator;
import com.umlpilot.umlpilot_api.domain.model.CollaboratorId;
import java.util.List;
import java.util.Optional;

public interface CollaboratorRepository {
    Collaborator save(Collaborator collaborator);
    Optional<Collaborator> findById(CollaboratorId id);
    List<Collaborator> findByProjectId(String projectId);
    Optional<Collaborator> findByProjectIdAndUserId(String projectId, String userId);
    Optional<Collaborator> findAcceptedCollaborator(String projectId, String userId);
    void delete(CollaboratorId id);
}
