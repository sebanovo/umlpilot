package com.umlpilot.umlpilot_api.infrastructure.adapter.out.persistence;

import com.umlpilot.umlpilot_api.domain.model.*;
import com.umlpilot.umlpilot_api.domain.repository.MethodRepository;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;

@Component
public class MethodPersistenceAdapter implements MethodRepository {
    private final MethodJpaRepository jpa;
    public MethodPersistenceAdapter(MethodJpaRepository jpa) { this.jpa = jpa; }

    @Override public Method save(Method method) {
        Optional<MethodJpaEntity> existing = jpa.findById(method.getId().value());
        MethodJpaEntity e = existing.orElse(new MethodJpaEntity());
        e.setElementId(method.getElementId().value());
        e.setName(method.getName());
        e.setReturnType(method.getReturnType());
        e.setVisibility(method.getVisibility());
        e.setIsStatic(method.getIsStatic());
        e.setIsAbstract(method.getIsAbstract());
        e.setIsFinal(method.getIsFinal());
        e.setIsConstructor(method.getIsConstructor());
        e.setIsSynchronized(method.getIsSynchronized());
        e.setIsNative(method.getIsNative());
        e.setBody(method.getBody());
        e.setOrderIndex(method.getOrderIndex());
        MethodJpaEntity saved = jpa.save(e);
        return Method.reconstitute(MethodId.from(saved.getId()), ElementId.from(saved.getElementId()), saved.getName(), saved.getReturnType(), saved.getVisibility(), saved.getIsStatic(), saved.getIsAbstract(), saved.getIsFinal(), saved.getIsConstructor(), saved.getIsSynchronized(), saved.getIsNative(), saved.getBody(), saved.getOrderIndex(), List.of());
    }
    @Override public Optional<Method> findById(MethodId id) { return jpa.findById(id.value()).map(e -> Method.reconstitute(MethodId.from(e.getId()), ElementId.from(e.getElementId()), e.getName(), e.getReturnType(), e.getVisibility(), e.getIsStatic(), e.getIsAbstract(), e.getIsFinal(), e.getIsConstructor(), e.getIsSynchronized(), e.getIsNative(), e.getBody(), e.getOrderIndex(), List.of())); }
    @Override public List<Method> findByElementId(String elementId) { return jpa.findByElementId(elementId).stream().map(e -> Method.reconstitute(MethodId.from(e.getId()), ElementId.from(e.getElementId()), e.getName(), e.getReturnType(), e.getVisibility(), e.getIsStatic(), e.getIsAbstract(), e.getIsFinal(), e.getIsConstructor(), e.getIsSynchronized(), e.getIsNative(), e.getBody(), e.getOrderIndex(), List.of())).toList(); }
    @Override public void delete(MethodId id) { jpa.deleteById(id.value()); }
}
