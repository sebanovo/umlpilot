package com.umlpilot.umlpilot_api.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "diagram_share_links")
public class DiagramShareLinkJpaEntity {

    @Id
    @Column(name = "share_link_id", updatable = false, nullable = false)
    private String id;

    @Column(name = "project_id", nullable = false)
    private String projectId;

    @Column(name = "diagram_id", nullable = false, unique = true)
    private String diagramId;

    @Column(name = "token", nullable = false, unique = true, length = 64)
    private String token;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "link_role", length = 20)
    private String linkRole;

    @Column(name = "active", nullable = false)
    private boolean active;

    public DiagramShareLinkJpaEntity() {}

    public DiagramShareLinkJpaEntity(String id, String projectId, String diagramId, String token,
                                     String createdBy, LocalDateTime createdAt, String linkRole, boolean active) {
        this.id = id;
        this.projectId = projectId;
        this.diagramId = diagramId;
        this.token = token;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.linkRole = linkRole;
        this.active = active;
    }

    public String getId() { return id; }
    public String getProjectId() { return projectId; }
    public String getDiagramId() { return diagramId; }
    public String getToken() { return token; }
    public String getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getLinkRole() { return linkRole; }
    public void setLinkRole(String linkRole) { this.linkRole = linkRole; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
