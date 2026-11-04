package com.umlpilot.umlpilot_api.domain.model;

import java.util.Objects;
import java.util.UUID;

public record NotificationId(String value) {
    public NotificationId {
        Objects.requireNonNull(value, "NotificationId cannot be null");
    }

    public static NotificationId generate() {
        return new NotificationId(UUID.randomUUID().toString());
    }

    public static NotificationId from(String value) {
        return new NotificationId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
