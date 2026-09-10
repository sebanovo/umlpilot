package com.umlpilot.umlpilot_api.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AttributeJpaRepository extends JpaRepository<AttributeJpaEntity, String> {
    List<AttributeJpaEntity> findByElementId(String elementId);
}
