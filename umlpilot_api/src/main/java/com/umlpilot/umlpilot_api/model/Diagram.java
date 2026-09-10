package com.umlpilot.umlpilot_api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "diagram")
public class Diagram {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "diagram_id")
    private String id;

    @Column(name = "project_id", nullable = false)
    private String projectId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String type;

    private String description;

    @Column(name = "canvas_data", columnDefinition = "jsonb")
    private String canvasData = "{}";

    @Column(nullable = false)
    private Integer version = 1;

    @Column(name = "is_locked", nullable = false)
    private Boolean isLocked = false;

    @Column(name = "locked_by_user_id")
    private String lockedByUserId;

    @Column(name = "creation_date", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "update_date", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Diagram() {}

    public Diagram(String projectId, String name, String type) {
        this.projectId = projectId;
        this.name = name;
        this.type = type;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCanvasData() { return canvasData; }
    public void setCanvasData(String canvasData) { this.canvasData = canvasData; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public Boolean getIsLocked() { return isLocked; }
    public void setIsLocked(Boolean locked) { isLocked = locked; }
    public String getLockedByUserId() { return lockedByUserId; }
    public void setLockedByUserId(String userId) { this.lockedByUserId = userId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
