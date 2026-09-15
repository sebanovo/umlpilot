package com.umlpilot.umlpilot_api.application.dto;

public record CreateRelationshipCommand(String diagramId, String sourceElementId, String targetElementId, String relationshipType, String associationClassElementId, String sourceHandle, String targetHandle) {}
