package com.umlpilot.umlpilot_api.application.service;

import com.umlpilot.umlpilot_api.application.dto.CreateElementCommand;
import com.umlpilot.umlpilot_api.application.dto.CreateRelationshipCommand;
import com.umlpilot.umlpilot_api.application.dto.ElementResult;
import com.umlpilot.umlpilot_api.application.dto.ImportXmiResult;
import com.umlpilot.umlpilot_api.application.dto.ImportedDiagram;
import com.umlpilot.umlpilot_api.application.dto.MethodResult;
import com.umlpilot.umlpilot_api.application.dto.MultiplicityResult;
import com.umlpilot.umlpilot_api.application.dto.RelationshipResult;
import com.umlpilot.umlpilot_api.application.port.out.DiagramChangeNotifier;
import com.umlpilot.umlpilot_api.application.port.out.XmiDocumentParser;
import com.umlpilot.umlpilot_api.domain.exception.DomainException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Caso de uso de importación XMI: parsea y valida el archivo y **añade** sus elementos al
 * diagrama actual sin borrar ni modificar lo existente. Reutiliza los mismos casos de uso
 * que las tools de IA y la construcción con IA.
 */
@Service
public class XmiImportService {

    static final int MAX_ELEMENTS = 200;
    private static final int CELL_WIDTH = 220;
    private static final int CELL_HEIGHT = 150;
    private static final int MARGIN = 40;
    private static final String IMPORT_ORIGIN = "import";

    private final XmiDocumentParser parser;
    private final ElementService elementService;
    private final RelationshipService relationshipService;
    private final DiagramChangeNotifier changeNotifier;

    public XmiImportService(XmiDocumentParser parser, ElementService elementService,
                            RelationshipService relationshipService, DiagramChangeNotifier changeNotifier) {
        this.parser = parser;
        this.elementService = elementService;
        this.relationshipService = relationshipService;
        this.changeNotifier = changeNotifier;
    }

    @Transactional
    public ImportXmiResult importDiagram(String diagramId, String userId, byte[] xmi) {
        ImportedDiagram imported = parser.parse(xmi);
        if (imported.elements().size() > MAX_ELEMENTS) {
            throw new DomainException(
                    "El archivo supera el máximo de " + MAX_ELEMENTS + " elementos por importación");
        }

        List<String> warnings = new ArrayList<>(imported.warnings());
        Set<String> usedNames = new HashSet<>();
        for (ElementResult existing : elementService.getElements(diagramId)) {
            usedNames.add(existing.name().toLowerCase(Locale.ROOT));
        }

        int startY = computeStartY(diagramId);
        int columns = Math.max(1, (int) Math.ceil(Math.sqrt(Math.max(1, imported.elements().size()))));

        Map<String, String> keyToId = new LinkedHashMap<>();
        int elements = 0;
        int attributes = 0;
        int methods = 0;
        int parameters = 0;
        int literals = 0;

        int index = 0;
        for (ImportedDiagram.ElementSpec element : imported.elements()) {
            String key = element.localKey();
            if (key == null || key.isBlank()) {
                warnings.add("Se omitió un elemento sin identificador");
                continue;
            }
            if (keyToId.containsKey(key)) {
                continue;
            }

            String name = uniqueName(element.name(), usedNames, warnings);
            int column = index % columns;
            int row = index / columns;
            int x = element.positionX() != null ? element.positionX() : MARGIN + column * CELL_WIDTH;
            int y = element.positionY() != null ? element.positionY() : startY + row * CELL_HEIGHT;

            ElementResult created = elementService.createElement(new CreateElementCommand(
                    diagramId, userId, name, element.elementType(), x, y));
            if (element.width() != null && element.height() != null) {
                elementService.updateElement(diagramId, created.id(), null, null, null, null, null,
                        element.width(), element.height());
            }
            keyToId.put(key, created.id());
            elements++;

            int order = 0;
            for (ImportedDiagram.AttributeSpec attribute : element.attributes()) {
                if (attribute.name() == null || attribute.name().isBlank()) {
                    continue;
                }
                // La UI usa "String" cuando no se indica tipo; la columna es NOT NULL.
                elementService.addAttribute(created.id(), attribute.name().trim(),
                        orDefault(attribute.dataType(), "String"), attribute.visibility(),
                        null, null, attribute.defaultValue(), order++);
                attributes++;
            }

            order = 0;
            for (ImportedDiagram.MethodSpec method : element.methods()) {
                if (method.name() == null || method.name().isBlank()) {
                    continue;
                }
                MethodResult createdMethod = elementService.addMethod(created.id(), method.name().trim(),
                        orDefault(method.returnType(), "void"), method.visibility(), null, null, null, order++);
                methods++;

                int parameterOrder = 0;
                for (ImportedDiagram.ParameterSpec parameter : method.parameters()) {
                    if (parameter.name() == null || parameter.name().isBlank()) {
                        continue;
                    }
                    elementService.addParameter(createdMethod.id(), parameter.name().trim(),
                            orDefault(parameter.dataType(), "String"),
                            orDefault(parameter.direction(), "in"), null, parameterOrder++);
                    parameters++;
                }
            }

            order = 0;
            for (ImportedDiagram.LiteralSpec literal : element.literals()) {
                if (literal.name() == null || literal.name().isBlank()) {
                    continue;
                }
                elementService.addLiteral(created.id(), literal.name().trim(), literal.value(), order++);
                literals++;
            }
            index++;
        }

        int relationships = 0;
        for (ImportedDiagram.RelationshipSpec relationship : imported.relationships()) {
            String sourceId = keyToId.get(relationship.sourceKey());
            String targetId = keyToId.get(relationship.targetKey());
            if (sourceId == null || targetId == null) {
                warnings.add("Se omitió una relación cuyos extremos no se importaron");
                continue;
            }
            String associationClassElementId = relationship.associationClassKey() == null
                    ? null
                    : keyToId.get(relationship.associationClassKey());

            RelationshipResult created = relationshipService.createRelationship(new CreateRelationshipCommand(
                    diagramId, sourceId, targetId, relationship.type(), associationClassElementId, null, null));
            if (relationship.name() != null && !relationship.name().isBlank()) {
                created = relationshipService.updateRelationship(created.id(), relationship.name().trim(),
                        null, null, null, null, null, null, null);
            }
            applyMultiplicity(created.id(), "source", relationship.sourceMultiplicity());
            applyMultiplicity(created.id(), "target", relationship.targetMultiplicity());
            relationships++;
        }

        changeNotifier.bulkChanged(diagramId, userId, IMPORT_ORIGIN);

        return new ImportXmiResult(elements, attributes, methods, parameters, literals, relationships, warnings);
    }

    private String uniqueName(String rawName, Set<String> usedNames, List<String> warnings) {
        String base = rawName == null || rawName.isBlank() ? "Sin nombre" : rawName.trim();
        String candidate = base;
        int suffix = 1;
        while (usedNames.contains(candidate.toLowerCase(Locale.ROOT))) {
            candidate = base + "_" + suffix++;
        }
        if (!candidate.equals(base)) {
            warnings.add("Se renombró '" + base + "' a '" + candidate + "' por colisión de nombres");
        }
        usedNames.add(candidate.toLowerCase(Locale.ROOT));
        return candidate;
    }

    /** Coloca la cuadrícula de los elementos sin posición por debajo del contenido existente. */
    private int computeStartY(String diagramId) {
        int bottom = 0;
        for (ElementResult element : elementService.getElements(diagramId)) {
            int y = element.positionY() == null ? 0 : element.positionY();
            int height = element.height() == null ? 80 : element.height();
            bottom = Math.max(bottom, y + height);
        }
        return bottom + MARGIN;
    }

    private void applyMultiplicity(String relationshipId, String end, String label) {
        if (label == null || label.isBlank()) {
            return;
        }
        int[] bounds = parseMultiplicity(label);
        for (MultiplicityResult multiplicity : relationshipService.getMultiplicities(relationshipId)) {
            if (end.equals(multiplicity.end())) {
                relationshipService.updateMultiplicity(multiplicity.id(), bounds[0], bounds[1]);
                return;
            }
        }
    }

    private static int[] parseMultiplicity(String label) {
        String value = label.trim();
        if (value.equals("*")) {
            return new int[] {0, -1};
        }
        if (value.contains("..")) {
            String[] parts = value.split("\\.\\.");
            int min = parseInt(parts.length > 0 ? parts[0] : "0", 0);
            int max = parts.length > 1 ? parseMax(parts[1]) : -1;
            return new int[] {min, max};
        }
        int exact = parseInt(value, 1);
        return new int[] {exact, exact};
    }

    private static int parseMax(String value) {
        String trimmed = value.trim();
        return trimmed.equals("*") ? -1 : parseInt(trimmed, -1);
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    private static String orDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
