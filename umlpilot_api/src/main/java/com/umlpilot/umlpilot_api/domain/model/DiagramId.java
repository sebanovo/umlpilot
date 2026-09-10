package com.umlpilot.umlpilot_api.domain.model;

import java.util.Objects;
import java.util.UUID;

public record DiagramId(String value) {
    public DiagramId {
        Objects.requireNonNull(value, "DiagramId cannot be null");
    }

    public static DiagramId generate() {
        return new DiagramId(UUID.randomUUID().toString());
    }

    public static DiagramId from(String value) {
        return new DiagramId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
