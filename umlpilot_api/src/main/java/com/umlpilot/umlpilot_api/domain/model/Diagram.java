package com.umlpilot.umlpilot_api.domain.model;

import com.umlpilot.umlpilot_api.domain.exception.DomainException;
import java.time.LocalDateTime;

public class Diagram {
    private final DiagramId id;
    private final ProjectId projectId;
    private String name;
    private final String type;
    private String description;
    private String canvasData;
    private Integer version;
    private Boolean isLocked;
    private String lockedByUserId;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Diagram(DiagramId id, ProjectId projectId, String name, String type, String description, String canvasData, Integer version, Boolean isLocked, String lockedByUserId, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.projectId = projectId;
        this.name = name;
        this.type = type;
        this.description = description;
        this.canvasData = canvasData;
        this.version = version;
        this.isLocked = isLocked;
        this.lockedByUserId = lockedByUserId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Diagram create(String projectId, String name, String type) {
        if (name == null || name.isBlank()) throw new DomainException("Diagram name cannot be empty");
        if (type == null || type.isBlank()) throw new DomainException("Diagram type cannot be empty");
        LocalDateTime now = LocalDateTime.now();
        return new Diagram(DiagramId.generate(), ProjectId.from(projectId), name, type, null, "{}", 1, false, null, now, now);
    }

    public static Diagram reconstitute(DiagramId id, ProjectId projectId, String name, String type, String description, String canvasData, Integer version, Boolean isLocked, String lockedByUserId, LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new Diagram(id, projectId, name, type, description, canvasData, version, isLocked, lockedByUserId, createdAt, updatedAt);
    }

    public void updateName(String name) { if (name != null) this.name = name; this.updatedAt = LocalDateTime.now(); }
    public void updateDescription(String description) { if (description != null) this.description = description; this.updatedAt = LocalDateTime.now(); }
    public void updateCanvasData(String canvasData) { if (canvasData != null) { this.canvasData = canvasData; this.version++; this.updatedAt = LocalDateTime.now(); } }

    public void lock(String userId) {
        if (isLocked && !lockedByUserId.equals(userId)) throw new DomainException("Diagram is locked by another user");
        this.isLocked = true;
        this.lockedByUserId = userId;
    }

    public void unlock(String userId) {
        if (!isLocked || !lockedByUserId.equals(userId)) throw new DomainException("You don't have this diagram locked");
        this.isLocked = false;
        this.lockedByUserId = null;
    }

    public DiagramId getId() { return id; }
    public ProjectId getProjectId() { return projectId; }
    public String getName() { return name; }
    public String getType() { return type; }
    public String getDescription() { return description; }
    public String getCanvasData() { return canvasData; }
    public Integer getVersion() { return version; }
    public Boolean getIsLocked() { return isLocked; }
    public String getLockedByUserId() { return lockedByUserId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
