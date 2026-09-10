package com.umlpilot.umlpilot_api.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ParameterJpaRepository extends JpaRepository<ParameterJpaEntity, String> {
    List<ParameterJpaEntity> findByMethodId(String methodId);
}
