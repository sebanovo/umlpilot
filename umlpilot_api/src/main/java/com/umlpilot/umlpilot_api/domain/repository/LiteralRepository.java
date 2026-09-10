package com.umlpilot.umlpilot_api.domain.repository;

import com.umlpilot.umlpilot_api.domain.model.*;
import java.util.List;
import java.util.Optional;

public interface LiteralRepository {
    Literal save(Literal literal);
    Optional<Literal> findById(LiteralId id);
    List<Literal> findByElementId(String elementId);
    void delete(LiteralId id);
}
