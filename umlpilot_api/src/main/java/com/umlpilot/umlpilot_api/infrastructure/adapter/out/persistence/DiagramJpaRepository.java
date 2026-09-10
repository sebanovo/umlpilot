package com.umlpilot.umlpilot_api.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DiagramJpaRepository extends JpaRepository<DiagramJpaEntity, String> {
    List<DiagramJpaEntity> findByProjectId(String projectId);
}
