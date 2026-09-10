package com.umlpilot.umlpilot_api.domain.repository;

import com.umlpilot.umlpilot_api.domain.model.*;
import java.util.List;
import java.util.Optional;

public interface AttributeRepository {
    Attribute save(Attribute attribute);
    Optional<Attribute> findById(AttributeId id);
    List<Attribute> findByElementId(String elementId);
    void delete(AttributeId id);
}
