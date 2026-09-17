package com.umlpilot.umlpilot_api.application.service;

import com.umlpilot.umlpilot_api.application.dto.*;
import com.umlpilot.umlpilot_api.domain.exception.DomainException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Construye (de forma aditiva) un diagrama completo a partir de una {@link DiagramSpec}
 * generada por la IA. Valida la especificación, asigna una cuadrícula de posiciones y
 * persiste elementos, atributos, métodos, parámetros y relaciones en una sola transacción.
 */
@Service
public class DiagramBuildService {

    private static final int MAX_ELEMENTS = 100;
    private static final int MAX_RELATIONSHIPS = 200;
    private static final int CELL_WIDTH = 220;
    private static final int CELL_HEIGHT = 150;
    private static final int MARGIN = 40;

    private final ElementService elementService;
    private final RelationshipService relationshipService;

    public DiagramBuildService(ElementService elementService, RelationshipService relationshipService) {
        this.elementService = elementService;
        this.relationshipService = relationshipService;
    }

    @Transactional
    public DiagramBuildResult build(String diagramId, String creatorId, DiagramSpec spec) {
        if (spec == null) {
            throw new DomainException("La especificación del diagrama está vacía");
        }

        List<DiagramSpec.ElementSpec> elementSpecs = spec.elements() == null ? List.of() : spec.elements();
        List<DiagramSpec.RelationshipSpec> relationshipSpecs =
                spec.relationships() == null ? List.of() : spec.relationships();

        if (elementSpecs.isEmpty() && relationshipSpecs.isEmpty()) {
            throw new DomainException("La especificación no contiene elementos ni relaciones");
        }
        if (elementSpecs.size() > MAX_ELEMENTS) {
            throw new DomainException("La especificación tiene demasiados elementos (máximo " + MAX_ELEMENTS + ")");
        }
        if (relationshipSpecs.size() > MAX_RELATIONSHIPS) {
            throw new DomainException("La especificación tiene demasiadas relaciones (máximo " + MAX_RELATIONSHIPS + ")");
        }

        int startY = computeStartY(diagramId);
        int columns = Math.max(1, (int) Math.ceil(Math.sqrt(elementSpecs.size())));

        Map<String, String> keyToId = new LinkedHashMap<>();
        Map<String, ElementResult> createdByKey = new LinkedHashMap<>();
        List<String> elementIds = new ArrayList<>();
        int attributeCount = 0;
        int methodCount = 0;
        int parameterCount = 0;

        int index = 0;
        for (DiagramSpec.ElementSpec elementSpec : elementSpecs) {
            if (elementSpec == null || isBlank(elementSpec.name())) {
                throw new DomainException("Cada elemento de la especificación necesita un 'name'");
            }
            String key = isBlank(elementSpec.key()) ? elementSpec.name().trim() : elementSpec.key().trim();
            if (keyToId.containsKey(key)) {
                throw new DomainException("Clave de elemento duplicada: " + key);
            }

            int column = index % columns;
            int row = index / columns;
            ElementResult created = elementService.createElement(new CreateElementCommand(
                    diagramId, creatorId, elementSpec.name().trim(), normalizeElementType(elementSpec.elementType()),
                    MARGIN + column * CELL_WIDTH, startY + row * CELL_HEIGHT));

            keyToId.put(key, created.id());
            createdByKey.put(key, created);
            elementIds.add(created.id());

            if (elementSpec.attributes() != null) {
                int order = 0;
                for (DiagramSpec.AttributeSpec attribute : elementSpec.attributes()) {
                    if (attribute == null || isBlank(attribute.name())) continue;
                    elementService.addAttribute(created.id(), attribute.name().trim(), attribute.dataType(),
                            normalizeVisibility(attribute.visibility(), "private"), attribute.alias(),
                            attribute.stereotype(), attribute.defaultValue(), order++);
                    attributeCount++;
                }
            }

            if (elementSpec.methods() != null) {
                int order = 0;
                for (DiagramSpec.MethodSpec method : elementSpec.methods()) {
                    if (method == null || isBlank(method.name())) continue;
                    MethodResult createdMethod = elementService.addMethod(created.id(), method.name().trim(),
                            isBlank(method.returnType()) ? "void" : method.returnType().trim(),
                            normalizeVisibility(method.visibility(), "public"), method.alias(), method.stereotype(),
                            method.defaultValue(), order++);
                    methodCount++;

                    if (method.parameters() != null) {
                        int paramOrder = 0;
                        for (DiagramSpec.ParameterSpec parameter : method.parameters()) {
                            if (parameter == null || isBlank(parameter.name())) continue;
                            elementService.addParameter(createdMethod.id(), parameter.name().trim(), parameter.dataType(),
                                    isBlank(parameter.direction()) ? "in" : parameter.direction().trim(),
                                    parameter.notes(), paramOrder++);
                            parameterCount++;
                        }
                    }
                }
            }

            index++;
        }

        List<String> relationshipIds = new ArrayList<>();
        for (DiagramSpec.RelationshipSpec relationship : relationshipSpecs) {
            if (relationship == null) continue;

            String sourceId = resolveKey(keyToId, relationship.sourceKey());
            String targetId = resolveKey(keyToId, relationship.targetKey());
            String type = normalizeRelationshipType(relationship.type());

            String associationClassElementId = null;
            if ("association_class".equals(type)) {
                ElementResult source = createdByKey.get(relationship.sourceKey().trim());
                ElementResult target = createdByKey.get(relationship.targetKey().trim());
                int midX = (positionX(source) + positionX(target)) / 2;
                int midY = (positionY(source) + positionY(target)) / 2 + 90;
                ElementResult cls = elementService.createElement(new CreateElementCommand(
                        diagramId, creatorId, source.name() + "_" + target.name(), "class", midX, midY));
                associationClassElementId = cls.id();
                elementIds.add(cls.id());
            }

            RelationshipResult created = relationshipService.createRelationship(new CreateRelationshipCommand(
                    diagramId, sourceId, targetId, type, associationClassElementId, null, null));

            if (!isBlank(relationship.name())) {
                created = relationshipService.updateRelationship(created.id(), relationship.name().trim(),
                        null, null, null, null, null, null, null);
            }

            applyMultiplicity(created.id(), "source", relationship.sourceMultiplicity());
            applyMultiplicity(created.id(), "target", relationship.targetMultiplicity());
            relationshipIds.add(created.id());
        }

        return new DiagramBuildResult(elementIds.size(), attributeCount, methodCount, parameterCount,
                relationshipIds.size(), elementIds, relationshipIds);
    }

    /** Coloca la nueva cuadrícula por debajo del contenido existente (construcción aditiva). */
    private int computeStartY(String diagramId) {
        int bottom = 0;
        for (ElementResult element : elementService.getElements(diagramId)) {
            int y = element.positionY() == null ? 0 : element.positionY();
            int height = element.height() == null ? 80 : element.height();
            bottom = Math.max(bottom, y + height);
        }
        return bottom + MARGIN;
    }

    private String resolveKey(Map<String, String> keyToId, String key) {
        if (isBlank(key)) {
            throw new DomainException("Una relación no indica su elemento origen/destino (sourceKey/targetKey)");
        }
        String id = keyToId.get(key.trim());
        if (id == null) {
            throw new DomainException("La relación referencia un elemento inexistente: " + key);
        }
        return id;
    }

    private void applyMultiplicity(String relationshipId, String end, String label) {
        if (isBlank(label)) return;
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
        if (value.equals("*")) return new int[] {0, -1};
        if (value.contains("..")) {
            String[] parts = value.split("\\.\\.");
            int min = parseInt(parts.length > 0 ? parts[0] : "0", 0);
            int max = parts.length > 1 ? parseMax(parts[1]) : -1;
            return new int[] {min, max};
        }
        int n = parseInt(value, 1);
        return new int[] {n, n};
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    private static int parseMax(String value) {
        String v = value.trim();
        return v.equals("*") ? -1 : parseInt(v, -1);
    }

    private static int positionX(ElementResult element) {
        return element.positionX() == null ? 0 : element.positionX();
    }

    private static int positionY(ElementResult element) {
        return element.positionY() == null ? 0 : element.positionY();
    }

    private static String normalizeElementType(String type) {
        if (isBlank(type)) return "class";
        return switch (type.trim().toLowerCase(Locale.ROOT)) {
            case "class", "clase" -> "class";
            case "interface", "interfaz" -> "interface";
            case "enumeration", "enum", "enumeracion" -> "enumeration";
            case "datatype", "data_type", "tipo" -> "dataType";
            case "note", "nota" -> "note";
            default -> "class";
        };
    }

    private static String normalizeVisibility(String visibility, String fallback) {
        if (isBlank(visibility)) return fallback;
        return switch (visibility.trim().toLowerCase(Locale.ROOT)) {
            case "public", "publico" -> "public";
            case "private", "privado" -> "private";
            case "protected", "protegido" -> "protected";
            case "package", "paquete" -> "package";
            default -> fallback;
        };
    }

    private static String normalizeRelationshipType(String type) {
        if (isBlank(type)) return "association";
        return switch (type.trim().toLowerCase(Locale.ROOT)) {
            case "aggregation", "agregacion" -> "aggregation";
            case "composition", "composicion" -> "composition";
            case "generalization", "generalizacion", "herencia", "inheritance" -> "generalization";
            case "realization", "realizacion", "implementation" -> "realization";
            case "association_class", "clase_asociacion" -> "association_class";
            default -> "association";
        };
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
