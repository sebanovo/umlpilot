package com.umlpilot.umlpilot_api.model;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class CollaboratorId implements Serializable {

    private String projectId;
    private String userId;

    public CollaboratorId() {}

    public CollaboratorId(String projectId, String userId) {
        this.projectId = projectId;
        this.userId = userId;
    }

    public String getProjectId() { return projectId; }
    public String getUserId() { return userId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CollaboratorId that)) return false;
        return Objects.equals(projectId, that.projectId) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(projectId, userId);
    }
}
