package com.umlpilot.umlpilot_api.application.dto;

public record CreateDiagramCommand(String projectId, String name, String type, String description) {}
