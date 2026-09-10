package com.umlpilot.umlpilot_api.domain.repository;

import com.umlpilot.umlpilot_api.domain.model.Diagram;
import com.umlpilot.umlpilot_api.domain.model.DiagramId;
import java.util.List;
import java.util.Optional;

public interface DiagramRepository {
    Diagram save(Diagram diagram);
    Optional<Diagram> findById(DiagramId id);
    List<Diagram> findByProjectId(String projectId);
    void delete(DiagramId id);
}
