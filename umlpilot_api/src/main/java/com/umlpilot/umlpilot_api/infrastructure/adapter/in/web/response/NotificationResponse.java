package com.umlpilot.umlpilot_api.infrastructure.adapter.in.web.response;

public record NotificationResponse(
        String id,
        String type,
        String title,
        String message,
        String projectId,
        String role,
        boolean read,
        String readAt,
        String createdAt,
        String invitationStatus
) {}
