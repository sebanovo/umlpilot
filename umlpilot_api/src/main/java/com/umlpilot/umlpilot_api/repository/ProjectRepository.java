package com.umlpilot.umlpilot_api.repository;

import com.umlpilot.umlpilot_api.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, String> {

    @Query("SELECT p FROM Project p WHERE p.id IN (SELECT c.projectId FROM Collaborator c WHERE c.userId = :userId AND c.invitationStatus = 'accepted')")
    List<Project> findByUserId(@Param("userId") String userId);

    @Query("SELECT p FROM Project p WHERE p.id = :projectId AND p.id IN (SELECT c.projectId FROM Collaborator c WHERE c.userId = :userId AND c.invitationStatus = 'accepted')")
    Project findByIdAndUserId(@Param("projectId") String projectId, @Param("userId") String userId);
}
