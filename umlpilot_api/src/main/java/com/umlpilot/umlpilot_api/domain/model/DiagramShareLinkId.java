package com.umlpilot.umlpilot_api.domain.model;

import java.util.Objects;
import java.util.UUID;

public record DiagramShareLinkId(String value) {
    public DiagramShareLinkId {
        Objects.requireNonNull(value, "DiagramShareLinkId cannot be null");
    }

    public static DiagramShareLinkId generate() {
        return new DiagramShareLinkId(UUID.randomUUID().toString());
    }

    public static DiagramShareLinkId from(String value) {
        return new DiagramShareLinkId(value);
    }
}
