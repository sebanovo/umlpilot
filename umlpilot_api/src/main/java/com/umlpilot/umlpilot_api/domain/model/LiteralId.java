package com.umlpilot.umlpilot_api.domain.model;

import java.util.Objects;
import java.util.UUID;

public record LiteralId(String value) {
    public LiteralId {
        Objects.requireNonNull(value, "LiteralId cannot be null");
    }
    public static LiteralId generate() { return new LiteralId(UUID.randomUUID().toString()); }
    public static LiteralId from(String value) { return new LiteralId(value); }
    @Override public String toString() { return value; }
}
