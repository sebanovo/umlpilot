package com.umlpilot.umlpilot_api.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface CollaboratorJpaRepository extends JpaRepository<CollaboratorJpaEntity, CollaboratorJpaEntity.CollaboratorId> {
    List<CollaboratorJpaEntity> findByProjectId(String projectId);
    CollaboratorJpaEntity findByProjectIdAndUserId(String projectId, String userId);

    @Query("SELECT c FROM CollaboratorJpaEntity c WHERE c.projectId = :projectId AND c.userId = :userId AND c.invitationStatus = 'accepted'")
    CollaboratorJpaEntity findAcceptedCollaborator(@Param("projectId") String projectId, @Param("userId") String userId);
}
