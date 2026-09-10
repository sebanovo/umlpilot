package com.umlpilot.umlpilot_api.domain.model;

import java.util.Objects;
import java.util.UUID;

public record AttributeId(String value) {
    public AttributeId {
        Objects.requireNonNull(value, "AttributeId cannot be null");
    }
    public static AttributeId generate() { return new AttributeId(UUID.randomUUID().toString()); }
    public static AttributeId from(String value) { return new AttributeId(value); }
    @Override public String toString() { return value; }
}
