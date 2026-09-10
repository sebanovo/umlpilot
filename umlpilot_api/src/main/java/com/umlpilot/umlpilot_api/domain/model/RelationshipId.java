package com.umlpilot.umlpilot_api.domain.model;

import java.util.Objects;
import java.util.UUID;

public record RelationshipId(String value) {
    public RelationshipId {
        Objects.requireNonNull(value, "RelationshipId cannot be null");
    }
    public static RelationshipId generate() { return new RelationshipId(UUID.randomUUID().toString()); }
    public static RelationshipId from(String value) { return new RelationshipId(value); }
    @Override public String toString() { return value; }
}
