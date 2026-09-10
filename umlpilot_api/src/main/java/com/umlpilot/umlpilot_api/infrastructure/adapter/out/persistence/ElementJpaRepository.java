package com.umlpilot.umlpilot_api.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ElementJpaRepository extends JpaRepository<ElementJpaEntity, String> {
    List<ElementJpaEntity> findByDiagramId(String diagramId);
}
