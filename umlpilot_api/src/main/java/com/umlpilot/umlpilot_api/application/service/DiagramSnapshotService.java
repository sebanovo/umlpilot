package com.umlpilot.umlpilot_api.application.service;

import com.umlpilot.umlpilot_api.application.dto.DiagramSnapshot;
import com.umlpilot.umlpilot_api.application.dto.ElementResult;
import com.umlpilot.umlpilot_api.application.dto.MultiplicityResult;
import com.umlpilot.umlpilot_api.application.dto.RelationshipResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Compone una {@link DiagramSnapshot} de un diagrama reutilizando los servicios
 * de elementos y relaciones. Es la entrada de la generación de código.
 */
@Service
public class DiagramSnapshotService {

    private final ElementService elementService;
    private final RelationshipService relationshipService;

    public DiagramSnapshotService(ElementService elementService, RelationshipService relationshipService) {
        this.elementService = elementService;
        this.relationshipService = relationshipService;
    }

    @Transactional(readOnly = true)
    public DiagramSnapshot snapshot(String diagramId) {
        List<DiagramSnapshot.ElementSnapshot> elements = elementService.getElements(diagramId).stream()
                .map(this::toElement)
                .toList();
        List<DiagramSnapshot.RelationshipSnapshot> relationships = relationshipService.getRelationships(diagramId).stream()
                .map(this::toRelationship)
                .toList();
        return new DiagramSnapshot(elements, relationships);
    }

    private DiagramSnapshot.ElementSnapshot toElement(ElementResult element) {
        List<DiagramSnapshot.AttributeSnapshot> attributes = elementService.getAttributes(element.id()).stream()
                .map(a -> new DiagramSnapshot.AttributeSnapshot(
                        a.id(), a.name(), a.dataType(), a.visibility(), a.stereotype(), a.defaultValue()))
                .toList();
        List<DiagramSnapshot.MethodSnapshot> methods = elementService.getMethods(element.id()).stream()
                .map(m -> new DiagramSnapshot.MethodSnapshot(m.id(), m.name(), m.returnType(), m.visibility(),
                        m.parameters().stream()
                                .map(p -> new DiagramSnapshot.ParameterSnapshot(
                                        p.id(), p.name(), p.dataType(), p.direction()))
                                .toList()))
                .toList();
        return new DiagramSnapshot.ElementSnapshot(
                element.id(), element.name(), element.elementType(), element.stereotype(), element.visibility(),
                attributes, methods);
    }

    private DiagramSnapshot.RelationshipSnapshot toRelationship(RelationshipResult relationship) {
        String source = null;
        String target = null;
        for (MultiplicityResult multiplicity : relationshipService.getMultiplicities(relationship.id())) {
            String label = formatMultiplicity(multiplicity);
            if ("source".equals(multiplicity.end())) {
                source = label;
            } else if ("target".equals(multiplicity.end())) {
                target = label;
            }
        }
        return new DiagramSnapshot.RelationshipSnapshot(
                relationship.id(), relationship.relationshipType(), relationship.name(),
                relationship.sourceElementId(), relationship.targetElementId(),
                source, target, relationship.associationClassElementId());
    }

    /** Formatea min/max a una etiqueta UML ("1", "0..*", "1..*"); {@code null} si no hay multiplicidad. */
    static String formatMultiplicity(MultiplicityResult multiplicity) {
        Integer min = multiplicity.min();
        Integer max = multiplicity.max();
        if (min == null && max == null) {
            return null;
        }
        if (max != null && max == -1) {
            return (min == null ? 0 : min) + "..*";
        }
        if (min != null && max != null && min.equals(max)) {
            return String.valueOf(min);
        }
        return (min == null ? 0 : min) + ".." + (max == null ? "*" : max);
    }
}
