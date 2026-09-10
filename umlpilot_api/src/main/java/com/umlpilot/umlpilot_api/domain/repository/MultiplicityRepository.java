package com.umlpilot.umlpilot_api.domain.repository;

import com.umlpilot.umlpilot_api.domain.model.*;
import java.util.List;
import java.util.Optional;

public interface MultiplicityRepository {
    Multiplicity save(Multiplicity multiplicity);
    Optional<Multiplicity> findById(MultiplicityId id);
    List<Multiplicity> findByRelationshipId(String relationshipId);
    void delete(MultiplicityId id);
}
