package com.umlpilot.umlpilot_api.application.dto;

import java.util.List;
import java.util.Map;

/** Vista de solo lectura de un diagrama compartido públicamente (sin autenticación). */
public record PublicDiagramView(
        String projectId,
        String diagramId,
        String linkRole,
        String projectName,
        String diagramName,
        String diagramType,
        List<ElementResult> elements,
        List<RelationshipResult> relationships,
        Map<String, List<AttributeResult>> attributes,
        Map<String, List<MethodResult>> methods,
        Map<String, List<LiteralResult>> literals,
        Map<String, List<MultiplicityResult>> multiplicities
) {}
