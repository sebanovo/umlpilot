package com.umlpilot.umlpilot_api.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MethodJpaRepository extends JpaRepository<MethodJpaEntity, String> {
    List<MethodJpaEntity> findByElementId(String elementId);
}
