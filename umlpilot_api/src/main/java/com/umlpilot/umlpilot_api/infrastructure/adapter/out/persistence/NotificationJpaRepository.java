package com.umlpilot.umlpilot_api.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationJpaRepository extends JpaRepository<NotificationJpaEntity, String> {

    List<NotificationJpaEntity> findByUserIdOrderByCreatedAtDesc(String userId);

    List<NotificationJpaEntity> findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(String userId);

    long countByUserIdAndReadAtIsNull(String userId);

    Optional<NotificationJpaEntity> findByIdAndUserId(String id, String userId);

    Optional<NotificationJpaEntity> findFirstByUserIdAndProjectIdAndTypeOrderByCreatedAtDesc(
            String userId, String projectId, String type);

    void deleteByUserIdAndProjectIdAndType(String userId, String projectId, String type);
}
