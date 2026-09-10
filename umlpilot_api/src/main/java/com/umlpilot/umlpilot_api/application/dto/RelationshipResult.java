package com.umlpilot.umlpilot_api.application.dto;

public record RelationshipResult(String id, String diagramId, String sourceElementId, String targetElementId, String relationshipType, String name, String direction) {}
