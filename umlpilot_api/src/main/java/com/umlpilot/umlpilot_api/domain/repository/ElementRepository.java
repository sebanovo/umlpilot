package com.umlpilot.umlpilot_api.domain.repository;

import com.umlpilot.umlpilot_api.domain.model.*;
import java.util.List;
import java.util.Optional;

public interface ElementRepository {
    Element save(Element element);
    Optional<Element> findById(ElementId id);
    List<Element> findByDiagramId(String diagramId);
    void delete(ElementId id);
}
