package com.umlpilot.umlpilot_api.domain.model;

import java.util.Objects;
import java.util.UUID;

public record ElementId(String value) {
    public ElementId {
        Objects.requireNonNull(value, "ElementId cannot be null");
    }
    public static ElementId generate() { return new ElementId(UUID.randomUUID().toString()); }
    public static ElementId from(String value) { return new ElementId(value); }
    @Override public String toString() { return value; }
}
