package com.umlpilot.umlpilot_api.dto;

public record ProjectResponse(
        String id,
        String name,
        String description,
        String creatorId,
        String status,
        String createdAt,
        String updatedAt
) {}
