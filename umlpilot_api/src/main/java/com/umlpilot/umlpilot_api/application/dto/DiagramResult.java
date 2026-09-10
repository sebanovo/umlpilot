package com.umlpilot.umlpilot_api.application.dto;

public record DiagramResult(String id, String projectId, String name, String type, String description, String canvasData, Integer version, Boolean isLocked, String lockedByUserId, String createdAt, String updatedAt) {}
