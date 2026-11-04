package com.umlpilot.umlpilot_api.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DiagramShareLinkJpaRepository extends JpaRepository<DiagramShareLinkJpaEntity, String> {
    Optional<DiagramShareLinkJpaEntity> findByDiagramId(String diagramId);

    Optional<DiagramShareLinkJpaEntity> findByTokenAndActiveTrue(String token);
}
