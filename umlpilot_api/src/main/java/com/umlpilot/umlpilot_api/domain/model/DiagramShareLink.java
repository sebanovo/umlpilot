package com.umlpilot.umlpilot_api.domain.model;

import com.umlpilot.umlpilot_api.domain.exception.DomainException;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Enlace para compartir un diagrama. Cualquiera con el token puede previsualizarlo y,
 * al iniciar sesión, unirse al proyecto con el rol configurado (como el enlace de Figma).
 */
public class DiagramShareLink {

    public static final String ROLE_VIEWER = "viewer";
    public static final String ROLE_EDITOR = "editor";

    private final DiagramShareLinkId id;
    private final String projectId;
    private final String diagramId;
    private final String token;
    private final String createdBy;
    private final LocalDateTime createdAt;
    private String linkRole;
    private boolean active;

    private DiagramShareLink(DiagramShareLinkId id, String projectId, String diagramId, String token,
                             String createdBy, LocalDateTime createdAt, String linkRole, boolean active) {
        this.id = id;
        this.projectId = projectId;
        this.diagramId = diagramId;
        this.token = token;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.linkRole = normalizeRole(linkRole);
        this.active = active;
    }

    public static DiagramShareLink create(String projectId, String diagramId, String createdBy, String linkRole) {
        String token = UUID.randomUUID().toString().replace("-", "");
        return new DiagramShareLink(DiagramShareLinkId.generate(), projectId, diagramId, token,
                createdBy, LocalDateTime.now(), linkRole, true);
    }

    public static DiagramShareLink reconstitute(DiagramShareLinkId id, String projectId, String diagramId,
                                                String token, String createdBy, LocalDateTime createdAt,
                                                String linkRole, boolean active) {
        return new DiagramShareLink(id, projectId, diagramId, token, createdBy, createdAt, linkRole, active);
    }

    public void activate() { this.active = true; }
    public void revoke() { this.active = false; }

    public void changeRole(String role) { this.linkRole = normalizeRole(role); }

    private static String normalizeRole(String role) {
        if (role == null || role.isBlank()) return ROLE_VIEWER;
        String normalized = role.trim().toLowerCase();
        if (!ROLE_VIEWER.equals(normalized) && !ROLE_EDITOR.equals(normalized)) {
            throw new DomainException("Rol inválido. Usa 'viewer' o 'editor'");
        }
        return normalized;
    }

    public DiagramShareLinkId getId() { return id; }
    public String getProjectId() { return projectId; }
    public String getDiagramId() { return diagramId; }
    public String getToken() { return token; }
    public String getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getLinkRole() { return linkRole; }
    public boolean isActive() { return active; }
}
