package com.umlpilot.umlpilot_api.infrastructure.adapter.out.persistence;

import com.umlpilot.umlpilot_api.domain.model.*;
import com.umlpilot.umlpilot_api.domain.repository.ParameterRepository;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;

@Component
public class ParameterPersistenceAdapter implements ParameterRepository {
    private final ParameterJpaRepository jpa;
    public ParameterPersistenceAdapter(ParameterJpaRepository jpa) { this.jpa = jpa; }

    @Override public Parameter save(Parameter param) {
        Optional<ParameterJpaEntity> existing = jpa.findById(param.getId().value());
        ParameterJpaEntity e = existing.orElse(new ParameterJpaEntity());
        e.setMethodId(param.getMethodId().value());
        e.setName(param.getName());
        e.setDataType(param.getDataType());
        e.setOrderIndex(param.getOrderIndex());
        e.setIsVarargs(param.getIsVarargs());
        e.setIsFinal(param.getIsFinal());
        e.setDefaultValue(param.getDefaultValue());
        ParameterJpaEntity saved = jpa.save(e);
        return Parameter.reconstitute(ParameterId.from(saved.getId()), MethodId.from(saved.getMethodId()), saved.getName(), saved.getDataType(), saved.getOrderIndex(), saved.getIsVarargs(), saved.getIsFinal(), saved.getDefaultValue());
    }
    @Override public Optional<Parameter> findById(ParameterId id) { return jpa.findById(id.value()).map(e -> Parameter.reconstitute(ParameterId.from(e.getId()), MethodId.from(e.getMethodId()), e.getName(), e.getDataType(), e.getOrderIndex(), e.getIsVarargs(), e.getIsFinal(), e.getDefaultValue())); }
    @Override public List<Parameter> findByMethodId(String methodId) { return jpa.findByMethodId(methodId).stream().map(e -> Parameter.reconstitute(ParameterId.from(e.getId()), MethodId.from(e.getMethodId()), e.getName(), e.getDataType(), e.getOrderIndex(), e.getIsVarargs(), e.getIsFinal(), e.getDefaultValue())).toList(); }
    @Override public void delete(ParameterId id) { jpa.deleteById(id.value()); }
}
