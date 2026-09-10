package com.umlpilot.umlpilot_api.domain.model;

import java.util.Objects;
import java.util.UUID;

public record MultiplicityId(String value) {
    public MultiplicityId {
        Objects.requireNonNull(value, "MultiplicityId cannot be null");
    }
    public static MultiplicityId generate() { return new MultiplicityId(UUID.randomUUID().toString()); }
    public static MultiplicityId from(String value) { return new MultiplicityId(value); }
    @Override public String toString() { return value; }
}
