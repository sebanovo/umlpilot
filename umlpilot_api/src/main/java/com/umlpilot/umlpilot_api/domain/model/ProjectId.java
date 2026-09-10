package com.umlpilot.umlpilot_api.domain.model;

import java.util.Objects;
import java.util.UUID;

public record ProjectId(String value) {
    public ProjectId {
        Objects.requireNonNull(value, "ProjectId cannot be null");
    }

    public static ProjectId generate() {
        return new ProjectId(UUID.randomUUID().toString());
    }

    public static ProjectId from(String value) {
        return new ProjectId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
