package com.umlpilot.umlpilot_api.domain.model;

import java.util.Objects;

public record CollaboratorId(String projectId, String userId) {
    public CollaboratorId {
        Objects.requireNonNull(projectId, "projectId cannot be null");
        Objects.requireNonNull(userId, "userId cannot be null");
    }
}
