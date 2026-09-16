package com.umlpilot.umlpilot_api.application.generation;

import com.umlpilot.umlpilot_api.application.dto.DiagramSnapshot;
import com.umlpilot.umlpilot_api.domain.exception.DomainException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Valida que el diagrama solo use relaciones soportadas por el generador:
 * asociación normal y clase de asociación. El resto (herencia/generalización,
 * realización, dependencia, template binding, agregación y composición) se rechaza.
 */
@Component
public class BackendGenerationValidator {

    public static final Set<String> SUPPORTED_RELATIONSHIPS = Set.of("association", "association_class");

    public void validate(DiagramSnapshot snapshot) {
        Map<String, String> elementNames = new HashMap<>();
        for (DiagramSnapshot.ElementSnapshot element : snapshot.elements()) {
            elementNames.put(element.id(), element.name());
        }

        List<String> unsupported = new ArrayList<>();
        for (DiagramSnapshot.RelationshipSnapshot relationship : snapshot.relationships()) {
            String type = normalizeType(relationship.type());
            if (SUPPORTED_RELATIONSHIPS.contains(type)) {
                continue;
            }
            String source = elementNames.getOrDefault(relationship.sourceElementId(), relationship.sourceElementId());
            String target = elementNames.getOrDefault(relationship.targetElementId(), relationship.targetElementId());
            unsupported.add(type + " (" + source + " -> " + target + ")");
        }

        if (!unsupported.isEmpty()) {
            throw new DomainException(
                    "El diagrama usa relaciones no soportadas; solo se permiten asociación y clase de asociación. "
                            + "Relaciones no soportadas: " + String.join(", ", unsupported));
        }
    }

    public static String normalizeType(String type) {
        if (type == null || type.isBlank()) {
            return "association";
        }
        return type.trim().toLowerCase(Locale.ROOT);
    }
}
