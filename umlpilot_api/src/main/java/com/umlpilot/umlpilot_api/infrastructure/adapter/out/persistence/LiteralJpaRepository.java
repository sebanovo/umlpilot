package com.umlpilot.umlpilot_api.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LiteralJpaRepository extends JpaRepository<LiteralJpaEntity, String> {
    List<LiteralJpaEntity> findByElementId(String elementId);
}
