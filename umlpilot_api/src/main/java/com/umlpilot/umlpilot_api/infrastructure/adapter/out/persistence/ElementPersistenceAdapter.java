package com.umlpilot.umlpilot_api.infrastructure.adapter.out.persistence;

import com.umlpilot.umlpilot_api.domain.model.*;
import com.umlpilot.umlpilot_api.domain.repository.ElementRepository;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;

@Component
public class ElementPersistenceAdapter implements ElementRepository {
    private final ElementJpaRepository jpa;
    public ElementPersistenceAdapter(ElementJpaRepository jpa) { this.jpa = jpa; }

    @Override public Element save(Element element) {
        Optional<ElementJpaEntity> existing = jpa.findById(element.getId().value());
        ElementJpaEntity e = existing.orElse(new ElementJpaEntity());
        e.setDiagramId(element.getDiagramId().value());
        e.setParentId(element.getParentId());
        e.setCreatorId(element.getCreatorId());
        e.setName(element.getName());
        e.setVisibility(element.getVisibility());
        e.setStereotype(element.getStereotype());
        e.setElementType(element.getElementType());
        e.setPositionX(element.getPositionX());
        e.setPositionY(element.getPositionY());
        e.setWidth(element.getWidth());
        e.setHeight(element.getHeight());
        e.setProperties(element.getProperties());
        return toDomain(jpa.save(e));
    }
    @Override public Optional<Element> findById(ElementId id) { return jpa.findById(id.value()).map(this::toDomain); }
    @Override public List<Element> findByDiagramId(String diagramId) { return jpa.findByDiagramId(diagramId).stream().map(this::toDomain).toList(); }
    @Override public void delete(ElementId id) { jpa.deleteById(id.value()); }
    private Element toDomain(ElementJpaEntity e) {
        return Element.reconstitute(ElementId.from(e.getId()), DiagramId.from(e.getDiagramId()), e.getParentId(), e.getCreatorId(), e.getName(), e.getVisibility(), e.getStereotype(), e.getElementType(), e.getPositionX(), e.getPositionY(), e.getWidth(), e.getHeight(), e.getProperties(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
