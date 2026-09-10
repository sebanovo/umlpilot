package com.umlpilot.umlpilot_api.dto;

public record DiagramResponse(
        String id,
        String projectId,
        String name,
        String type,
        String description,
        String canvasData,
        Integer version,
        Boolean isLocked,
        String lockedByUserId,
        String createdAt,
        String updatedAt
) {}
