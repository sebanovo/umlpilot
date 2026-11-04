package com.umlpilot.umlpilot_api.domain.model;

import java.time.LocalDateTime;

/**
 * Notificación persistente del usuario (bandeja de entrada).
 * No depende de servicios de terceros: se consulta por polling desde el cliente.
 */
public class Notification {

    public static final String TYPE_PROJECT_INVITATION = "project_invitation";

    private final NotificationId id;
    private final String userId;
    private final String type;
    private final String title;
    private final String message;
    private final String projectId;
    private final String role;
    private final LocalDateTime createdAt;
    private LocalDateTime readAt;

    private Notification(NotificationId id, String userId, String type, String title, String message,
                         String projectId, String role, LocalDateTime createdAt, LocalDateTime readAt) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.projectId = projectId;
        this.role = role;
        this.createdAt = createdAt;
        this.readAt = readAt;
    }

    public static Notification projectInvitation(String userId, String projectId, String projectName,
                                                  String inviterName, String role) {
        String message = inviterName + " te invitó a colaborar en \"" + projectName + "\" como "
                + roleLabel(role) + ".";
        return new Notification(NotificationId.generate(), userId, TYPE_PROJECT_INVITATION,
                "Invitación a proyecto", message, projectId, role, LocalDateTime.now(), null);
    }

    public static Notification reconstitute(NotificationId id, String userId, String type, String title,
                                            String message, String projectId, String role,
                                            LocalDateTime createdAt, LocalDateTime readAt) {
        return new Notification(id, userId, type, title, message, projectId, role, createdAt, readAt);
    }

    /** Marca la notificación como leída (idempotente). */
    public void markAsRead() {
        if (readAt == null) {
            readAt = LocalDateTime.now();
        }
    }

    public boolean isRead() {
        return readAt != null;
    }

    public boolean isProjectInvitation() {
        return TYPE_PROJECT_INVITATION.equals(type);
    }

    public NotificationId getId() { return id; }
    public String getUserId() { return userId; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getProjectId() { return projectId; }
    public String getRole() { return role; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getReadAt() { return readAt; }

    private static String roleLabel(String role) {
        return switch (role) {
            case "viewer" -> "lector";
            case "editor" -> "editor";
            case "owner" -> "propietario";
            default -> role;
        };
    }
}
