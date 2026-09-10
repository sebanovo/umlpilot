package com.umlpilot.umlpilot_api.domain.model;

import com.umlpilot.umlpilot_api.domain.exception.DomainException;
import java.time.LocalDateTime;

public class Project {
    private final ProjectId id;
    private String name;
    private String description;
    private final UserId creatorId;
    private final String status;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Project(ProjectId id, String name, String description, UserId creatorId, String status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.creatorId = creatorId;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Project create(String name, String description, String creatorId) {
        if (name == null || name.isBlank()) throw new DomainException("Project name cannot be empty");
        LocalDateTime now = LocalDateTime.now();
        return new Project(ProjectId.generate(), name, description, UserId.from(creatorId), "active", now, now);
    }

    public static Project reconstitute(ProjectId id, String name, String description, UserId creatorId, String status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new Project(id, name, description, creatorId, status, createdAt, updatedAt);
    }

    public void updateName(String name) {
        if (name != null) this.name = name;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateDescription(String description) {
        if (description != null) this.description = description;
        this.updatedAt = LocalDateTime.now();
    }

    public ProjectId getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public UserId getCreatorId() { return creatorId; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
