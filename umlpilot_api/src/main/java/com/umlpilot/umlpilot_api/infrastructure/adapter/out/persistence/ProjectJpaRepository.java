package com.umlpilot.umlpilot_api.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ProjectJpaRepository extends JpaRepository<ProjectJpaEntity, String> {

    @Query("SELECT p FROM ProjectJpaEntity p WHERE p.id IN (SELECT c.projectId FROM CollaboratorJpaEntity c WHERE c.userId = :userId AND c.invitationStatus = 'accepted')")
    List<ProjectJpaEntity> findByUserId(@Param("userId") String userId);

    @Query("SELECT p FROM ProjectJpaEntity p WHERE p.id = :projectId AND p.id IN (SELECT c.projectId FROM CollaboratorJpaEntity c WHERE c.userId = :userId AND c.invitationStatus = 'accepted')")
    ProjectJpaEntity findByIdAndUserId(@Param("projectId") String projectId, @Param("userId") String userId);
}
