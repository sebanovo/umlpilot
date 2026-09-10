package com.umlpilot.umlpilot_api.repository;

import com.umlpilot.umlpilot_api.model.Collaborator;
import com.umlpilot.umlpilot_api.model.CollaboratorId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface CollaboratorRepository extends JpaRepository<Collaborator, CollaboratorId> {

    List<Collaborator> findByProjectId(String projectId);

    List<Collaborator> findByUserIdAndInvitationStatus(String userId, String status);

    Collaborator findByProjectIdAndUserId(String projectId, String userId);

    @Query("SELECT c FROM Collaborator c WHERE c.projectId = :projectId AND c.userId = :userId AND c.invitationStatus = 'accepted'")
    Collaborator findAcceptedCollaborator(@Param("projectId") String projectId, @Param("userId") String userId);
}
