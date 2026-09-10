package com.umlpilot.umlpilot_api.infrastructure.adapter.in.web.response;

public record DiagramResponse(String id, String projectId, String name, String type, String description, String canvasData, Integer version, Boolean isLocked, String lockedByUserId, String createdAt, String updatedAt) {}
