package com.umlpilot.umlpilot_api.infrastructure.adapter.out.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserJpaRepository extends JpaRepository<UserJpaEntity, String> {
    Optional<UserJpaEntity> findByEmail(String email);
    boolean existsByEmail(String email);

    @Query("""
            SELECT u FROM UserJpaEntity u
            WHERE u.id <> :excludeUserId
              AND (LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :query, '%')))
            ORDER BY u.email ASC
            """)
    List<UserJpaEntity> search(@Param("query") String query,
                               @Param("excludeUserId") String excludeUserId,
                               Pageable pageable);
}
