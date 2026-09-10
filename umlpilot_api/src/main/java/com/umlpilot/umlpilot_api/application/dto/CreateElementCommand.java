package com.umlpilot.umlpilot_api.application.dto;

public record CreateElementCommand(String diagramId, String creatorId, String name, String elementType, Integer positionX, Integer positionY) {}
