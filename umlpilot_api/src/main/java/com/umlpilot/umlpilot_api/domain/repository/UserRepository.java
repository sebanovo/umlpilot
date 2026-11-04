package com.umlpilot.umlpilot_api.domain.repository;

import com.umlpilot.umlpilot_api.domain.model.User;
import com.umlpilot.umlpilot_api.domain.model.UserId;
import java.util.List;
import java.util.Optional;

public interface UserRepository {
    User save(User user);
    Optional<User> findById(UserId id);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    /** Busca usuarios por email o nombre, excluyendo al usuario indicado. */
    List<User> search(String query, String excludeUserId, int limit);
}
