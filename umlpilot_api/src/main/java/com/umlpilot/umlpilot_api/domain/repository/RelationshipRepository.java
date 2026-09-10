package com.umlpilot.umlpilot_api.domain.repository;

import com.umlpilot.umlpilot_api.domain.model.*;
import java.util.List;
import java.util.Optional;

public interface RelationshipRepository {
    Relationship save(Relationship relationship);
    Optional<Relationship> findById(RelationshipId id);
    List<Relationship> findByDiagramId(String diagramId);
    void delete(RelationshipId id);
}
