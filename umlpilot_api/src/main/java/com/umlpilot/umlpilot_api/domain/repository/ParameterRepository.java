package com.umlpilot.umlpilot_api.domain.repository;

import com.umlpilot.umlpilot_api.domain.model.*;
import java.util.List;
import java.util.Optional;

public interface ParameterRepository {
    Parameter save(Parameter parameter);
    Optional<Parameter> findById(ParameterId id);
    List<Parameter> findByMethodId(String methodId);
    void delete(ParameterId id);
}
