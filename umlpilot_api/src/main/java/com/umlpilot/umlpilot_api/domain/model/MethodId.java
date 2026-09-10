package com.umlpilot.umlpilot_api.domain.model;

import java.util.Objects;
import java.util.UUID;

public record MethodId(String value) {
    public MethodId {
        Objects.requireNonNull(value, "MethodId cannot be null");
    }
    public static MethodId generate() { return new MethodId(UUID.randomUUID().toString()); }
    public static MethodId from(String value) { return new MethodId(value); }
    @Override public String toString() { return value; }
}
