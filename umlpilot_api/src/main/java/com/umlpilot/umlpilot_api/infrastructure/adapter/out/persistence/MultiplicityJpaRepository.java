package com.umlpilot.umlpilot_api.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MultiplicityJpaRepository extends JpaRepository<MultiplicityJpaEntity, String> {
    List<MultiplicityJpaEntity> findByRelationshipId(String relationshipId);
}
