package com.umlpilot.umlpilot_api.domain.repository;

import com.umlpilot.umlpilot_api.domain.model.DiagramShareLink;

import java.util.Optional;

public interface DiagramShareLinkRepository {
    DiagramShareLink save(DiagramShareLink link);

    Optional<DiagramShareLink> findByDiagramId(String diagramId);

    Optional<DiagramShareLink> findActiveByToken(String token);
}
