package com.umlpilot.umlpilot_api.application.dto;

/**
 * @param invitationStatus estado de la invitación asociada (pending/accepted/rejected),
 *                         o null si la notificación no es una invitación.
 */
public record NotificationResult(
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
