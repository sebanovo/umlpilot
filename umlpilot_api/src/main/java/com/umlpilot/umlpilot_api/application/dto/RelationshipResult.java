package com.umlpilot.umlpilot_api.application.dto;

public record RelationshipResult(String id, String diagramId, String sourceElementId, String targetElementId, String relationshipType, String name, String direction, String associationClassElementId, String sourceHandle, String targetHandle, String waypoints) {
    public RelationshipResult(String id, String diagramId, String sourceElementId, String targetElementId, String relationshipType, String name, String direction, String associationClassElementId, String sourceHandle, String targetHandle) {
        this(id, diagramId, sourceElementId, targetElementId, relationshipType, name, direction, associationClassElementId, sourceHandle, targetHandle, null);
    }
}
