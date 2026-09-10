package com.umlpilot.umlpilot_api.domain.repository;

import com.umlpilot.umlpilot_api.domain.model.*;
import java.util.List;
import java.util.Optional;

public interface MethodRepository {
    Method save(Method method);
    Optional<Method> findById(MethodId id);
    List<Method> findByElementId(String elementId);
    void delete(MethodId id);
}
