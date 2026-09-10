package com.umlpilot.umlpilot_api.domain.model;

import java.util.Objects;
import java.util.UUID;

public record ParameterId(String value) {
    public ParameterId {
        Objects.requireNonNull(value, "ParameterId cannot be null");
    }
    public static ParameterId generate() { return new ParameterId(UUID.randomUUID().toString()); }
    public static ParameterId from(String value) { return new ParameterId(value); }
    @Override public String toString() { return value; }
}
